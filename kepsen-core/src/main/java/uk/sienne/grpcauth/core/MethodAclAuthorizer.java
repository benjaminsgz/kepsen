package uk.sienne.grpcauth.core;

import uk.sienne.grpcauth.core.constants.AclAction;
import uk.sienne.grpcauth.core.constants.AclPattern;
import uk.sienne.grpcauth.core.constants.ConfigKeys;
import uk.sienne.grpcauth.core.constants.IdentitySource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodAclAuthorizer {

    private final AclConfig config;
    private final List<AclRule> rules;

    public MethodAclAuthorizer(AclConfig config, List<AclRule> rules) {
        this.config = config;
        this.rules = List.copyOf(rules);
        validate(config, this.rules);
    }

    public boolean isAllowed(String clientIdentity, String fullMethodName) {
        if (!config.isEnabled()) {
            return true;
        }

        String serviceName = fullMethodName.split("/", 2)[0];

        for (AclRule rule : rules) {
            if (matches(rule.getMethod(), fullMethodName, serviceName)) {
                Set<String> allowedClients = new HashSet<>(rule.getAllowedClients());
                if (allowedClients.contains(clientIdentity)) {
                    return true;
                }
            }
        }

        return AclAction.ALLOW.equalsIgnoreCase(config.getDefaultAction());
    }

    private boolean matches(String pattern, String fullMethodName, String serviceName) {
        return pattern.equals(fullMethodName)
                || pattern.equals(serviceName + AclPattern.WILDCARD_SERVICE)
                || pattern.equals(AclPattern.WILDCARD_ALL);
    }

    private void validate(AclConfig config, List<AclRule> rules) {
        if (!AclAction.DENY.equalsIgnoreCase(config.getDefaultAction())
                && !AclAction.ALLOW.equalsIgnoreCase(config.getDefaultAction())) {
            throw new IllegalArgumentException(ConfigKeys.ACL_PREFIX + ".default-action must be deny or allow");
        }

        String identitySource = config.getIdentitySource();
        if (!IdentitySource.SAN_URI.equalsIgnoreCase(identitySource)
                && !IdentitySource.CN.equalsIgnoreCase(identitySource)
                && !IdentitySource.SAN_URI_THEN_CN.equalsIgnoreCase(identitySource)) {
            throw new IllegalArgumentException(
                    ConfigKeys.ACL_PREFIX + ".identity-source is unsupported: " + identitySource
            );
        }

        for (AclRule rule : rules) {
            if (rule.getMethod() == null || rule.getMethod().isBlank()) {
                throw new IllegalArgumentException("ACL rule method is blank: " + rule.getName());
            }

            if (rule.getAllowedClients() == null || rule.getAllowedClients().isEmpty()) {
                throw new IllegalArgumentException("ACL rule allowed-clients is empty: " + rule.getMethod());
            }
        }
    }
}
