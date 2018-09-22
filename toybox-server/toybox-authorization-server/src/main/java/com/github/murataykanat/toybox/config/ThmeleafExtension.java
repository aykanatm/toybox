package com.github.murataykanat.toybox.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import javax.annotation.PostConstruct;
import java.io.File;

@RefreshScope
@Configuration
public class ThmeleafExtension {
    @Value("${toyboxHome}")
    private String toyboxHome;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @PostConstruct
    public void extension(){
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(toyboxHome + File.separator + "ui" + File.separator + "login" + File.separator);
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML5");
        resolver.setOrder(templateEngine.getTemplateResolvers().size());
        resolver.setCacheable(false);
        templateEngine.addTemplateResolver(resolver);
    }
}
