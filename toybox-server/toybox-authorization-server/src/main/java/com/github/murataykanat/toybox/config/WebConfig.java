package com.github.murataykanat.toybox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;

@RefreshScope
@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
    @Value("${toyboxHome}")
    private String toyboxHome;

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("/resources/", "file:" + toyboxHome
                        + File.separator + "ui" + File.separator
                        + "login" + File.separator  + "css" + File.separator);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("/resources/", "file:" + toyboxHome
                        + File.separator + "ui" + File.separator
                        + "login" + File.separator  + "js" + File.separator);
        registry.addResourceHandler("/images/**")
                .addResourceLocations("/resources/", "file:" + toyboxHome
                        + File.separator + "ui" + File.separator
                        + "login" + File.separator  + "images" + File.separator);
    }
}
