package com.alok.home.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/notes")
public class NoteController {

    // Path to the folder where your .md files are stored
    // 1. Inject the path configuration value dynamically
    @Value("${notes.root-directory}")
    private String configuredPath;

    private Path rootDirectory;

    private final List<String> EXCLUDE_ROOT_DIRS = List.of("git", "docs");

    // 2. Initialize the path safely on startup
    @PostConstruct
    public void init() {
        this.rootDirectory = Paths.get(configuredPath).toAbsolutePath().normalize();
        System.out.println("INFO: Notes System initialized at root path: " + this.rootDirectory);
    }

    @GetMapping("/tree")
    public List<FileNode> getFileTree() throws IOException {
        // 1. Build the full tree starting at the root
        FileNode rootNode = buildTree(rootDirectory, "");

        // 2. Return the children of the root to skip the "Root" folder level in your UI
        return rootNode.getChildren() != null ? rootNode.getChildren() : new ArrayList<>();
    }

    private FileNode buildTree(Path currentPath, String relativePath) throws IOException {
        String name = currentPath.getFileName() == null ? "" : currentPath.getFileName().toString();
        // Check if the current node is a directory
        boolean isDir = Files.isDirectory(currentPath);

        FileNode node = new FileNode(name, relativePath, isDir);

        if (isDir) {
            try (Stream<Path> stream = Files.list(currentPath)) {
                List<Path> paths = stream.toList();
                for (Path p : paths) {
                    // Exclude certain directories at the root level, but allow them if they are nested deeper in the structure
                    if (EXCLUDE_ROOT_DIRS.contains(p.getFileName().toString()) && relativePath.isEmpty()) {
                        continue; // Skip excluded directories
                    }

                    String childRelative = relativePath.isEmpty() ? p.getFileName().toString() : relativePath + "/" + p.getFileName().toString();

                    if (Files.isDirectory(p) || p.getFileName().toString().endsWith(".md")) {
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

    @GetMapping("/content")
    public ResponseEntity<String> getNoteContent(@RequestParam String path) throws IOException {
        // Resolve path safely against root to block directory traversal attacks
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

    @GetMapping("/images/{imageName}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) {
        try {
            String decodedName = URLDecoder.decode(imageName, StandardCharsets.UTF_8.toString());

            // 1. Navigate two levels up (../../) from your notes root directory
            Path targetDirectory = rootDirectory.getParent() != null ? rootDirectory.getParent().getParent() : null;
            if (targetDirectory == null) {
                System.out.println("DEBUG: Grandparent directory could not be resolved.");
                return ResponseEntity.notFound().build();
            }

            // 2. Resolve the absolute path to the file
            Path imagePath = targetDirectory.resolve(decodedName).normalize();

            // 3. Security Check: Ensure the requested path stays within the target directory boundary
            if (!imagePath.startsWith(targetDirectory)) {
                System.out.println("DEBUG: Blocked due to security directory traversal guard.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 4. Fallback Handler: Check for extension mismatches (.jpeg vs .jpg) if not found initially
            if (!Files.exists(imagePath) || Files.isDirectory(imagePath)) {
                if (decodedName.endsWith(".jpeg")) {
                    String fallbackName = decodedName.substring(0, decodedName.lastIndexOf(".jpeg")) + ".jpg";
                    Path fallbackPath = targetDirectory.resolve(fallbackName).normalize();
                    if (Files.exists(fallbackPath)) {
                        imagePath = fallbackPath;
                    } else {
                        System.out.println("DEBUG: File not found at: " + imagePath.toAbsolutePath());
                        return ResponseEntity.notFound().build();
                    }
                } else if (decodedName.endsWith(".jpg")) {
                    String fallbackName = decodedName.substring(0, decodedName.lastIndexOf(".jpg")) + ".jpeg";
                    Path fallbackPath = targetDirectory.resolve(fallbackName).normalize();
                    if (Files.exists(fallbackPath)) {
                        imagePath = fallbackPath;
                    } else {
                        System.out.println("DEBUG: File not found at: " + imagePath.toAbsolutePath());
                        return ResponseEntity.notFound().build();
                    }
                } else {
                    System.out.println("DEBUG: File not found at: " + imagePath.toAbsolutePath());
                    return ResponseEntity.notFound().build();
                }
            }

            // 5. Build and return the image stream resource
            Resource resource = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "image/jpeg"; // Safe fallback default for image assets
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

class FileNode {
    private String name;
    private String relativePath;
    private boolean isDirectory;
    private List<FileNode> children;

    // Constructors, Getters, and Setters
    public FileNode(String name, String relativePath, boolean isDirectory) {
        this.name = name;
        this.relativePath = relativePath;
        this.isDirectory = isDirectory;
        this.children = isDirectory ? new ArrayList<>() : null;
    }
    public String getName() { return name; }
    public String getRelativePath() { return relativePath; }
    public boolean isDirectory() { return isDirectory; }
    public List<FileNode> getChildren() { return children; }
    public void addChild(FileNode child) { if (children != null) children.add(child); }
}

