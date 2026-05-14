package uk.sienne.grpcauth.core;

import uk.sienne.grpcauth.core.constants.AclAction;
import uk.sienne.grpcauth.core.constants.IdentitySource;

public class AclConfig {

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
}
