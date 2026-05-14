package uk.sienne.grpcauth.core;

import uk.sienne.grpcauth.core.constants.IdentitySource;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ClientIdentityExtractor {

    private static final int SAN_URI = 6;

    public String extract(X509Certificate cert, String mode) {
        if (IdentitySource.SAN_URI.equalsIgnoreCase(mode)) {
            return extractSanUri(cert)
                    .orElseThrow(() -> new IllegalArgumentException("Client certificate has no SAN URI"));
        }

        if (IdentitySource.CN.equalsIgnoreCase(mode)) {
            return extractCommonName(cert)
                    .orElseThrow(() -> new IllegalArgumentException("Client certificate has no CN"));
        }

        if (IdentitySource.SAN_URI_THEN_CN.equalsIgnoreCase(mode)) {
            return extractSanUri(cert)
                    .or(() -> extractCommonName(cert))
                    .orElseThrow(() -> new IllegalArgumentException("Client certificate has no SAN URI or CN"));
        }

        throw new IllegalArgumentException("Unsupported identity source: " + mode);
    }

    private Optional<String> extractSanUri(X509Certificate cert) {
        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans == null) {
                return Optional.empty();
            }

            for (List<?> san : sans) {
                if (san.size() >= 2 && san.get(0) instanceof Integer type && type == SAN_URI) {
                    return Optional.of(String.valueOf(san.get(1)));
                }
            }

            return Optional.empty();
        } catch (CertificateParsingException e) {
            throw new IllegalArgumentException("Failed to parse certificate SAN", e);
        }
    }

    private Optional<String> extractCommonName(X509Certificate cert) {
        try {
            LdapName ldapName = new LdapName(cert.getSubjectX500Principal().getName());

            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return Optional.of(String.valueOf(rdn.getValue()));
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse certificate CN", e);
        }
    }
}
