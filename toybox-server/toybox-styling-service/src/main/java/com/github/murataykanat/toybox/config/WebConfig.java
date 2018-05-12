package com.github.murataykanat.toybox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {
    // Since, potentially, we want to access the styling from different domains,
    // we allow different domains to access the static content that is served on
    // this service
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/js/**").addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/style/**").addResourceLocations("classpath:/static/style/");
    }
}
