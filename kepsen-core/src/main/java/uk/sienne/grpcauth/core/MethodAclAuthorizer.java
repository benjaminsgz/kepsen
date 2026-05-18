package uk.sienne.grpcauth.core;

import uk.sienne.grpcauth.core.constants.AclAction;
import uk.sienne.grpcauth.core.constants.AclPattern;
import uk.sienne.grpcauth.core.constants.ConfigKeys;
import uk.sienne.grpcauth.core.constants.IdentitySource;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MethodAclAuthorizer {

    private final AclConfig config;
    private final Map<String, Set<String>> exactMethodClients;
    private final Map<String, Set<String>> serviceClients;
    private final Set<String> globalClients;

    public MethodAclAuthorizer(AclConfig config, List<AclRule> rules) {
        this.config = config;
        List<AclRule> ruleCopy = List.copyOf(rules);
        validate(config, ruleCopy);

        Map<String, Set<String>> exact = new LinkedHashMap<>();
        Map<String, Set<String>> service = new LinkedHashMap<>();
        Set<String> global = new HashSet<>();

        for (AclRule rule : ruleCopy) {
            Set<String> target = targetSet(rule.getMethod(), exact, service, global);
            target.addAll(rule.getAllowedClients());
        }

        this.exactMethodClients = freeze(exact);
        this.serviceClients = freeze(service);
        this.globalClients = Set.copyOf(global);
    }

    public boolean isAllowed(String clientIdentity, String fullMethodName) {
        if (!config.isEnabled()) {
            return true;
        }

        int slash = fullMethodName.indexOf('/');
        String serviceName = slash < 0 ? fullMethodName : fullMethodName.substring(0, slash);

        boolean matchedRule = false;

        Set<String> exactClients = exactMethodClients.get(fullMethodName);
        if (exactClients != null) {
            matchedRule = true;
            if (exactClients.contains(clientIdentity)) {
                return true;
            }
        }

        Set<String> serviceAllowedClients = serviceClients.get(serviceName);
        if (serviceAllowedClients != null) {
            matchedRule = true;
            if (serviceAllowedClients.contains(clientIdentity)) {
                return true;
            }
        }

        if (!globalClients.isEmpty()) {
            matchedRule = true;
            if (globalClients.contains(clientIdentity)) {
                return true;
            }
        }

        if (matchedRule) {
            return false;
        }

        return AclAction.ALLOW.equalsIgnoreCase(config.getDefaultAction());
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

    private Set<String> targetSet(
            String method,
            Map<String, Set<String>> exact,
            Map<String, Set<String>> service,
            Set<String> global
    ) {
        if (AclPattern.WILDCARD_ALL.equals(method)) {
            return global;
        }

        if (method.endsWith(AclPattern.WILDCARD_SERVICE)) {
            String serviceName = method.substring(0, method.length() - AclPattern.WILDCARD_SERVICE.length());
            return service.computeIfAbsent(serviceName, ignored -> new HashSet<>());
        }

        return exact.computeIfAbsent(method, ignored -> new HashSet<>());
    }

    private Map<String, Set<String>> freeze(Map<String, Set<String>> source) {
        Map<String, Set<String>> frozen = new LinkedHashMap<>(source.size());
        for (Map.Entry<String, Set<String>> entry : source.entrySet()) {
            frozen.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Map.copyOf(frozen);
    }
}
