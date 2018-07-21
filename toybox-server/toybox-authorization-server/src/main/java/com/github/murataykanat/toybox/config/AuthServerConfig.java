package com.github.murataykanat.toybox.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;

import javax.sql.DataSource;

@Configuration
public class AuthServerConfig extends AuthorizationServerConfigurerAdapter {
    @Autowired
    DataSource dataSource;

    @Override
    public void configure(
            AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
        oauthServer.tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()");
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clientDetailsServiceConfigurer) throws Exception {
        clientDetailsServiceConfigurer.jdbc(dataSource);
    }
}
