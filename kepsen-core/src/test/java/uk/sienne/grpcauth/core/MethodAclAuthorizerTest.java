package uk.sienne.grpcauth.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodAclAuthorizerTest {

    @Test
    void allowsExactMethodMatch() {
        MethodAclAuthorizer authorizer = new MethodAclAuthorizer(properties(), List.of(
                rule("uk.sienne.KepsenService/send", "spiffe://internal/ns/default/sa/service-a")
        ));

        assertTrue(authorizer.isAllowed(
                "spiffe://internal/ns/default/sa/service-a",
                "uk.sienne.KepsenService/send"
        ));
    }

    @Test
    void allowsServiceWildcardMatch() {
        MethodAclAuthorizer authorizer = new MethodAclAuthorizer(properties(), List.of(
                rule("uk.sienne.KepsenService/*", "spiffe://internal/ns/default/sa/service-b")
        ));

        assertTrue(authorizer.isAllowed(
                "spiffe://internal/ns/default/sa/service-b",
                "uk.sienne.KepsenService/send"
        ));
    }

    @Test
    void deniesByDefaultWhenNoRuleMatches() {
        MethodAclAuthorizer authorizer = new MethodAclAuthorizer(properties(), List.of(
                rule("uk.sienne.KepsenService/send", "spiffe://internal/ns/default/sa/service-a")
        ));

        assertFalse(authorizer.isAllowed(
                "spiffe://internal/ns/default/sa/service-b",
                "uk.sienne.KepsenService/send"
        ));
    }

    @Test
    void failsFastOnInvalidDefaultAction() {
        AclConfig config = properties();
        config.setDefaultAction("maybe");

        assertThrows(IllegalArgumentException.class, () -> new MethodAclAuthorizer(config, List.of()));
    }

    private AclConfig properties() {
        AclConfig config = new AclConfig();
        config.setEnabled(true);
        config.setDefaultAction("deny");
        config.setIdentitySource("san-uri");
        return config;
    }

    private AclRule rule(String method, String allowedClient) {
        return new AclRule("test", method, List.of(allowedClient));
    }
}
