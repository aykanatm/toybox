package com.github.murataykanat.toybox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("/resources/", "file:" + System.getenv("TOYBOX_HOME")
                        + File.separator + "ui" + File.separator
                        + "login" + File.separator  + "css" + File.separator);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("/resources/", "file:" + System.getenv("TOYBOX_HOME")
                        + File.separator + "ui" + File.separator
                        + "login" + File.separator  + "js" + File.separator);
    }
}
