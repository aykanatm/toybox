package com.github.murataykanat.toybox.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.configuration.GenericFieldValue;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RefreshScope
@RestController
public class ConfigController {
    private static final Log _logger = LogFactory.getLog(ConfigController.class);

    @Value("${configServerFieldRequestUrl}")
    private String configServerFieldRequestUrl;

    @Autowired
    private UsersRepository usersRepository;

    private RestTemplate restTemplate = new RestTemplateBuilder().build();

    @RequestMapping(value = "/configuration", method = RequestMethod.GET)
    public ResponseEntity<GenericFieldValue> getConfiguration(Authentication authentication, @RequestParam("field") String fieldName){
        _logger.debug("getConfiguration() >> [" + fieldName + "]");
        _logger.debug("Configuration Server Field Request URL: " + this.configServerFieldRequestUrl);

        try{
            GenericFieldValue configurationFieldValue = new GenericFieldValue();

            List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
            if(!usersByUsername.isEmpty()){
                if(usersByUsername.size() == 1){
                    ResponseEntity<String> response = restTemplate.getForEntity(this.configServerFieldRequestUrl, String.class);

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(response.getBody());
                    String fieldValue = root.get("propertySources").get(0).get("source").get(fieldName).textValue();

                    if(StringUtils.isNotBlank(fieldValue)){
                        configurationFieldValue.setValue(fieldValue);
                        configurationFieldValue.setMessage("Field value retrieved successfully!");

                        _logger.debug("<< getConfiguration() [" + configurationFieldValue.getValue() + "]");
                        return new ResponseEntity<>(configurationFieldValue, HttpStatus.OK);
                    }
                    else{
                        configurationFieldValue.setMessage("Field value is blank!");

                        _logger.debug("<< getConfiguration()");
                        return new ResponseEntity<>(configurationFieldValue, HttpStatus.BAD_REQUEST);
                    }
                }
                else{
                    String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                    _logger.debug(errorMessage);

                    configurationFieldValue.setMessage(errorMessage);

                    _logger.debug("<< retrieveJobs()");
                    return new ResponseEntity<>(configurationFieldValue, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                _logger.debug(errorMessage);

                configurationFieldValue.setMessage(errorMessage);

                _logger.debug("<< retrieveJobs()");
                return new ResponseEntity<>(configurationFieldValue, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while getting the configuration. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);

            GenericFieldValue configurationFieldValue = new GenericFieldValue();
            configurationFieldValue.setMessage(errorMessage);

            _logger.debug("<< getConfiguration()");
            return new ResponseEntity<>(configurationFieldValue, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
