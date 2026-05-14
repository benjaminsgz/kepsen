package uk.sienne.security;

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
        AclProperties props = properties();
        props.setDefaultAction("maybe");

        assertThrows(IllegalArgumentException.class, () -> new MethodAclAuthorizer(props, List.of()));
    }

    private AclProperties properties() {
        AclProperties props = new AclProperties();
        props.setEnabled(true);
        props.setDefaultAction("deny");
        props.setIdentitySource("san-uri");
        return props;
    }

    private AclRuleProperties rule(String method, String allowedClient) {
        AclRuleProperties rule = new AclRuleProperties("test");
        rule.setMethod(method);
        rule.setAllowedClients(List.of(allowedClient));
        return rule;
    }
}
