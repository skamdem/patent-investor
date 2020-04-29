package org.launchcode.patentinvestor.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kamdem
 */
@Controller
public class HomeController {
    @RequestMapping(value = "")
    public String index(Model model) {
        Map<String, String> actionChoices = new LinkedHashMap<>();
        actionChoices.put("stocks/search", "Search stock");
        actionChoices.put("stocks", "All Stocks");
        actionChoices.put("tags", "All investment fields");
        actionChoices.put("stocks/portfolio", "Portfolio");

        model.addAttribute("actions", actionChoices);
        model.addAttribute("title", "Home");
        return "index";
    }
}
