package uk.sienne.grpcauth.micronaut;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import uk.sienne.grpcauth.core.AclRule;
import uk.sienne.grpcauth.core.ClientIdentityExtractor;
import uk.sienne.grpcauth.core.MethodAclAuthorizer;
import uk.sienne.grpcauth.grpc.MtlsAclInterceptor;

import java.util.List;

@Factory
public class MtlsAclInterceptorFactory {

    @Singleton
    ClientIdentityExtractor clientIdentityExtractor() {
        return new ClientIdentityExtractor();
    }

    @Singleton
    MethodAclAuthorizer methodAclAuthorizer(
            MicronautAclProperties properties,
            List<MicronautAclRuleProperties> ruleProperties
    ) {
        List<AclRule> rules = ruleProperties.stream()
                .map(MicronautAclRuleProperties::toAclRule)
                .toList();
        return new MethodAclAuthorizer(properties.toAclConfig(), rules);
    }

    @Singleton
    MtlsAclInterceptor mtlsAclInterceptor(
            MicronautAclProperties properties,
            MicronautMtlsProperties mtlsProperties,
            MethodAclAuthorizer authorizer,
            ClientIdentityExtractor identityExtractor
    ) {
        return new MtlsAclInterceptor(
                properties.toAclConfig(),
                authorizer,
                identityExtractor,
                mtlsProperties.isEnabled()
        );
    }
}
