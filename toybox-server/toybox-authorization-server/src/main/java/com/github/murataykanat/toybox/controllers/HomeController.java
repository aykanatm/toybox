package com.github.murataykanat.toybox.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @RequestMapping("/login")
    public String loadHome(){
        return "login_view";
    }
}
