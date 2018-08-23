package com.github.murataykanat.toybox.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class ThmeleafExtension {
    @Autowired
    private SpringTemplateEngine templateEngine;

    @PostConstruct
    public void extension(){
        // This code makes spring boot to load the UI from an external source (disk) rather than the resources
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix(System.getenv("TOYBOX_HOME") + File.separator + "ui" + File.separator + "home" + File.separator);
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML5");
        resolver.setOrder(templateEngine.getTemplateResolvers().size());
        resolver.setCacheable(false);
        templateEngine.addTemplateResolver(resolver);
    }
}
