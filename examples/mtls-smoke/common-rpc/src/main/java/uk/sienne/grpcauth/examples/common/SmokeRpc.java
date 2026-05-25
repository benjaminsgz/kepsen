package uk.sienne.grpcauth.examples.common;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import io.netty.handler.ssl.SslContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class SmokeRpc {

    public static final String SPRING_SERVICE = "demo.SpringPeer";
    public static final String MICRONAUT_SERVICE = "demo.MicronautPeer";

    public static final MethodDescriptor<String, String> SPRING_PING = unaryMethod(SPRING_SERVICE, "Ping");
    public static final MethodDescriptor<String, String> MICRONAUT_PING = unaryMethod(MICRONAUT_SERVICE, "Ping");

    public static final String SERVICE_A_IDENTITY = "spiffe://internal/ns/default/sa/service-a";
    public static final String SERVICE_B_IDENTITY = "spiffe://internal/ns/default/sa/service-b";

    private SmokeRpc() {
    }

    public static ServerServiceDefinition unaryService(
            String serviceName,
            MethodDescriptor<String, String> method,
            Function<String, String> handler
    ) {
        return ServerServiceDefinition.builder(serviceName)
                .addMethod(method, ServerCalls.asyncUnaryCall((request, responseObserver) -> {
                    responseObserver.onNext(handler.apply(request));
                    responseObserver.onCompleted();
                }))
                .build();
    }

    public static Function<Integer, String> buildMtlsUnaryCall(
            Path certDir,
            String clientName,
            MethodDescriptor<String, String> method,
            String payload
    ) {
        Objects.requireNonNull(certDir, "certDir");
        Objects.requireNonNull(clientName, "clientName");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(payload, "payload");

        return port -> callWithClientCertificate(certDir, clientName, method, port, payload);
    }

    public static String callWithClientCertificate(
            Path certDir,
            String clientName,
            MethodDescriptor<String, String> method,
            int port,
            String payload
    ) {
        try {
            SslContext sslContext = GrpcSslContexts.forClient()
                    .trustManager(file(certDir, "ca.crt"))
                    .keyManager(file(certDir, clientName + ".crt"), file(certDir, clientName + ".key"))
                    .build();

            return call(certDir, sslContext, method, port, payload);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build mTLS client context", e);
        }
    }

    public static String callWithoutClientCertificate(
            Path certDir,
            MethodDescriptor<String, String> method,
            int port,
            String payload
    ) {
        try {
            SslContext sslContext = GrpcSslContexts.forClient()
                    .trustManager(file(certDir, "ca.crt"))
                    .build();

            return call(certDir, sslContext, method, port, payload);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build TLS client context", e);
        }
    }

    private static String call(
            Path certDir,
            SslContext sslContext,
            MethodDescriptor<String, String> method,
            int port,
            String payload
    ) {
        Channel channel = NettyChannelBuilder.forAddress("127.0.0.1", port)
                .sslContext(sslContext)
                .overrideAuthority("localhost")
                .build();

        try {
            return ClientCalls.blockingUnaryCall(channel, method, CallOptions.DEFAULT, payload);
        } catch (StatusRuntimeException e) {
            throw e;
        } finally {
            if (channel instanceof io.grpc.ManagedChannel managedChannel) {
                managedChannel.shutdownNow();
                try {
                    managedChannel.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static MethodDescriptor<String, String> unaryMethod(String service, String method) {
        return MethodDescriptor.<String, String>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(service, method))
                .setRequestMarshaller(StringMarshaller.INSTANCE)
                .setResponseMarshaller(StringMarshaller.INSTANCE)
                .build();
    }

    private static File file(Path certDir, String name) {
        return certDir.resolve(name).toFile();
    }

    private enum StringMarshaller implements MethodDescriptor.Marshaller<String> {
        INSTANCE;

        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read smoke payload", e);
            }
        }
    }
}
