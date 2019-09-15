package com.github.murataykanat.toybox.controllers;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.dbo.UserGroup;
import com.github.murataykanat.toybox.repositories.UserGroupsRepository;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import com.github.murataykanat.toybox.schema.user.RetrieveUsersResponse;
import com.github.murataykanat.toybox.schema.user.UserResponse;
import com.github.murataykanat.toybox.schema.usergroup.RetrieveUserGroupsResponse;
import com.github.murataykanat.toybox.utilities.AuthenticationUtils;
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
public class UserController {
    private static final Log _logger = LogFactory.getLog(UserController.class);

    @Autowired
    private AuthenticationUtils authenticationUtils;

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private UserGroupsRepository userGroupsRepository;

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/users/me", method = RequestMethod.GET)
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication){
        UserResponse userResponse = new UserResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                User user = authenticationUtils.getUser(authentication);
                if(user != null){
                    userResponse.setUser(user);
                    userResponse.setMessage("User is retrieved successfully.");

                    return new ResponseEntity<>(userResponse, HttpStatus.OK);
                }
                else{
                    throw new IllegalArgumentException("User is null!");
                }
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                userResponse.setMessage(errorMessage);

                return new ResponseEntity<>(userResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving the current user. " + e.getLocalizedMessage();
            _logger.debug(errorMessage, e);

            userResponse.setMessage(errorMessage);

            return new ResponseEntity<>(userResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public ResponseEntity<RetrieveUsersResponse> retrieveUsers(Authentication authentication){
        _logger.debug("retrieveUsers() >>");
        RetrieveUsersResponse retrieveUsersResponse = new RetrieveUsersResponse();

        try {
            if(authenticationUtils.isSessionValid(authentication)){
                List<User> users = usersRepository.findAll();

                retrieveUsersResponse.setUsers(users);
                retrieveUsersResponse.setMessage("Users retrieved successfully!");

                return new ResponseEntity<>(retrieveUsersResponse, HttpStatus.OK);
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveUsersResponse.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveUsersResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving users. " + e.getLocalizedMessage();
            _logger.debug(errorMessage, e);

            retrieveUsersResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveUsersResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @LogEntryExitExecutionTime
    @RequestMapping(value = "/usergroups", method = RequestMethod.GET)
    public ResponseEntity<RetrieveUserGroupsResponse> retrieveUserGroups(Authentication authentication){
        RetrieveUserGroupsResponse retrieveUserGroupsResponse = new RetrieveUserGroupsResponse();

        try{
            if(authenticationUtils.isSessionValid(authentication)){
                List<UserGroup> userGroups = userGroupsRepository.findAll();

                retrieveUserGroupsResponse.setUsergroups(userGroups);
                retrieveUserGroupsResponse.setMessage("User groups retrieved successfully!");

                return new ResponseEntity<>(retrieveUserGroupsResponse, HttpStatus.OK);
            }
            else{
                String errorMessage = "Session for the username '" + authentication.getName() + "' is not valid!";
                _logger.error(errorMessage);

                retrieveUserGroupsResponse.setMessage(errorMessage);

                return new ResponseEntity<>(retrieveUserGroupsResponse, HttpStatus.UNAUTHORIZED);
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while retrieving user groups. " + e.getLocalizedMessage();
            _logger.debug(errorMessage, e);

            retrieveUserGroupsResponse.setMessage(errorMessage);

            return new ResponseEntity<>(retrieveUserGroupsResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}