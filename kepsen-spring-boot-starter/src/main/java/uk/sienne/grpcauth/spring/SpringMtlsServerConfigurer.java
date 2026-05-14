package uk.sienne.grpcauth.spring;

import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import uk.sienne.grpcauth.grpc.NettyMtlsServerConfigurer;

public class SpringMtlsServerConfigurer implements GrpcServerConfigurer {

    private final NettyMtlsServerConfigurer configurer;

    public SpringMtlsServerConfigurer(MtlsConfigProperties properties) {
        this.configurer = new NettyMtlsServerConfigurer(properties.toMtlsConfig());
    }

    @Override
    public void accept(ServerBuilder<?> builder) {
        if (!configurer.isEnabled()) {
            return;
        }

        if (!(builder instanceof NettyServerBuilder nettyBuilder)) {
            throw new IllegalStateException("mTLS requires NettyServerBuilder");
        }

        configurer.configure(nettyBuilder);
    }
}
