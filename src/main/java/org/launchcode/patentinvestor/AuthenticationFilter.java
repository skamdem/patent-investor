package org.launchcode.patentinvestor;

import org.launchcode.patentinvestor.controllers.AuthenticationController;
import org.launchcode.patentinvestor.data.UserRepository;
import org.launchcode.patentinvestor.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kamdem
 */
public class AuthenticationFilter extends HandlerInterceptorAdapter {

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthenticationController authenticationController;

    /**
     * List of URLs that shall proceed without authentication
     */
    private static final List<String> whitelist = Arrays.asList(
            "/login", "/register", "/logout",
            "/webjars/bootstrap/4.4.1-1/css/bootstrap.min.css",
            "/img/stock_icon_128_128.png",
            "/img/stock_market_icon_96x96px.png",
            "/img/showcase.jpg",
            "/styles.css"
    );

    private static boolean isWhitelisted(String path) {
        for (String pathRoot : whitelist) {
            if (path.startsWith(pathRoot)) {
                return true;
            }
        }
        return true;
        //return false;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws IOException {
        // Don't require sign-in for whitelisted pages
        if (isWhitelisted(request.getRequestURI())) {
            // returning true indicates that the request may proceed
            return true;
        }
        HttpSession session = request.getSession();
        User user = authenticationController.getUserFromSession(session);
        // The user is logged in
        if (user != null) {
            return true;
        }
        // The user is NOT logged in
        response.sendRedirect("/login");
        return false;
    }

}
