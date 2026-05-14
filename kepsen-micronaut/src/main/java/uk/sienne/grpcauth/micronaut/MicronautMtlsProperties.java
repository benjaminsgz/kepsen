package uk.sienne.grpcauth.micronaut;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import uk.sienne.grpcauth.core.MtlsConfig;
import uk.sienne.grpcauth.core.constants.ConfigKeys;

@ConfigurationProperties(ConfigKeys.MTLS_PREFIX)
@Introspected
public class MicronautMtlsProperties {

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

    public MtlsConfig toMtlsConfig() {
        MtlsConfig config = new MtlsConfig();
        config.setEnabled(enabled);
        config.setCertChain(certChain);
        config.setPrivateKey(privateKey);
        config.setTrustCertCollection(trustCertCollection);
        return config;
    }
}
