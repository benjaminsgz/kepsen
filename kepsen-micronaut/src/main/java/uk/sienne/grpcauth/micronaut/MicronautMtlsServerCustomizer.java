package uk.sienne.grpcauth.micronaut;

import io.grpc.ServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Singleton;
import uk.sienne.grpcauth.grpc.NettyMtlsServerConfigurer;

@Singleton
public class MicronautMtlsServerCustomizer implements BeanCreatedEventListener<ServerBuilder<?>> {

    private final NettyMtlsServerConfigurer configurer;

    public MicronautMtlsServerCustomizer(MicronautMtlsProperties properties) {
        this.configurer = new NettyMtlsServerConfigurer(properties.toMtlsConfig());
    }

    @Override
    public ServerBuilder<?> onCreated(BeanCreatedEvent<ServerBuilder<?>> event) {
        ServerBuilder<?> builder = event.getBean();

        if (!configurer.isEnabled()) {
            return builder;
        }

        if (!(builder instanceof NettyServerBuilder nettyBuilder)) {
            throw new IllegalStateException("mTLS requires NettyServerBuilder");
        }

        configurer.configure(nettyBuilder);
        return nettyBuilder;
    }
}
