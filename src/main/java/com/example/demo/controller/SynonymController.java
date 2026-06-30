package com.example.demo.controller;

import com.example.demo.constants.ApiConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/synonyms")
public class SynonymController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of("status", "ready", ApiConstants.KEY_MESSAGE, "Synonym API is available"));
    }
}
