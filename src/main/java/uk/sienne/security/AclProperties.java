package uk.sienne.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Introspected;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("service-acl")
@Introspected
public class AclProperties {
    private boolean enabled = true;
    private String defaultAction = "deny";
    private String identitySource = "san-uri";
    private List<Rule> rules = new ArrayList<>();

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

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    @Introspected
    public static class Rule {
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
            this.allowedClients = allowedClients;
        }
    }
}
