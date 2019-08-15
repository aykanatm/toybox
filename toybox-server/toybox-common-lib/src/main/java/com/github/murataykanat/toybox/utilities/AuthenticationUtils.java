package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.User;
import com.github.murataykanat.toybox.repositories.UsersRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;

import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AuthenticationUtils {
    private static final Log _logger = LogFactory.getLog(AuthenticationUtils.class);

    private static AuthenticationUtils authenticationUtils;

    private AuthenticationUtils(){}

    public static AuthenticationUtils getInstance(){
        if(authenticationUtils != null){
            return authenticationUtils;
        }

        authenticationUtils = new AuthenticationUtils();
        return authenticationUtils;
    }

    @LogEntryExitExecutionTime
    public User getUser(UsersRepository usersRepository, Authentication authentication){
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
    public User getUser(UsersRepository usersRepository, String username) throws Exception {
        List<User> usersByUsername = usersRepository.findUsersByUsername(username);
        if(!usersByUsername.isEmpty()){
            if(usersByUsername.size() == 1){
                return usersByUsername.get(0);
            }
            else{
                throw new Exception("There are multiple instances of the username '" + username + "'!");
            }
        }
        else{
            throw new Exception("Username '" + username + "' cannot be found in the system!");
        }
    }

    @LogEntryExitExecutionTime
    public boolean isAdminUser(Authentication authentication){
        _logger.debug("isAdminUser() >>");
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        List<? extends GrantedAuthority> roleAdmin = authorities.stream().filter(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")).collect(Collectors.toList());
        if(!roleAdmin.isEmpty()){
            _logger.debug("<< isAdminUser() [false]");
            return true;
        }

        _logger.debug("<< isAdminUser() [false]");
        return false;
    }

    @LogEntryExitExecutionTime
    public boolean isSessionValid(UsersRepository usersRepository, Authentication authentication){
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
