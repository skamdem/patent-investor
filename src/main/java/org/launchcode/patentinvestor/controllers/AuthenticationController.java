package org.launchcode.patentinvestor.controllers;

import org.launchcode.patentinvestor.data.TagRepository;
import org.launchcode.patentinvestor.data.UserRepository;
import org.launchcode.patentinvestor.models.Tag;
import org.launchcode.patentinvestor.models.User;
import org.launchcode.patentinvestor.models.dto.LoginFormDTO;
import org.launchcode.patentinvestor.models.dto.RegisterFormDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Optional;

/**
 * Created by kamdem
 * For login/logout
 */
@Controller
public class AuthenticationController {

    //General messages
    public final String INFO_MESSAGE_KEY = "message";

    private static final String userSessionKey = "user";

    @Autowired
    UserRepository userRepository;

    @Autowired
    private TagRepository tagRepository;

    /**
     * @param session
     * @return
     */
    public User getUserFromSession(HttpSession session) {
        Integer userId = (Integer) session.getAttribute(userSessionKey);
        if (userId == null) {
            return null;
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return null;
        }
        return user.get();
    }

    /**
     * @param session
     * @param user
     */
    private static void setUserInSession(
            HttpSession session,
            User user) {
        session.setAttribute(userSessionKey, user.getId());
    }

    /**
     * @param model
     * @return
     */
    @GetMapping("/register")
    public String displayRegistrationForm(Model model) {
        model.addAttribute(new RegisterFormDTO());
        model.addAttribute("title", "Register");
        return "register";
    }

    /**
     * @param registerFormDTO
     * @param errors
     * @param request
     * @param model
     * @return
     */
    @PostMapping("/register")
    public String processRegistrationForm(
            @ModelAttribute @Valid RegisterFormDTO registerFormDTO,
            Errors errors,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("title", "Register");
            return "register";
        }
        User existingUser = userRepository.findByUsername(registerFormDTO.getUsername());
        if (existingUser != null) {
            errors.rejectValue("username", "username.alreadyexists", "A user with that username already exists");
            model.addAttribute("title", "Register");
            return "register";
        }

        String password = registerFormDTO.getPassword();
        String verifyPassword = registerFormDTO.getVerifyPassword();
        if (!password.equals(verifyPassword)) {
            errors.rejectValue("password", "passwords.mismatch", "Passwords do not match");
            model.addAttribute("title", "Register");
            return "register";
        }

        User newUser = new User(registerFormDTO.getUsername(), registerFormDTO.getPassword());
        userRepository.save(newUser);

        //----START Create default contents of an account here---
        //by default set up 5 tags in the portfolio
        Tag t1 = new Tag("Biotech", "Biotechnologies and life sciences", newUser.getPortfolio());
        Tag t2 = new Tag("Nanotech", "Nanotechnologies", newUser.getPortfolio());
        Tag t3 = new Tag("AI", "Artificial Intelligence", newUser.getPortfolio());
        Tag t4 = new Tag("Fintech", "Finance and related technologies", newUser.getPortfolio());
        Tag t5 = new Tag("IoT", "Internet of things", newUser.getPortfolio());
        tagRepository.save(t1);
        tagRepository.save(t2);
        tagRepository.save(t3);
        tagRepository.save(t4);
        tagRepository.save(t5);
        redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "success|Welcome, " + newUser.getUsername() + ". You are now logged in.");
        //----END Create default contents of an account here---

        setUserInSession(request.getSession(), newUser);
        return "redirect:";
    }

    /**
     * @param model
     * @return
     */
    @GetMapping("/login")
    public String displayLoginForm(
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        User loggedInUser = this.getUserFromSession(request.getSession());
        if (loggedInUser != null) {//user is logged in
            redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY,
                    "info|You are already logged in as "+ loggedInUser.getUsername() +"!");
            return "redirect:/";
        }

        model.addAttribute(new LoginFormDTO());
        model.addAttribute("title", "Log In");
        return "login";
    }

    /**
     * @param loginFormDTO
     * @param errors
     * @param request
     * @param model
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/login")
    public String processLoginForm(
            @ModelAttribute @Valid LoginFormDTO loginFormDTO,
            Errors errors,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("title", "Log In");
            return "login";
        }
        User theUser = userRepository.findByUsername(loginFormDTO.getUsername());
        if (theUser == null) {
            errors.rejectValue("username", "user.invalid", "The given username does not exist");
            model.addAttribute("title", "Log In");
            return "login";
        }
        String password = loginFormDTO.getPassword();
        if (!theUser.isMatchingPassword(password)) {
            errors.rejectValue("password", "password.invalid", "Invalid password");
            model.addAttribute("title", "Log In");
            return "login";
        }
        redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "success|Welcome, " + loginFormDTO.getUsername() + ". You are now logged in.");
        setUserInSession(request.getSession(), theUser);
        return "redirect:";
    }

    /**
     * @param request
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/logout")
    public String logout(
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        request.getSession().invalidate();
        redirectAttributes.addFlashAttribute(INFO_MESSAGE_KEY, "info|You have logged out");
        return "redirect:/login";
    }

}
