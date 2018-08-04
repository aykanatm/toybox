package com.github.murataykanat.toybox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("/resources/", "file:" + System.getenv("TOYBOX_HOME")
                        + File.separator + "ui" + File.separator
                        + "home" + File.separator  + "css" + File.separator);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("/resources/", "file:" + System.getenv("TOYBOX_HOME")
                        + File.separator + "ui" + File.separator
                        + "home" + File.separator  + "js" + File.separator);
        registry.addResourceHandler("/images/**")
                .addResourceLocations("/resources/", "file:" + System.getenv("TOYBOX_HOME")
                        + File.separator + "ui" + File.separator
                        + "home" + File.separator  + "images" + File.separator);
        registry.addResourceHandler("/components/**")
                .addResourceLocations("/resources/", "file:" + System.getenv("TOYBOX_HOME")
                        + File.separator + "ui" + File.separator
                        + "home" + File.separator  + "components" + File.separator);
    }
}
