package uk.sienne.security;

import io.grpc.ServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.inject.Singleton;

import java.io.File;
import java.net.URI;

@Singleton
public class GrpcMtlsServerCustomizer implements BeanCreatedEventListener<ServerBuilder<?>> {

    private final MtlsProperties props;

    public GrpcMtlsServerCustomizer(MtlsProperties props) {
        this.props = props;
    }

    @Override
    public ServerBuilder<?> onCreated(BeanCreatedEvent<ServerBuilder<?>> event) {
        ServerBuilder<?> builder = event.getBean();

        if (!props.isEnabled()) {
            return builder;
        }

        if (!(builder instanceof NettyServerBuilder nettyBuilder)) {
            throw new IllegalStateException("mTLS requires NettyServerBuilder");
        }

        try {
            File certChain = toFile(props.getCertChain());
            File privateKey = toFile(props.getPrivateKey());
            File trustCa = toFile(props.getTrustCertCollection());

            SslContext sslContext = GrpcSslContexts.configure(
                    SslContextBuilder
                            .forServer(certChain, privateKey)
                            .trustManager(trustCa)
                            .clientAuth(ClientAuth.REQUIRE)
            ).build();

            nettyBuilder.sslContext(sslContext);
            return nettyBuilder;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure gRPC mTLS", e);
        }
    }

    private File toFile(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("TLS file location is blank");
        }

        if (location.startsWith("file:")) {
            if (location.startsWith("file:/")) {
                return new File(URI.create(location));
            }
            return new File(location.substring("file:".length()));
        }

        return new File(location);
    }
}
