package com.alok.home.controller;

import com.alok.home.dto.FileNode;
import com.alok.home.service.PdfService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/pdfs")
@CrossOrigin(origins = "http://localhost:3000")
public class PdfController {

    private final PdfService pdfService;

    public PdfController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/tree")
    public List<FileNode> getPdfTree() throws IOException {
        return pdfService.getPdfTree();
    }

    @GetMapping("/stream")
    public ResponseEntity<Resource> streamPdf(@RequestParam String path) throws IOException {
        return pdfService.streamDocument(path);
    }
}

