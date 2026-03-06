package com.exception.ccpp.FileManagement;

import com.exception.ccpp.CustomExceptions.NotDirException;
import com.exception.ccpp.DockerHandler;
import com.exception.ccpp.GUI.FileExplorer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static com.exception.ccpp.Gang.SlaveManager.romanArmy;
import static com.exception.ccpp.Gang.SlaveManager.slaveWorkers;

public class FileManager {
    public static final Set<String> IGNORED_FOLDERS = Set.of(".git", ".svn", ".vscode", "target", "out", "bin", "vendor", "System32", "Windows");
    public static final Set<String> ALL_ALLOWED_EXTENSIONS = Set.of(".c", ".cpp", ".h", ".hpp",".java",".py");
    public static final Set<String> JAVA_ALLOWED_EXTENSIONS = Set.of(".java");
    public static final Set<String> CPP_ALLOWED_EXTENSIONS  = Set.of(".cpp");
    public static final Set<String> C_ALLOWED_EXTENSIONS    = Set.of(".c");
    public static final Set<String> PY_ALLOWED_EXTENSIONS   = Set.of(".py");
    private static FileManager instance;

    private Path rootdir;
    private SFile currentFile;
    private final ArrayList<SFile> all_files;
    private ArrayList<SFile> s_files;
    private String language;
    private FileWatcher fileWatcher;
    private Thread watcherThread;


    // NOTE: for RENAMING files just use Files.move(), the fileWatcher will do its job
    // file renaming and deleting forgot to fking add these xD
    /****************** File Explorer Helper ******************/
    public boolean renameFile(SFile sfile, String newName) {
        try {
            Path oldPath = sfile.getPath();
            Path newPath = oldPath.resolveSibling(newName);

            if (Files.exists(newPath)) {
                System.err.println("File already exists: " + newPath);
                return false;
            }

            Files.move(oldPath, newPath);
            synchronized (all_files) {
                if (currentFile != null && currentFile.equals(sfile)) {
                    currentFile = all_files.get(all_files.size()-1);
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to rename file: " + e.getMessage());
            return false;
        }
    }
    public boolean deleteFile(SFile sfile) {
        if (sfile == null) return false;

        Path filePath = sfile.getPath();
        if (!Files.exists(filePath)) {
            System.err.println("File not found: " + filePath);
            return false;
        }
        removeFile(this,sfile.path);
        sfile.delete();
        if (currentFile != null && currentFile.equals(sfile)) {
            currentFile = null;
        }
        System.out.println("[DELETED] " + filePath);
        return true;

    }
    public boolean isAllowedFile(String filename) {
        if (filename == null) return false;
        return ALL_ALLOWED_EXTENSIONS.stream().anyMatch(filename::endsWith);
    }
    public boolean createFolder(Path targetDir, String folderName) {
        if (targetDir == null || folderName == null || folderName.isBlank()) {
            return false;
        }
        Path newDirPath = targetDir.resolve(folderName);

        try {
            if (Files.exists(newDirPath)) {
                System.out.println("Error: Directory already exists at " + newDirPath);
                return false;
            }
            Files.createDirectory(newDirPath);
            System.out.println("Folder created successfully: " + newDirPath);
            return true;

        } catch (IOException e) {
            System.err.println("Failed to create directory: " + newDirPath + ". Error: " + e.getMessage());
            return false;
        }
}
    public boolean deleteFolder(Path directoryPath) {
        if (!Files.exists(directoryPath)) {
            System.out.println("Directory does not exist: " + directoryPath);
            return false;
        }
        try {
            Files.walkFileTree(directoryPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    System.out.println("Deleted directory: " + dir.getFileName());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    System.out.println("Deleted file: " + file.getFileName());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("Failed to delete/access: " + file + " Error: " + exc.getMessage());
                    throw exc; // Propagate the error
                }
            });
        } catch (IOException e) { return false; }
        return true;
    }

    /****************** File Manager ******************/
    // alll of these functions by FileWatcher
    // TODO @ETHAN: you add function CALLS here to refresh your fileExplorer
    public static void addFile(FileManager fm, Path file) {
        fm.all_files.add(SFile.open(file));
        FileExplorer.getInstance().addFileNodeToTree(file);
    }
    public static void removeFile(FileManager fm, Path file) {
        for (SFile sfile : fm.all_files) {
            if (sfile.getPath().equals(file)) {
                fm.all_files.remove(sfile);
                FileExplorer.removeFileNodeFromTree(file);
                return;
            }
        }
    }
    public static void removeDir(FileManager fm, Path dir) {
        fm.all_files.removeIf(p -> p.getPath().startsWith(dir));
        FileExplorer.removeFileNodeFromTree(dir);
    }

    /****************** INITIALIZERS/BUILDERS ******************/
    public FileManager setLanguage(String language) {
        if (language != null)
            this.language = language.toLowerCase();
        return this;
    }
    public FileManager setRootdir(Path rootPath) throws NotDirException {
        if (null == rootPath) throw new NotDirException();
        rootPath = rootPath.toAbsolutePath().normalize();
        if (!Files.isDirectory(rootPath)) throw new NotDirException();
        this.rootdir = rootPath;
        romanArmy.submit("create_workers", ()->{
            DockerHandler.stopAllDockers();
            DockerHandler.createWorker(rootdir.toString(), DockerHandler.DockerImageType.C_CPP);
            DockerHandler.createWorker(rootdir.toString(), DockerHandler.DockerImageType.JAVA);
            DockerHandler.createWorker(rootdir.toString(), DockerHandler.DockerImageType.PYTHON);
        });
        return this;
    }
    public FileManager setRootdir(String rootDir) throws NotDirException {
        if (null == rootDir) throw new NotDirException();
        return setRootdir(Paths.get(rootDir));
    }
    public FileManager update() {
        if (rootdir == null) return this;
        all_files.clear();
        s_files.clear();
        CCFile.newCache();
        try {
            listAllContents(rootdir, getAllowedExtensions(language));
            openNewFolder();
        } catch (Exception e) {
            System.err.println("Failed to open new folder: " + e.getMessage());
        }
        CCFile.clearOldCache();
        return this;
    }
    public FileManager setAll(Path rootPath, String language) throws NotDirException {
        setLanguage(language);
        setRootdir(rootPath);
        return update();
    }
    public FileManager setAll(String rootDir, String language) throws NotDirException {
        setLanguage(language);
        setRootdir(rootDir);
        return update();
    }


    /****************** GETTERS ******************/
    public Path getRootdir() { return rootdir; }
    public String getLanguage() { return language; }
    public ArrayList<SFile> getFiles() { return all_files; }
    public ArrayList<SFile> getLanguageFiles() {
        return new ArrayList<>(
                all_files.stream()
                        .filter(a -> {
                            String filename = a.getPath().getFileName().toString().toLowerCase();
                            int dot = filename.lastIndexOf('.');
                            if (dot == -1 || !Files.exists(a.path)) return false;
//                            if (a.isReadOnly()) return false;
                            return getAllowedExtensions(language).contains(filename.substring(dot));
                        })
                        .toList()
        );
    }
    public SFile getCurrentFile() { return currentFile; }
    public String getCurrentFileStringPath() {
        if (currentFile == null) return null;
        return getRelativePath(currentFile).toString();
    }

    /****************** HELPERS ******************/
    public void setCurrentFile(SFile currentFile) { this.currentFile = currentFile; }
    public Path getRelativePath(SFile sfile) { return rootdir.relativize(sfile.getPath()); }
    private Set<String> getAllowedExtensions(String lang) {
        if (lang == null) return ALL_ALLOWED_EXTENSIONS;

        return switch (lang.trim().toLowerCase()) {
            case "java" -> JAVA_ALLOWED_EXTENSIONS;
            case "cpp", "c++" -> CPP_ALLOWED_EXTENSIONS;
            case "c" -> C_ALLOWED_EXTENSIONS;
            case "python", "py" -> PY_ALLOWED_EXTENSIONS;
            default -> ALL_ALLOWED_EXTENSIONS;
        };
    }

    /****************** INPUT/OUTPUT ******************/
    // @ TODO: REMOVE sout WHEN DEBUGGING IS DONE
    private void listAllContents(Path rootDir, Set<String> allowed_extensions) throws IOException {
        Path absoluteRootDir = rootDir.toAbsolutePath().normalize();
        final int MAX_DEPTH = 16;
        final int FILE_LIMIT = 512;

        System.out.println("--- Listing files: " + ALL_ALLOWED_EXTENSIONS + " inside: " + absoluteRootDir + " ---");
        try {
            Files.walkFileTree(rootDir, java.util.Collections.emptySet(), MAX_DEPTH, new SimpleFileVisitor<Path>() {
                // DIR FILTER
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                    if (all_files.size() >= FILE_LIMIT) {
                        System.out.println("File limit of " + FILE_LIMIT + " reached. Terminating traversal.");
                        return FileVisitResult.TERMINATE;
                    }

                    // ROOT DIR KEEP ON GETTING SKIPPED SO THIS CODE EXIST
                    if (dir.toAbsolutePath().normalize().equals(absoluteRootDir)) {
                        // System.out.println("[ROOT DIR] " + dir);
                        // TODO@ETHAN ADD ROOT FOLDER NODE using (Path dir)
                        return FileVisitResult.CONTINUE;
                    }

                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";

                    // SKIP HIDDEN FOLDERS
                    if (IGNORED_FOLDERS.contains(name.toLowerCase()) || name.startsWith(".") || name.startsWith("$")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    // TODO@ETHAN ADD FOLDER NODE using (Path dir)
                    // System.out.println("[DIR]  " + dir);
                    return FileVisitResult.CONTINUE;
                }

                // FILE FILTER
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    String fileName = file.getFileName().toString().toLowerCase();

                    // NO JEWS ALLOWED
                    boolean isAllowed = ALL_ALLOWED_EXTENSIONS.stream()
                            .anyMatch(fileName::endsWith);

                    if (isAllowed) {
                        // TODO@ETHAN ADD FILE NODE using (Path dir)
                        // TODO@ETHAN or just use fm.getFiles() your calls

                        SFile sfile = SFile.open(file);
                        all_files.add(sfile);
                        System.out.println("[FILE.open] " + file);
                    }

                    return FileVisitResult.CONTINUE;
                }

                // MAGIC CODE THAT PREVENTS CRASH WHEN ACCESS DENIED
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    System.err.println("Failed to access: " + file + " because " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to open new folder: " + e.getMessage());
        }
    }
    public void saveAll() { for (SFile sfile : all_files) sfile.write(); }

    public void refreshAll()
    {
        for (SFile s : all_files)
        {
            s.read();
        }
    }

    /****************** File Watcher ******************/
    public void openNewFolder() throws Exception {
        if (fileWatcher != null) {
            System.out.println("Stopping old watcher...");

            fileWatcher.closeAndCleanup();

            if (watcherThread != null && watcherThread.isAlive()) {
                watcherThread.interrupt();
                try {
                    watcherThread.join(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        this.fileWatcher = new FileWatcher(this, FileExplorer.getInstance());

        this.watcherThread = new Thread(this.fileWatcher);
        this.watcherThread.setDaemon(true);
        this.watcherThread.start();
    }


    public static FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }
        return instance;
    }
    private FileManager() {
        rootdir = null;
        currentFile = null;
        this.language = null;
        s_files = new ArrayList<>();
        all_files = new ArrayList<>();
    }

    public static void main(String[] args) {

        try {
            FileManager fm = FileManager.getInstance().setAll(".", "java").update();
            SFile s = fm.getFiles().get(0);
            Path p = fm.getRelativePath(s);
            System.out.println("RELATIVE PATH: " + p);
        } catch (Exception ignored) {}
        slaveWorkers.shutdown();
    }
}
