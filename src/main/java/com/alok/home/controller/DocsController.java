package com.alok.home.controller;

import com.alok.home.dto.FileNode;
import com.alok.home.service.DocsService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/pdfs", "/docs"})
@CrossOrigin(origins = "http://localhost:3000")
public class DocsController {

    private final DocsService docsService;

    public DocsController(DocsService docsService) {
        this.docsService = docsService;
    }

    @GetMapping("/tree")
    public List<FileNode> getDocsTree() throws IOException {
        return docsService.getDocsTree();
    }

    @GetMapping("/stream")
    public ResponseEntity<Resource> streamDocs(@RequestParam String path) throws IOException {
        return docsService.streamDocument(path);
    }
}

