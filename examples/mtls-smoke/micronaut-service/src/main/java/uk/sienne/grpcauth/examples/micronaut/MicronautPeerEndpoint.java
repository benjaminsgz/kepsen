package uk.sienne.grpcauth.examples.micronaut;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import jakarta.inject.Singleton;
import uk.sienne.grpcauth.examples.common.SmokeRpc;

@Singleton
final class MicronautPeerEndpoint implements BindableService {

    @Override
    public ServerServiceDefinition bindService() {
        return SmokeRpc.unaryService(
                SmokeRpc.MICRONAUT_SERVICE,
                SmokeRpc.MICRONAUT_PING,
                request -> "micronaut:" + request
        );
    }
}
