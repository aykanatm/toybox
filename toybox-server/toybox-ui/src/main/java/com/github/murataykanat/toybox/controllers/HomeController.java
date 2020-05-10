package com.github.murataykanat.toybox.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping( "/toybox")
    public String loadHome(){
        return "home_view";
    }

    @GetMapping("/toybox/files")
    public String loadFiles(){ return "files_view"; }

    @GetMapping("/toybox/folders")
    public String loadFolders(){ return "folders_view"; }

    @GetMapping("/toybox/recyclebin")
    public String loadRecycleBin(){ return "recycle_bin_view"; }

    @GetMapping("/toybox/jobs")
    public String loadJobs(){ return "jobs_view"; }

    @GetMapping("/toybox/notifications")
    public String loadNotifications(){ return "notifications_view"; }

    @GetMapping("/toybox/shares")
    public String loadShares(){ return "shares_view";}

    @GetMapping("/exit")
    public String loadExit(){ return "logout_view"; }
}
