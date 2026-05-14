package uk.sienne;

import io.grpc.stub.StreamObserver;
import jakarta.inject.Singleton;

@Singleton
public class KepsenEndpoint extends KepsenServiceGrpc.KepsenServiceImplBase {

    @Override
    public void send(KepsenRequest request, StreamObserver<KepsenReply> responseObserver) {
        KepsenReply reply = KepsenReply.newBuilder()
                .setMessage("Hello " + request.getName())
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
