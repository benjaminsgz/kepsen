package uk.sienne;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.micronaut.context.annotation.Property;
import io.micronaut.grpc.server.GrpcEmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.netty.handler.ssl.SslContext;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
@Property(name = "grpc.server.port", value = "0")
@Property(name = "service-acl.enabled", value = "true")
@Property(name = "service-acl.default-action", value = "deny")
@Property(name = "service-acl.identity-source", value = "san-uri")
@Property(name = "service-acl.rules.kepsen-send.method", value = "uk.sienne.KepsenService/send")
@Property(name = "service-acl.rules.kepsen-send.allowed-clients", value = "spiffe://internal/ns/default/sa/service-a")
@Property(name = "service-acl.rules.kepsen-service.method", value = "uk.sienne.OtherService/*")
@Property(name = "service-acl.rules.kepsen-service.allowed-clients", value = "spiffe://internal/ns/default/sa/service-b")
class KepsenMtlsIntegrationTest {

    @Inject
    GrpcEmbeddedServer grpcServer;

    @Test
    void allowsClientWithMatchingSanUriAcl() throws Exception {
        ManagedChannel channel = channelFor("service-a");
        try {
            KepsenServiceGrpc.KepsenServiceBlockingStub stub = KepsenServiceGrpc.newBlockingStub(channel);

            KepsenReply reply = stub.send(KepsenRequest.newBuilder()
                    .setName("service-a")
                    .build());

            assertEquals("Hello service-a", reply.getMessage());
        } finally {
            channel.shutdownNow();
        }
    }

    @Test
    void deniesClientWithValidCertificateButNoAclMatch() throws Exception {
        ManagedChannel channel = channelFor("service-b");
        try {
            KepsenServiceGrpc.KepsenServiceBlockingStub stub = KepsenServiceGrpc.newBlockingStub(channel);

            StatusRuntimeException exception = assertThrows(
                    StatusRuntimeException.class,
                    () -> stub.send(KepsenRequest.newBuilder().setName("service-b").build())
            );

            assertEquals(Status.Code.PERMISSION_DENIED, exception.getStatus().getCode());
        } finally {
            channel.shutdownNow();
        }
    }

    private ManagedChannel channelFor(String clientName) throws Exception {
        SslContext sslContext = GrpcSslContexts.forClient()
                .trustManager(new File("certs/dev/ca.crt"))
                .keyManager(
                        new File("certs/dev/" + clientName + ".crt"),
                        new File("certs/dev/" + clientName + ".key")
                )
                .build();

        return NettyChannelBuilder
                .forAddress("localhost", grpcServer.getPort())
                .sslContext(sslContext)
                .overrideAuthority("localhost")
                .build();
    }
}
