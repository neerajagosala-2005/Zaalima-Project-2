
package com.zaalima.iam_server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ConsentController {

    @GetMapping("/oauth2/consent")
    public String consent(
            @RequestParam("client_id") String clientId,
            @RequestParam("scope") String scope,
            @RequestParam("state") String state,
            Model model) {

        model.addAttribute("clientId", clientId);
        model.addAttribute("scope", scope);
        model.addAttribute("state", state);

        return "consent";
    }
}

