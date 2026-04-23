package com.example.demo.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("${app.admin.base-path}")
public class AdminDashboardController {

    @GetMapping({"", "/"})
    public String dashboard() {
        return "admin/dashboard";
    }
}
