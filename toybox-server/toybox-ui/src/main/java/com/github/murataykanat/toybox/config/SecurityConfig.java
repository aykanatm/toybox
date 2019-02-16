package com.github.murataykanat.toybox.config;

import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@EnableOAuth2Sso
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.antMatcher("/**")
                .authorizeRequests()
                .antMatchers("/exit**" ,"/", "/login**", "/frameworks/**").permitAll()
                .anyRequest().authenticated()
                .and()
                    .logout()
                    .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                // TODO:
                // Find a way to redirect user to http://localhost:8081/login?logout
                    .logoutSuccessUrl("/exit")
                    .deleteCookies("JSESSIONID", "TSESSION")
                    .invalidateHttpSession(true);
    }
}
