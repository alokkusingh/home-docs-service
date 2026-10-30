package com.alok.home.service;

import com.alok.home.dto.FileNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class NoteService {

    @Value("${notes.root-directory}")
    private String configuredPath;

    private Path rootDirectory;
    private final List<String> excludeRootDirs = List.of("git", "docs");

    @PostConstruct
    public void init() {
        this.rootDirectory = Paths.get(configuredPath).toAbsolutePath().normalize();
    }

    public List<FileNode> getFileTree() throws IOException {
        FileNode rootNode = buildTree(rootDirectory, "");
        return rootNode.getChildren() != null ? rootNode.getChildren() : new ArrayList<>();
    }

    public ResponseEntity<String> getNoteContent(String path) throws IOException {
        Path filePath = rootDirectory.resolve(path).normalize();

        if (!filePath.startsWith(rootDirectory)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied");
        }

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return ResponseEntity.notFound().build();
        }

        String content = Files.readString(filePath);
        return ResponseEntity.ok(content);
    }

    public ResponseEntity<Resource> getImage(String imageName) {
        try {
            String decodedName = URLDecoder.decode(imageName, StandardCharsets.UTF_8.toString());

            Path targetDirectory = rootDirectory.getParent() != null ? rootDirectory.getParent().getParent() : null;
            if (targetDirectory == null) {
                return ResponseEntity.notFound().build();
            }

            Path imagePath = targetDirectory.resolve(decodedName).normalize();
            if (!imagePath.startsWith(targetDirectory)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
                if (decodedName.endsWith(".jpeg")) {
                    String fallbackName = decodedName.substring(0, decodedName.lastIndexOf(".jpeg")) + ".jpg";
                    Path fallbackPath = targetDirectory.resolve(fallbackName).normalize();
                    if (Files.exists(fallbackPath)) {
                        imagePath = fallbackPath;
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                } else if (decodedName.endsWith(".jpg")) {
                    String fallbackName = decodedName.substring(0, decodedName.lastIndexOf(".jpg")) + ".jpeg";
                    Path fallbackPath = targetDirectory.resolve(fallbackName).normalize();
                    if (Files.exists(fallbackPath)) {
                        imagePath = fallbackPath;
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                } else {
                    return ResponseEntity.notFound().build();
                }
            }

            Resource resource = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

                    if (Files.isDirectory(p) || p.getFileName().toString().endsWith(".md")) {
                        node.addChild(buildTree(p, childRelative));
                    }
                }
            }
        }

        if (node.getChildren() != null) {
            node.getChildren().sort((a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) {
                    return 1;
                } else if (!a.isDirectory() && b.isDirectory()) {
                    return -1;
                } else {
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
        }
        return node;
    }

}
