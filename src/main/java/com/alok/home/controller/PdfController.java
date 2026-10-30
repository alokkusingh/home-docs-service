package com.alok.home.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/pdfs")
@CrossOrigin(origins = "http://localhost:3000")
public class PdfController {

    // Configure the absolute path to your root PDF directory
    private final Path pdfRootDirectory = Paths.get("/Users/aloksingh/git/home-docs").toAbsolutePath().normalize();

    @GetMapping("/tree")
    public List<FileNode> getPdfTree() throws IOException {
        // Reuses the identical recursive FileNode tree logic we built for your notes
        FileNode rootNode = buildTree(pdfRootDirectory, "");
        return rootNode.getChildren() != null ? rootNode.getChildren() : new ArrayList<>();
    }

    private FileNode buildTree(Path currentPath, String relativePath) throws IOException {
        String name = currentPath.getFileName() == null ? "" : currentPath.getFileName().toString();
        boolean isDir = Files.isDirectory(currentPath);

        FileNode node = new FileNode(name, relativePath, isDir);

        if (isDir) {
            try (Stream<Path> stream = Files.list(currentPath)) {
                List<Path> paths = stream.collect(Collectors.toList());
                for (Path p : paths) {
                    String childRelative = relativePath.isEmpty() ? p.getFileName().toString() : relativePath + "/" + p.getFileName().toString();

                    // Filter: Only include sub-directories or actual PDF files or actual JPG files or actual PNG files. This ensures we don't include other file types.
                    if (Files.isDirectory(p) || p.getFileName().toString().toLowerCase().endsWith(".pdf") || p.getFileName().toString().toLowerCase().endsWith(".jpg") || p.getFileName().toString().toLowerCase().endsWith(".png")) {
                        node.addChild(buildTree(p, childRelative));
                    }
                }
            }
        }

        // sort the files and directories alphabetically, with files first and then directories
        if (node.getChildren() != null) {
            node.getChildren().sort((a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) {
                    return 1; // directories after files
                } else if (!a.isDirectory() && b.isDirectory()) {
                    return -1; // files before directories
                } else {
                    return a.getName().compareToIgnoreCase(b.getName()); // alphabetical order
                }
            });
        }

        return node;
    }

    @GetMapping("/stream")
    public ResponseEntity<Resource> streamPdf(@RequestParam String path) throws IOException {
        Path filePath = pdfRootDirectory.resolve(path).normalize();

        // Security check to completely block directory traversal attacks
        if (!filePath.startsWith(pdfRootDirectory)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        // If the file is PDF, we can set the content type to application/pdf. For other file types, you might want to handle them differently or set a generic content type.
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream"; // Default binary type if unknown
        }
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(contentType))
                // inline tells the browser to display it inside our frame instead of downloading it
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName().toString() + "\"")
                .body(resource);
    }
}

