package uk.sienne.grpcauth.micronaut;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Introspected;
import uk.sienne.grpcauth.core.AclRule;
import uk.sienne.grpcauth.core.constants.ConfigKeys;

import java.util.ArrayList;
import java.util.List;

@EachProperty(ConfigKeys.ACL_RULES)
@Introspected
public class MicronautAclRuleProperties {

    private final String name;
    private String method;
    private List<String> allowedClients = new ArrayList<>();

    public MicronautAclRuleProperties(@Parameter String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

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

    public AclRule toAclRule() {
        return new AclRule(name, method, allowedClients);
    }
}
