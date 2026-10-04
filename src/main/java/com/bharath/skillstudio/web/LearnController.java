package com.bharath.skillstudio.web;

import com.bharath.skillstudio.learn.SkillLesson;
import com.bharath.skillstudio.learn.SkillLessonService;
import com.bharath.skillstudio.learn.SkillLessonService.SkillButton;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LearnController {

    private static final MediaType ZIP = MediaType.parseMediaType("application/zip");

    private final SkillLessonService skillLessonService;

    public LearnController(SkillLessonService skillLessonService) {
        this.skillLessonService = skillLessonService;
    }

    @GetMapping(path = "/api/skills.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> skills() {
        List<SkillButton> buttons = skillLessonService.listSkills();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("skills", buttons);
        return out;
    }

    @GetMapping(path = "/api/lesson.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public SkillLesson lesson(@RequestParam String skill,
                              @RequestParam(required = false) String concept,
                              @RequestParam(required = false) Integer page,
                              @RequestParam(required = false) Integer size) {
        return skillLessonService.lesson(skill, concept, page, size);
    }

    @GetMapping(path = "/api/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public SkillLesson generate(@RequestParam String skill) {
        return skillLessonService.generate(skill);
    }

    @GetMapping(path = "/api/project.zip")
    public ResponseEntity<ByteArrayResource> project(@RequestParam String skills,
                                                     @RequestParam(required = false) String concepts) {
        byte[] zip = skillLessonService.projectZip(skills, concepts);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"skill-studio.zip\"")
                .contentType(ZIP)
                .contentLength(zip.length)
                .body(new ByteArrayResource(zip));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage() == null ? "Bad skill" : e.getMessage());
    }
}
