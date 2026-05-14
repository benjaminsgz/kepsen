package uk.sienne.grpcauth.micronaut;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;
import uk.sienne.grpcauth.core.AclConfig;
import uk.sienne.grpcauth.core.constants.AclAction;
import uk.sienne.grpcauth.core.constants.ConfigKeys;
import uk.sienne.grpcauth.core.constants.IdentitySource;

@ConfigurationProperties(ConfigKeys.ACL_PREFIX)
@Introspected
public class MicronautAclProperties {

    private boolean enabled = true;
    private String defaultAction = AclAction.DENY;
    private String identitySource = IdentitySource.SAN_URI;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultAction() {
        return defaultAction;
    }

    public void setDefaultAction(String defaultAction) {
        this.defaultAction = defaultAction;
    }

    public String getIdentitySource() {
        return identitySource;
    }

    public void setIdentitySource(String identitySource) {
        this.identitySource = identitySource;
    }

    public AclConfig toAclConfig() {
        AclConfig config = new AclConfig();
        config.setEnabled(enabled);
        config.setDefaultAction(defaultAction);
        config.setIdentitySource(identitySource);
        return config;
    }
}
