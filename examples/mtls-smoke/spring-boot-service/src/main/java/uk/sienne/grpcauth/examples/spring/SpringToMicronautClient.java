package uk.sienne.grpcauth.examples.spring;

import uk.sienne.grpcauth.examples.common.SmokeRpc;

import java.nio.file.Path;
import java.util.function.Function;

public final class SpringToMicronautClient {

    private SpringToMicronautClient() {
    }

    public static Function<Integer, String> build(Path certDir) {
        return SmokeRpc.buildMtlsUnaryCall(
                certDir,
                "service-a",
                SmokeRpc.MICRONAUT_PING,
                "from-spring"
        );
    }
}
