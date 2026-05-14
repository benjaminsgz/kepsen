package uk.sienne.security;

import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class MethodAclAuthorizer {

    private final AclProperties props;

    public MethodAclAuthorizer(AclProperties props) {
        this.props = props;
        validate(props);
    }

    public boolean isAllowed(String clientIdentity, String fullMethodName) {
        if (!props.isEnabled()) {
            return true;
        }

        String serviceName = fullMethodName.split("/", 2)[0];

        for (AclProperties.Rule rule : props.getRules()) {
            if (matches(rule.getMethod(), fullMethodName, serviceName)) {
                Set<String> allowed = new HashSet<>(rule.getAllowedClients());
                if (allowed.contains(clientIdentity)) {
                    return true;
                }
            }
        }

        return "allow".equalsIgnoreCase(props.getDefaultAction());
    }

    private boolean matches(String pattern, String fullMethodName, String serviceName) {
        return pattern.equals(fullMethodName)
                || pattern.equals(serviceName + "/*")
                || pattern.equals("*");
    }

    private void validate(AclProperties props) {
        if (!"deny".equalsIgnoreCase(props.getDefaultAction())
                && !"allow".equalsIgnoreCase(props.getDefaultAction())) {
            throw new IllegalArgumentException("service-acl.default-action must be deny or allow");
        }

        String identitySource = props.getIdentitySource();
        if (!"san-uri".equalsIgnoreCase(identitySource)
                && !"cn".equalsIgnoreCase(identitySource)
                && !"san-uri-then-cn".equalsIgnoreCase(identitySource)) {
            throw new IllegalArgumentException("service-acl.identity-source is unsupported: " + identitySource);
        }

        List<AclProperties.Rule> rules = props.getRules();
        if (rules == null) {
            throw new IllegalArgumentException("service-acl.rules must not be null");
        }

        for (AclProperties.Rule rule : rules) {
            if (rule.getMethod() == null || rule.getMethod().isBlank()) {
                throw new IllegalArgumentException("ACL rule method is blank");
            }

            if (rule.getAllowedClients() == null || rule.getAllowedClients().isEmpty()) {
                throw new IllegalArgumentException("ACL rule allowed-clients is empty: " + rule.getMethod());
            }
        }
    }
}
