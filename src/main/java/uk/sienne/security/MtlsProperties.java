package uk.sienne.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;

@ConfigurationProperties("mtls.server")
@Introspected
public class MtlsProperties {
    private boolean enabled = true;
    private String certChain;
    private String privateKey;
    private String trustCertCollection;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCertChain() {
        return certChain;
    }

    public void setCertChain(String certChain) {
        this.certChain = certChain;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getTrustCertCollection() {
        return trustCertCollection;
    }

    public void setTrustCertCollection(String trustCertCollection) {
        this.trustCertCollection = trustCertCollection;
    }
}
