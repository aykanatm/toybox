package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.Role;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.schema.user.UserResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

@RestController
public class SecurityController {
    private static final Log _logger = LogFactory.getLog(SecurityController.class);

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public ResponseEntity<UserResponse> getPrincipal(Principal principal){
        _logger.debug("getPrincipal() >>");

        try{
            OAuth2Authentication oAuth2Authentication = (OAuth2Authentication) principal;
            LinkedHashMap<String, Object> details = (LinkedHashMap<String, Object>) oAuth2Authentication.getUserAuthentication().getDetails();

            LinkedHashMap<String, Object> actualPrincipal = ( LinkedHashMap<String, Object>) details.get("principal");

            User user = new User();
            user.setId((Integer) actualPrincipal.get("id"));
            user.setUsername((String) actualPrincipal.get("username"));
            user.setName((String) actualPrincipal.get("name"));
            user.setLastname((String) actualPrincipal.get("lastname"));
            user.setEmail((String) actualPrincipal.get("email"));
            user.setEnabled(((boolean) actualPrincipal.get("enabled")));
            user.setAvatarPath((String) actualPrincipal.get("avatarPath"));
            user.setAccountNonExpired(((boolean) actualPrincipal.get("accountNonExpired")));
            user.setCredentialsNonExpired(((boolean) actualPrincipal.get("credentialsNonExpired")));
            user.setAccountNonLocked(((boolean) actualPrincipal.get("accountNonLocked")));
            user.setRoles(new HashSet<>((ArrayList<Role>) actualPrincipal.get("roles")));

            UserResponse userResponse = new UserResponse();
            userResponse.setUser(user);
            userResponse.setMessage("User is retrieved successfully.");

            _logger.debug("<< getPrincipal()");
            return new ResponseEntity<>(userResponse, HttpStatus.OK);
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the user. " + e.getLocalizedMessage();
            _logger.debug(errorMessage, e);

            UserResponse userResponse = new UserResponse();
            userResponse.setMessage(errorMessage);

            _logger.debug("<< getPrincipal()");
            return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
