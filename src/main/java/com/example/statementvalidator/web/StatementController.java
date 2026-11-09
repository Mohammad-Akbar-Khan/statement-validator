package com.example.statementvalidator.web;

import com.example.statementvalidator.service.StatementPipeline;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatementController {
    private final StatementPipeline pipeline;
    public StatementController(StatementPipeline pipeline) { this.pipeline = pipeline; }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> validate(@RequestPart("file") MultipartFile file,
                                      @RequestParam(required = false) String expectedAccount,
                                      @RequestParam(required = false) String expectedOwner,
                                      @RequestParam(required = false) String expectedPeriod) throws Exception {
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Choose a file first."));
        if (expectedAccount == null || expectedAccount.isBlank() || expectedOwner == null || expectedOwner.isBlank() || expectedPeriod == null || expectedPeriod.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter the account number, account holder name, and statement period."));
        }
        String type = file.getContentType();
        if (type == null || !(type.equalsIgnoreCase("application/pdf") || type.startsWith("image/"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Upload a PDF or image statement."));
        }
        if (file.getSize() > 20L * 1024 * 1024) return ResponseEntity.badRequest().body(Map.of("error", "File must be 20 MB or smaller."));
        return ResponseEntity.ok(pipeline.process(file, expectedAccount, expectedOwner, expectedPeriod));
    }
}
