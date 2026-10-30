package com.alok.home.controller;

import com.alok.home.dto.FileNode;
import com.alok.home.service.NotesService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/notes")
public class NotesController {

    private final NotesService notesService;

    public NotesController(NotesService notesService) {
        this.notesService = notesService;
    }

    @GetMapping("/tree")
    public List<FileNode> getFileTree() throws IOException {
        return notesService.getFileTree();
    }

    @GetMapping("/content")
    public ResponseEntity<String> getNoteContent(@RequestParam String path) throws IOException {
        return notesService.getNoteContent(path);
    }

    @GetMapping("/images/{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {
        return notesService.getImage(imageName);
    }
}

