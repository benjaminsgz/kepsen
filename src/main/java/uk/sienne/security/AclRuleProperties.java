package uk.sienne.security;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Introspected;

import java.util.ArrayList;
import java.util.List;

@EachProperty("service-acl.rules")
@Introspected
public class AclRuleProperties {
    private final String name;
    private String method;
    private List<String> allowedClients = new ArrayList<>();

    public AclRuleProperties(@Parameter String name) {
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
        this.allowedClients = allowedClients;
    }
}
