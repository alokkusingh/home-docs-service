package com.alok.home.service;

import com.alok.home.dto.FileNode;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class PdfService {

    private final Path pdfRootDirectory = Paths.get("/Users/aloksingh/git/home-docs").toAbsolutePath().normalize();
    private final List<String> excludeRootDirs = List.of(".git", "temp");

    public List<FileNode> getPdfTree() throws IOException {
        FileNode rootNode = buildTree(pdfRootDirectory, "");
        return rootNode.getChildren() != null ? rootNode.getChildren() : new ArrayList<>();
    }

    public ResponseEntity<Resource> streamDocument(String path) throws IOException {
        Path filePath = pdfRootDirectory.resolve(path).normalize();

        if (!filePath.startsWith(pdfRootDirectory)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return ResponseEntity.notFound().build();
        }

        if (!isSupportedDocument(filePath)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String contentType = detectContentType(filePath);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                .body(resource);
    }

    private FileNode buildTree(Path currentPath, String relativePath) throws IOException {
        String name = currentPath.getFileName() == null ? "" : currentPath.getFileName().toString();
        boolean isDir = Files.isDirectory(currentPath);

        FileNode node = new FileNode(name, relativePath, isDir);

        if (isDir) {
            try (Stream<Path> stream = Files.list(currentPath)) {
                List<Path> paths = stream.toList();
                for (Path p : paths) {
                    if (excludeRootDirs.contains(p.getFileName().toString()) && relativePath.isEmpty()) {
                        continue;
                    }

                    String childRelative = relativePath.isEmpty()
                            ? p.getFileName().toString()
                            : relativePath + "/" + p.getFileName().toString();

                    if (Files.isDirectory(p) || isSupportedDocument(p)) {
                        node.addChild(buildTree(p, childRelative));
                    }
                }
            }
        }

        if (node.getChildren() != null) {
            node.getChildren().sort((a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) {
                    return 1;
                }
                if (!a.isDirectory() && b.isDirectory()) {
                    return -1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });
        }

        return node;
    }

    private boolean isSupportedDocument(Path filePath) {
        if (Files.isDirectory(filePath)) {
            return true;
        }

        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".pdf")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png");
    }

    private String detectContentType(Path filePath) {
        String contentType = null;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException ignored) {
            // Ignore and fall through to default logic below.
        }

        if (contentType != null) {
            return contentType;
        }

        String fileName = filePath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
