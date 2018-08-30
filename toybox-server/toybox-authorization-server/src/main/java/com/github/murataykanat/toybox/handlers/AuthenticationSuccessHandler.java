package com.github.murataykanat.toybox.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Value("${toyboxHomePage}")
    private String toyboxHomePage;

    private static final Log _logger = LogFactory.getLog(AuthenticationSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // TODO:
        // Redirect to previous url if it is from localhost:8083 (toybox ui service)
        // If not redirect to default url like below
        setDefaultTargetUrl(toyboxHomePage);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
