package uk.sienne.grpcauth.examples.spring;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import net.devh.boot.grpc.server.service.GrpcService;
import uk.sienne.grpcauth.examples.common.SmokeRpc;

@GrpcService
final class SpringPeerEndpoint implements BindableService {

    @Override
    public ServerServiceDefinition bindService() {
        return SmokeRpc.unaryService(
                SmokeRpc.SPRING_SERVICE,
                SmokeRpc.SPRING_PING,
                request -> "spring:" + request
        );
    }
}
