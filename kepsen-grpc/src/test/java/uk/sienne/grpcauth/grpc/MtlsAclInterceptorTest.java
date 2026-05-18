package uk.sienne.grpcauth.grpc;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import uk.sienne.grpcauth.core.AclConfig;
import uk.sienne.grpcauth.core.ClientIdentityExtractor;
import uk.sienne.grpcauth.core.MethodAclAuthorizer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MtlsAclInterceptorTest {

    @Test
    void skipsCertificateChecksWhenMtlsIsDisabled() {
        AclConfig aclConfig = new AclConfig();
        MethodAclAuthorizer authorizer = new MethodAclAuthorizer(aclConfig, List.of());
        MtlsAclInterceptor interceptor = new MtlsAclInterceptor(
                aclConfig,
                authorizer,
                new ClientIdentityExtractor(),
                false
        );

        TestServerCall call = new TestServerCall();
        TestListener expectedListener = new TestListener();
        TestHandler handler = new TestHandler(expectedListener);

        ServerCall.Listener<byte[]> listener = interceptor.interceptCall(call, new Metadata(), handler);

        assertSame(expectedListener, listener);
        assertTrue(handler.started);
        assertFalse(call.closed);
    }

    private static final class TestHandler implements ServerCallHandler<byte[], byte[]> {
        private final TestListener listener;
        private boolean started;

        private TestHandler(TestListener listener) {
            this.listener = listener;
        }

        @Override
        public ServerCall.Listener<byte[]> startCall(ServerCall<byte[], byte[]> call, Metadata headers) {
            started = true;
            return listener;
        }
    }

    private static final class TestListener extends ServerCall.Listener<byte[]> {
    }

    private static final class TestServerCall extends ServerCall<byte[], byte[]> {
        private boolean closed;

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void sendHeaders(Metadata headers) {
        }

        @Override
        public void sendMessage(byte[] message) {
        }

        @Override
        public void close(Status status, Metadata trailers) {
            closed = true;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public MethodDescriptor<byte[], byte[]> getMethodDescriptor() {
            return MethodDescriptor.<byte[], byte[]>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName("uk.sienne.KepsenService/send")
                    .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
                    .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
                    .build();
        }

        @Override
        public Attributes getAttributes() {
            return Attributes.EMPTY;
        }
    }

    private enum ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        INSTANCE;

        @Override
        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value);
        }

        @Override
        public byte[] parse(InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse test bytes", e);
            }
        }
    }
}
