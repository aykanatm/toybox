package com.github.murataykanat.toybox.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.multipart.support.MultipartFilter;

import javax.servlet.ServletContext;

public class SecurityApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

    // This code makes sure the multipart file filter is before the security filter
    @Override
    protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
        insertFilters(servletContext, new MultipartFilter());
    }
}
