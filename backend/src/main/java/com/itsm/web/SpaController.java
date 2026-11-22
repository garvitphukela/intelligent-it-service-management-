package com.itsm.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the React single-page app for client-side routes when bundled into the jar. */
@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/register", "/app", "/app/**"})
    public String index() {
        return "forward:/index.html";
    }
}
