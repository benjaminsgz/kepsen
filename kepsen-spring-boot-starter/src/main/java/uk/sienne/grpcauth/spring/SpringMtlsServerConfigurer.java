package uk.sienne.grpcauth.spring;

import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import uk.sienne.grpcauth.core.MtlsConfig;
import uk.sienne.grpcauth.grpc.NettyMtlsServerConfigurer;

import java.io.File;
import java.net.URI;

public class SpringMtlsServerConfigurer implements GrpcServerConfigurer {

    private final NettyMtlsServerConfigurer configurer;
    private final MtlsConfig config;

    public SpringMtlsServerConfigurer(MtlsConfigProperties properties) {
        this.config = properties.toMtlsConfig();
        this.configurer = new NettyMtlsServerConfigurer(config);
    }

    @Override
    public void accept(ServerBuilder<?> builder) {
        if (!configurer.isEnabled()) {
            return;
        }

        if (!(builder instanceof NettyServerBuilder nettyBuilder)) {
            if (builder instanceof io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder shadedNettyBuilder) {
                configureShaded(shadedNettyBuilder);
                return;
            }
            throw new IllegalStateException("mTLS requires NettyServerBuilder");
        }

        configurer.configure(nettyBuilder);
    }

    private void configureShaded(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder builder) {
        try {
            File certChain = toFile(config.getCertChain());
            File privateKey = toFile(config.getPrivateKey());
            File trustCa = toFile(config.getTrustCertCollection());

            io.grpc.netty.shaded.io.netty.handler.ssl.SslContext sslContext =
                    io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts.configure(
                            io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder
                                    .forServer(certChain, privateKey)
                                    .trustManager(trustCa)
                                    .clientAuth(io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth.REQUIRE)
                    ).build();

            builder.sslContext(sslContext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure shaded gRPC mTLS", e);
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
