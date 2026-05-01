package org.example.pim_system;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @GetMapping("/index")
    public String indexPage() {
        return "index";
    }

    @GetMapping("/display")
    public String displayPage() {
        return "display";
    }

    @GetMapping("/employee-work")
    public String employeeWork(Model model, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        model.addAttribute("serverUsername", username);
        return "employee-work";
    }
}



