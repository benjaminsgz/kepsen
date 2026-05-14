package uk.sienne.grpcauth.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.sienne.grpcauth.core.AclConfig;
import uk.sienne.grpcauth.core.AclRule;
import uk.sienne.grpcauth.core.constants.AclAction;
import uk.sienne.grpcauth.core.constants.ConfigKeys;
import uk.sienne.grpcauth.core.constants.IdentitySource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(ConfigKeys.ACL_PREFIX)
public class AclConfigProperties {

    private boolean enabled = true;
    private String defaultAction = AclAction.DENY;
    private String identitySource = IdentitySource.SAN_URI;
    private Map<String, RuleEntry> rules = new LinkedHashMap<>();

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

    public Map<String, RuleEntry> getRules() {
        return rules;
    }

    public void setRules(Map<String, RuleEntry> rules) {
        this.rules = rules == null ? new LinkedHashMap<>() : new LinkedHashMap<>(rules);
    }

    public AclConfig toAclConfig() {
        AclConfig config = new AclConfig();
        config.setEnabled(enabled);
        config.setDefaultAction(defaultAction);
        config.setIdentitySource(identitySource);
        return config;
    }

    public List<AclRule> toAclRules() {
        return rules.entrySet().stream()
                .map(entry -> new AclRule(
                        entry.getKey(),
                        entry.getValue().getMethod(),
                        entry.getValue().getAllowedClients()
                ))
                .toList();
    }

    public static class RuleEntry {

        private String method;
        private List<String> allowedClients = new ArrayList<>();

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public List<String> getAllowedClients() {
            return allowedClients;
        }

        public void setAllowedClients(List<String> allowedClients) {
            this.allowedClients = allowedClients == null ? new ArrayList<>() : new ArrayList<>(allowedClients);
        }
    }
}
