package uk.sienne.grpcauth.spring;

import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import net.devh.boot.grpc.server.serverfactory.GrpcServerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import uk.sienne.grpcauth.core.AclConfig;
import uk.sienne.grpcauth.core.ClientIdentityExtractor;
import uk.sienne.grpcauth.core.MethodAclAuthorizer;
import uk.sienne.grpcauth.grpc.MtlsAclInterceptor;

@AutoConfiguration
@EnableConfigurationProperties({AclConfigProperties.class, MtlsConfigProperties.class})
public class GrpcMtlsAutoConfiguration {

    @Bean
    AclConfig aclConfig(AclConfigProperties properties) {
        return properties.toAclConfig();
    }

    @Bean
    ClientIdentityExtractor clientIdentityExtractor() {
        return new ClientIdentityExtractor();
    }

    @Bean
    MethodAclAuthorizer methodAclAuthorizer(AclConfigProperties properties) {
        return new MethodAclAuthorizer(properties.toAclConfig(), properties.toAclRules());
    }

    @Bean
    @GrpcGlobalServerInterceptor
    MtlsAclInterceptor mtlsAclInterceptor(
            AclConfig aclConfig,
            MtlsConfigProperties mtlsProperties,
            MethodAclAuthorizer authorizer,
            ClientIdentityExtractor identityExtractor
    ) {
        return new MtlsAclInterceptor(aclConfig, authorizer, identityExtractor, mtlsProperties.isEnabled());
    }

    @Bean
    GrpcServerConfigurer grpcServerConfigurer(MtlsConfigProperties properties) {
        return new SpringMtlsServerConfigurer(properties);
    }
}
