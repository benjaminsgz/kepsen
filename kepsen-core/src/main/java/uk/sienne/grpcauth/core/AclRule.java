package uk.sienne.grpcauth.core;

import java.util.ArrayList;
import java.util.List;

public class AclRule {

    private String name;
    private String method;
    private List<String> allowedClients = new ArrayList<>();

    public AclRule() {
    }

    public AclRule(String name, String method, List<String> allowedClients) {
        this.name = name;
        this.method = method;
        this.allowedClients = allowedClients == null ? new ArrayList<>() : new ArrayList<>(allowedClients);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
