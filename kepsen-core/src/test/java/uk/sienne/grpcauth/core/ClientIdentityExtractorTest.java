package uk.sienne.grpcauth.core;

import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientIdentityExtractorTest {

    private final ClientIdentityExtractor extractor = new ClientIdentityExtractor();

    @Test
    void extractsSanUriFirst() {
        X509Certificate cert = new TestCertificate(
                List.of(List.of(6, "spiffe://internal/ns/default/sa/service-a")),
                "CN=service-a"
        );

        assertEquals(
                "spiffe://internal/ns/default/sa/service-a",
                extractor.extract(cert, "san-uri")
        );
    }

    @Test
    void canFallbackToCnForDevelopment() {
        X509Certificate cert = new TestCertificate(null, "CN=service-a,OU=dev");

        assertEquals("service-a", extractor.extract(cert, "san-uri-then-cn"));
    }

    @Test
    void rejectsMissingSanUriInStrictMode() {
        X509Certificate cert = new TestCertificate(null, "CN=service-a");

        assertThrows(IllegalArgumentException.class, () -> extractor.extract(cert, "san-uri"));
    }

    private static final class TestCertificate extends X509Certificate {
        private final Collection<List<?>> subjectAlternativeNames;
        private final X500Principal subject;

        private TestCertificate(Collection<List<?>> subjectAlternativeNames, String subjectName) {
            this.subjectAlternativeNames = subjectAlternativeNames;
            this.subject = new X500Principal(subjectName);
        }

        @Override
        public Collection<List<?>> getSubjectAlternativeNames() {
            return subjectAlternativeNames;
        }

        @Override
        public X500Principal getSubjectX500Principal() {
            return subject;
        }

        @Override
        public void checkValidity() {
        }

        @Override
        public void checkValidity(Date date) {
        }

        @Override
        public int getVersion() {
            return 3;
        }

        @Override
        public BigInteger getSerialNumber() {
            return BigInteger.ONE;
        }

        @Override
        public Principal getIssuerDN() {
            return subject;
        }

        @Override
        public Principal getSubjectDN() {
            return subject;
        }

        @Override
        public Date getNotBefore() {
            return new Date(0L);
        }

        @Override
        public Date getNotAfter() {
            return new Date(Long.MAX_VALUE);
        }

        @Override
        public byte[] getTBSCertificate() {
            return new byte[0];
        }

        @Override
        public byte[] getSignature() {
            return new byte[0];
        }

        @Override
        public String getSigAlgName() {
            return "none";
        }

        @Override
        public String getSigAlgOID() {
            return "0.0";
        }

        @Override
        public byte[] getSigAlgParams() {
            return new byte[0];
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return new boolean[0];
        }

        @Override
        public boolean[] getKeyUsage() {
            return new boolean[0];
        }

        @Override
        public int getBasicConstraints() {
            return -1;
        }

        @Override
        public byte[] getEncoded() {
            return new byte[0];
        }

        @Override
        public void verify(PublicKey key) {
        }

        @Override
        public void verify(PublicKey key, String sigProvider) {
        }

        @Override
        public String toString() {
            return subject.getName();
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return false;
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return Set.of();
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return Set.of();
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return new byte[0];
        }
    }
}
