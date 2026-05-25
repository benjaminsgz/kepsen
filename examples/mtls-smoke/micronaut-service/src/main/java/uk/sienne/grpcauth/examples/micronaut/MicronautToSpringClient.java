package uk.sienne.grpcauth.examples.micronaut;

import uk.sienne.grpcauth.examples.common.SmokeRpc;

import java.nio.file.Path;
import java.util.function.Function;

public final class MicronautToSpringClient {

    private MicronautToSpringClient() {
    }

    public static Function<Integer, String> build(Path certDir) {
        return SmokeRpc.buildMtlsUnaryCall(
                certDir,
                "service-b",
                SmokeRpc.SPRING_PING,
                "from-micronaut"
        );
    }
}
