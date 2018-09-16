package com.github.murataykanat.toybox.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.murataykanat.toybox.models.ConfigurationFieldValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RefreshScope
@RestController
public class ConfigController {
    private static final Log _logger = LogFactory.getLog(ConfigController.class);

    @Value("${configServerFieldRequestUrl}")
    private String configServerFieldRequestUrl;

    private RestTemplate restTemplate = new RestTemplateBuilder().build();

    @RequestMapping(value = "/configuration", method = RequestMethod.GET)
    public @ResponseBody ConfigurationFieldValue getConfiguration(@RequestParam("field") String fieldName){
        _logger.debug("getConfiguration() >>");
        _logger.debug("Configuration Server Field Request URL: " + configServerFieldRequestUrl);

        String fieldValue = null;

        try{
            ResponseEntity<String> response = restTemplate.getForEntity(configServerFieldRequestUrl, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            fieldValue = root.get("propertySources").get(0).get("source").get(fieldName).textValue();
            ConfigurationFieldValue configurationFieldValue = new ConfigurationFieldValue(fieldValue);

            _logger.debug("<< getConfiguration() [" + configurationFieldValue.getValue() + "]");
            return configurationFieldValue;
        }
        catch (Exception e){
            String errorMessage = "An error occured while getting the configuration. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
        }

        _logger.debug("<< getConfiguration() [" + null + "]");
        return null;
    }
}
