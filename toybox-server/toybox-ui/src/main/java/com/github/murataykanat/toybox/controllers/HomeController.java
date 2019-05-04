package com.github.murataykanat.toybox.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @RequestMapping("/toybox")
    public String loadHome(){
        return "home_view";
    }

    @RequestMapping("/toybox/files")
    public String loadFiles(){ return "files_view"; }

    @RequestMapping("/toybox/folders")
    public String loadFolders(){ return "folders_view"; }

    @RequestMapping("/toybox/jobs")
    public String loadJobs(){ return "jobs_view"; }

    @RequestMapping("/toybox/notifications")
    public String loadNotifications(){ return "notifications_view"; }

    @RequestMapping("/exit")
    public String loadExit(){ return "logout_view"; }
}
