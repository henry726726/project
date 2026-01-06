// src/main/java/com/example/backend/controller/TokenController.java
package com.example.backend.controller;

import com.example.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
public class TokenController {

    private final UserService userService;

    @GetMapping("/remaining")
    public ResponseEntity<Integer> getRemainingTokens(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        int remaining = userService.getRemainingImageTokens(userDetails.getUsername());
        return ResponseEntity.ok(remaining);
    }
}
