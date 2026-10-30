package com.alok.home.dto;

import java.util.ArrayList;
import java.util.List;

public class FileNode {
    private String name;
    private String relativePath;
    private boolean directory;
    private List<FileNode> children;

    public FileNode() {
    }

    public FileNode(String name, String relativePath, boolean directory) {
        this.name = name;
        this.relativePath = relativePath;
        this.directory = directory;
        this.children = directory ? new ArrayList<>() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileNode> children) {
        this.children = children;
    }

    public void addChild(FileNode child) {
        if (children != null) {
            children.add(child);
        }
    }
}
