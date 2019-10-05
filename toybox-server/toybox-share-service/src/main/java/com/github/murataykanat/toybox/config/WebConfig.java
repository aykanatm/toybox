package com.github.murataykanat.toybox.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;

@RefreshScope
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    @Value("${toyboxHome}")
    private String toyboxHome;

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        // This code maps the resources to an external source (a folder on disk)
        registry.addResourceHandler("/css/**")
                .addResourceLocations("/resources/", "file:" + toyboxHome
                        + File.separator + "ui" + File.separator
                        + "error" + File.separator  + "css" + File.separator);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("/resources/", "file:" + toyboxHome
                        + File.separator + "ui" + File.separator
                        + "error" + File.separator  + "js" + File.separator);
        registry.addResourceHandler("/images/**")
                .addResourceLocations("/resources/", "file:" + toyboxHome
                        + File.separator + "ui" + File.separator
                        + "error" + File.separator  + "images" + File.separator);
        registry.addResourceHandler("/thirdparty/**")
                .addResourceLocations("/resources/", "file:" + toyboxHome
                        + File.separator + "ui" + File.separator
                        + "thirdparty" + File.separator);
    }
}
