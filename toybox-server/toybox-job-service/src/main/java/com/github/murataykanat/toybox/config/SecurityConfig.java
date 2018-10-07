package com.github.murataykanat.toybox.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;

@EnableOAuth2Sso
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/", "/login**")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and()
                    .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .and()
                    .cors().configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues());
    }
}
