package com.bharath.skillstudio.web;

import com.bharath.skillstudio.learn.SkillLessonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final SkillLessonService skillLessonService;

    public HomeController(SkillLessonService skillLessonService) {
        this.skillLessonService = skillLessonService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("learnSkills", skillLessonService.listSkills());
        return "index";
    }
}
