package microsec.freddysbbq.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClientHttpRequestFactory;
import org.springframework.cloud.security.oauth2.resource.EnableOAuth2Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

import microsec.common.Targets;
import microsec.freddysbbq.order.model.v1.Order;

@SpringBootApplication
@EntityScan(basePackageClasses = Order.class)
@EnableOAuth2Resource
@EnableDiscoveryClient
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @Configuration
    public static class RepositoryConfig extends RepositoryRestMvcConfiguration {
        @Override
        protected void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
            config.exposeIdsFor(Order.class);
        }
    }

    @Bean
    public Targets targets() {
        return new Targets();
    }

    @Bean
    public ResourceServerConfigurer resourceServer(SecurityProperties securityProperties) {
        return new ResourceServerConfigurerAdapter() {
            @Override
            public void configure(ResourceServerSecurityConfigurer resources) {
                resources.resourceId("order");
            }

            @Override
            public void configure(HttpSecurity http) throws Exception {
                if (securityProperties.isRequireSsl()) {
                    http.requiresChannel().anyRequest().requiresSecure();
                }
                http.authorizeRequests()
                        .antMatchers("/orders/**").access("#oauth2.hasScope('order.admin')");
            }
        };
    }
    
    @Bean
    @ConfigurationProperties(prefix="spring.oauth2.client")
    public ClientCredentialsResourceDetails clientCredentials() {
    	return new ClientCredentialsResourceDetails();
    }
    
    @Bean
    @LoadBalanced
    public OAuth2RestTemplate clientCredentialsRestTemplate(
    		ClientCredentialsResourceDetails clientCredentials,
    		RibbonClientHttpRequestFactory requestFactory) {
    	OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(clientCredentials);
    	oAuth2RestTemplate.setRequestFactory(requestFactory);
		return oAuth2RestTemplate;
    }
}