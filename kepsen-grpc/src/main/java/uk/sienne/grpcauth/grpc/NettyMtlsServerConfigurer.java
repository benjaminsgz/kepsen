package uk.sienne.grpcauth.grpc;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import uk.sienne.grpcauth.core.MtlsConfig;

import java.io.File;
import java.net.URI;

public class NettyMtlsServerConfigurer {

    private final MtlsConfig config;

    public NettyMtlsServerConfigurer(MtlsConfig config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void configure(NettyServerBuilder builder) {
        if (!config.isEnabled()) {
            return;
        }

        try {
            File certChain = toFile(config.getCertChain());
            File privateKey = toFile(config.getPrivateKey());
            File trustCa = toFile(config.getTrustCertCollection());

            SslContext sslContext = GrpcSslContexts.configure(
                    SslContextBuilder
                            .forServer(certChain, privateKey)
                            .trustManager(trustCa)
                            .clientAuth(ClientAuth.REQUIRE)
            ).build();

            builder.sslContext(sslContext);
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
