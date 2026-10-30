package com.alok.home.controller;

import com.alok.home.dto.FileNode;
import com.alok.home.service.NoteService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/tree")
    public List<FileNode> getFileTree() throws IOException {
        return noteService.getFileTree();
    }

    @GetMapping("/content")
    public ResponseEntity<String> getNoteContent(@RequestParam String path) throws IOException {
        return noteService.getNoteContent(path);
    }

    @GetMapping("/images/{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {
        return noteService.getImage(imageName);
    }
}

