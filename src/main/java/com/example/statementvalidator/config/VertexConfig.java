package com.example.statementvalidator.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexConfig {
    @Bean
    Client vertexClient(@Value("${vertex.project-id}") String project,
                        @Value("${vertex.location}") String location) {
        if (project == null || project.isBlank()) {
            throw new IllegalStateException("Set GOOGLE_CLOUD_PROJECT before starting the app.");
        }
        return Client.builder().project(project).location(location).enterprise(true).build();
    }
}
