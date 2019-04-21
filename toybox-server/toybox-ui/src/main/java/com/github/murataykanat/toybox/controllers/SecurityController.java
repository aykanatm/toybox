package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.user.UserResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SecurityController {
    private static final Log _logger = LogFactory.getLog(SecurityController.class);

    @Autowired
    private UsersRepository usersRepository;

    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication){
        _logger.debug("getCurrentUser() >>");
        try{
            UserResponse userResponse = new UserResponse();

            List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
            if(!usersByUsername.isEmpty()){
                if(usersByUsername.size() == 1){
                    User user = usersByUsername.get(0);

                    userResponse.setUser(user);
                    userResponse.setMessage("User is retrieved successfully.");

                    _logger.debug("<< getCurrentUser()");
                    return new ResponseEntity<>(userResponse, HttpStatus.OK);
                }
                else{
                    String errorMessage = "Username '" + authentication.getName() + "' is not unique!";
                    _logger.debug(errorMessage);

                    userResponse.setMessage(errorMessage);

                    _logger.debug("<< getCurrentUser()");
                    return new ResponseEntity<>(userResponse, HttpStatus.UNAUTHORIZED);
                }
            }
            else{
                String errorMessage = "No users with username '" + authentication.getName() + " is found!";
                _logger.debug(errorMessage);

                userResponse.setMessage(errorMessage);

                _logger.debug("<< getCurrentUser()");
                return new ResponseEntity<>(userResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the current user. " + e.getLocalizedMessage();
            _logger.debug(errorMessage, e);

            UserResponse userResponse = new UserResponse();
            userResponse.setMessage(errorMessage);

            _logger.debug("<< getCurrentUser()");
            return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
