package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuthenticationUtils {
    private static final Log _logger = LogFactory.getLog(AuthenticationUtils.class);

    @Autowired
    private UsersRepository usersRepository;

    @LogEntryExitExecutionTime
    public User getUser(Authentication authentication){
        String errorMessage;
        List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
                return usersByUsername.get(0);
            }
            else{
                errorMessage = "Username '" + authentication.getName() + "' is not unique!";
            }
        }
        else{
            errorMessage = "No users with username '" + authentication.getName() + " is found!";
        }
        _logger.error(errorMessage);
        return null;
    }

    @LogEntryExitExecutionTime
    public User getUser(String username) throws Exception {
        List<User> usersByUsername = usersRepository.findUsersByUsername(username);
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
                return usersByUsername.get(0);
            }
            else{
                throw new IllegalArgumentException("There are multiple instances of the username '" + username + "'!");
            }
        }
        else{
            throw new IllegalArgumentException("Username '" + username + "' cannot be found in the system!");
        }
    }

    @LogEntryExitExecutionTime
    public User getUser(int userId) throws Exception {
        List<User> toUsersByUserId = usersRepository.findUsersByUserId(userId);
        if(!toUsersByUserId.isEmpty()){
            if(toUsersByUserId.size() == 1){
                return toUsersByUserId.get(0);
            }
            else{
                throw new IllegalArgumentException("There are multiple instances of the ID '" + userId + "'!");
            }
        }
        else{
            throw new IllegalArgumentException("User ID '" + userId + "' cannot be found in the system!");
        }
    }

    @LogEntryExitExecutionTime
    public boolean isAdminUser(Authentication authentication){
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<? extends GrantedAuthority> roleAdmin = authorities.stream().filter(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")).collect(Collectors.toList());
        if(!roleAdmin.isEmpty()){
            return true;
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public boolean isSessionValid(Authentication authentication){
        String errorMessage;
        List<User> usersByUsername = usersRepository.findUsersByUsername(authentication.getName());
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
                return true;
            }
            else{
                errorMessage = "Username '" + authentication.getName() + "' is not unique!";
            }
        }
        else{
            errorMessage = "No users with username '" + authentication.getName() + " is found!";
        }

        _logger.error(errorMessage);
        return false;
    }

    @LogEntryExitExecutionTime
    public HttpHeaders getHeaders(HttpSession session) {
        HttpHeaders headers = new HttpHeaders();

        _logger.debug("Session ID: " + session.getId());
        CsrfToken token = (CsrfToken) session.getAttribute("org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
        if(token != null){
            _logger.debug("CSRF Token: " + token.getToken());
            headers.set("Cookie", "SESSION=" + session.getId() + "; XSRF-TOKEN=" + token.getToken());
            headers.set("X-XSRF-TOKEN", token.getToken());

            return headers;
        }
        else{
            throw new IllegalArgumentException("CSRF token is null!");
        }
    }
}