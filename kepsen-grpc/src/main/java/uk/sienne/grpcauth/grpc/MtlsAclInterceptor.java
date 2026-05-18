package uk.sienne.grpcauth.grpc;

import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.sienne.grpcauth.core.AclConfig;
import uk.sienne.grpcauth.core.ClientIdentityExtractor;
import uk.sienne.grpcauth.core.MethodAclAuthorizer;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

public class MtlsAclInterceptor implements ServerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(MtlsAclInterceptor.class);

    private final AclConfig aclConfig;
    private final MethodAclAuthorizer authorizer;
    private final ClientIdentityExtractor identityExtractor;
    private final boolean mtlsRequired;

    public MtlsAclInterceptor(
            AclConfig aclConfig,
            MethodAclAuthorizer authorizer,
            ClientIdentityExtractor identityExtractor
    ) {
        this(aclConfig, authorizer, identityExtractor, true);
    }

    public MtlsAclInterceptor(
            AclConfig aclConfig,
            MethodAclAuthorizer authorizer,
            ClientIdentityExtractor identityExtractor,
            boolean mtlsRequired
    ) {
        this.aclConfig = aclConfig;
        this.authorizer = authorizer;
        this.identityExtractor = identityExtractor;
        this.mtlsRequired = mtlsRequired;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next
    ) {
        String fullMethodName = call.getMethodDescriptor().getFullMethodName();

        if (!mtlsRequired) {
            return next.startCall(call, headers);
        }

        SSLSession sslSession = call.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
        if (sslSession == null) {
            LOG.warn("decision=deny reason=missing_mtls method={}", fullMethodName);
            call.close(
                    Status.UNAUTHENTICATED.withDescription("mTLS is required"),
                    new Metadata()
            );
            return new ServerCall.Listener<>() {
            };
        }

        String clientIdentity;
        try {
            X509Certificate cert = firstPeerX509Certificate(sslSession);
            clientIdentity = identityExtractor.extract(cert, aclConfig.getIdentitySource());
        } catch (Exception e) {
            LOG.warn("decision=deny reason=invalid_client_certificate method={}", fullMethodName);
            call.close(
                    Status.UNAUTHENTICATED.withDescription("Invalid client certificate"),
                    new Metadata()
            );
            return new ServerCall.Listener<>() {
            };
        }

        if (!authorizer.isAllowed(clientIdentity, fullMethodName)) {
            LOG.warn(
                    "decision=deny reason=acl_miss method={} client_hash={}",
                    fullMethodName,
                    hashIdentity(clientIdentity)
            );
            call.close(
                    Status.PERMISSION_DENIED.withDescription("Client is not authorized to call method"),
                    new Metadata()
            );
            return new ServerCall.Listener<>() {
            };
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "decision=allow reason=acl_match method={} client_hash={}",
                    fullMethodName,
                    hashIdentity(clientIdentity)
            );
        }
        return next.startCall(call, headers);
    }

    private X509Certificate firstPeerX509Certificate(SSLSession session)
            throws SSLPeerUnverifiedException {
        Certificate[] peerCerts = session.getPeerCertificates();

        if (peerCerts.length == 0 || !(peerCerts[0] instanceof X509Certificate cert)) {
            throw new SSLPeerUnverifiedException("No X509 client certificate found");
        }

        return cert;
    }

    private String hashIdentity(String clientIdentity) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(clientIdentity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
