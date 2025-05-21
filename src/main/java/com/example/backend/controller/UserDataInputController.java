package com.example.backend.controller;

import com.example.backend.dto.ContentRequest;
import com.example.backend.service.UserDataInputService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/userdatainput")
@CrossOrigin(origins = "*")
public class UserDataInputController {

    @Autowired
    private UserDataInputService userdataService;

    @PostMapping("/content")
    public ResponseEntity<?> uploadContent(@RequestBody ContentRequest dto) {
        userdataService.saveContent(dto.getUserdatainputId(), dto.getCaption(), dto.getImageUrl());
        return ResponseEntity.ok("Saved");
    }
}
