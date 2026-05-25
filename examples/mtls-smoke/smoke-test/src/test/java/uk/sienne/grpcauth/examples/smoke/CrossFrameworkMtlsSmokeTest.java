package uk.sienne.grpcauth.examples.smoke;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import uk.sienne.grpcauth.examples.common.SmokeRpc;
import uk.sienne.grpcauth.examples.micronaut.MicronautSmokeApplication;
import uk.sienne.grpcauth.examples.micronaut.MicronautToSpringClient;
import uk.sienne.grpcauth.examples.spring.SpringSmokeApplication;
import uk.sienne.grpcauth.examples.spring.SpringToMicronautClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrossFrameworkMtlsSmokeTest {

    @Test
    void springAndMicronautServicesExchangeTrafficOverMtls() {
        Path certDir = kepsenRoot().resolve("certs").resolve("dev");
        int springPort = freePort();
        int micronautPort = freePort();
        System.out.printf(
                "MTLS_SMOKE ports spring=%d micronaut=%d certDir=%s%n",
                springPort,
                micronautPort,
                certDir
        );

        try (
                ConfigurableApplicationContext spring = startSpring(springPort, certDir);
                ApplicationContext micronaut = startMicronaut(micronautPort, certDir)
        ) {
            String springToMicronaut = eventually(() -> SpringToMicronautClient.build(certDir).apply(micronautPort));
            String micronautToSpring = eventually(() -> MicronautToSpringClient.build(certDir).apply(springPort));

            assertEquals("micronaut:from-spring", springToMicronaut);
            assertEquals("spring:from-micronaut", micronautToSpring);
            System.out.printf(
                    "MTLS_SMOKE allow client=%s target=%s response=%s%n",
                    SmokeRpc.SERVICE_A_IDENTITY,
                    SmokeRpc.MICRONAUT_SERVICE + "/Ping",
                    springToMicronaut
            );
            System.out.printf(
                    "MTLS_SMOKE allow client=%s target=%s response=%s%n",
                    SmokeRpc.SERVICE_B_IDENTITY,
                    SmokeRpc.SPRING_SERVICE + "/Ping",
                    micronautToSpring
            );

            StatusRuntimeException wrongClient = assertThrows(
                    StatusRuntimeException.class,
                    () -> SmokeRpc.callWithClientCertificate(
                            certDir,
                            "service-a",
                            SmokeRpc.SPRING_PING,
                            springPort,
                            "wrong-client"
                    )
            );
            assertEquals(Status.Code.PERMISSION_DENIED, wrongClient.getStatus().getCode());
            System.out.printf(
                    "MTLS_SMOKE deny client=%s target=%s status=%s%n",
                    SmokeRpc.SERVICE_A_IDENTITY,
                    SmokeRpc.SPRING_SERVICE + "/Ping",
                    wrongClient.getStatus().getCode()
            );

            StatusRuntimeException missingClientCert = assertThrows(
                    StatusRuntimeException.class,
                    () -> SmokeRpc.callWithoutClientCertificate(
                            certDir,
                            SmokeRpc.SPRING_PING,
                            springPort,
                            "no-client-cert"
                    )
            );
            assertEquals(Status.Code.UNAVAILABLE, missingClientCert.getStatus().getCode());
            System.out.printf(
                    "MTLS_SMOKE reject client=no-client-cert target=%s status=%s%n",
                    SmokeRpc.SPRING_SERVICE + "/Ping",
                    missingClientCert.getStatus().getCode()
            );
        }
    }

    private ConfigurableApplicationContext startSpring(int port, Path certDir) {
        return new SpringApplicationBuilder(SpringSmokeApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Map.ofEntries(
                        Map.entry("grpc.server.port", String.valueOf(port)),
                        Map.entry("mtls.server.enabled", "true"),
                        Map.entry("mtls.server.cert-chain", certDir.resolve("server.crt").toString()),
                        Map.entry("mtls.server.private-key", certDir.resolve("server.key").toString()),
                        Map.entry("mtls.server.trust-cert-collection", certDir.resolve("ca.crt").toString()),
                        Map.entry("service-acl.enabled", "true"),
                        Map.entry("service-acl.default-action", "deny"),
                        Map.entry("service-acl.identity-source", "san-uri"),
                        Map.entry("service-acl.rules.micronaut.method", SmokeRpc.SPRING_SERVICE + "/Ping"),
                        Map.entry("service-acl.rules.micronaut.allowed-clients[0]", SmokeRpc.SERVICE_B_IDENTITY),
                        Map.entry("spring.main.banner-mode", "off"),
                        Map.entry("logging.level.root", "WARN")
                ))
                .run();
    }

    private ApplicationContext startMicronaut(int port, Path certDir) {
        return ApplicationContext.builder()
                .properties(Map.ofEntries(
                        Map.entry("micronaut.application.name", "micronaut-mtls-service"),
                        Map.entry("grpc.server.port", String.valueOf(port)),
                        Map.entry("mtls.server.enabled", "true"),
                        Map.entry("mtls.server.cert-chain", certDir.resolve("server.crt").toString()),
                        Map.entry("mtls.server.private-key", certDir.resolve("server.key").toString()),
                        Map.entry("mtls.server.trust-cert-collection", certDir.resolve("ca.crt").toString()),
                        Map.entry("service-acl.enabled", "true"),
                        Map.entry("service-acl.default-action", "deny"),
                        Map.entry("service-acl.identity-source", "san-uri"),
                        Map.entry("service-acl.rules.spring.method", SmokeRpc.MICRONAUT_SERVICE + "/Ping"),
                        Map.entry("service-acl.rules.spring.allowed-clients[0]", SmokeRpc.SERVICE_A_IDENTITY)
                ))
                .eagerInitSingletons(true)
                .start();
    }

    private String eventually(Callable<String> call) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        RuntimeException last = null;

        while (System.nanoTime() < deadline) {
            try {
                return call.call();
            } catch (RuntimeException e) {
                last = e;
                sleep();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        throw last == null ? new IllegalStateException("Call did not run") : last;
    }

    private Path kepsenRoot() {
        return Path.of(System.getProperty("kepsen.root")).toAbsolutePath().normalize();
    }

    private int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to allocate local port", e);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
