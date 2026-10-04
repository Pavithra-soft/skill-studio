package com.bharath.skillstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SkillStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillStudioApplication.class, args);
    }
}
