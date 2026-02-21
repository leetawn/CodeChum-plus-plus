package com.exception.ccpp.GUI;

import com.exception.ccpp.CCJudge.QuestionFile;
import com.exception.ccpp.CustomExceptions.NotDirException;
import com.exception.ccpp.FileManagement.FileManager;
import com.exception.ccpp.FileManagement.SFile;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.Enumeration; // Keep for JTree enumeration

public class FileExplorer extends JPanel {
    private static JTree fe_tree;
    private static FileManager fileManager;
    private RSyntaxTextArea codeArea;
    private JPopupMenu contextMenu;
    private JMenuItem renameItem;
    private JMenuItem createFolderItem;
    private JMenuItem deleteItem;
    private JMenuItem addFileItem;
    private TextEditor textEditor;
    private SFile selectedFile;
    private QuestionFile questionFile;
    private static FileExplorer fe_instance;

    public FileExplorer(String rootDir, RSyntaxTextArea editorTextArea, TextEditor textEditor){
        fe_instance = FileExplorer.this;
        this.codeArea = editorTextArea;
        this.textEditor = textEditor;
        initializeBackend(rootDir);
        initializeComponents();
        setupLayout();
        setupEventListeners();
        buildFileTree();
    }

    public void updateRootDirectory(String newRootDir) throws NotDirException {
        // Fetch language from TextEditor before updating FileManager
        String currentLang = textEditor.getCurrentSelectedLanguage();
        this.fileManager = fileManager.setAll(newRootDir, currentLang);
        textEditor.setCodeAreaText("");
        // Full rebuild required when root directory changes
        FileExplorer.buildFileTree();
    }

    private void initializeBackend(String rootDir) {
        try {
            // Ensure FileManager is initialized (using getInstance() handles singleton)
            fileManager = FileManager.getInstance();
            if (rootDir == null) return;
            fileManager.setAll(rootDir, textEditor.getCurrentSelectedLanguage());
            selectedFile = null;
            questionFile = null;
        } catch (NotDirException e) {
            JOptionPane.showMessageDialog(this, "Invalid directory: " + e.getMessage());
        }
    }

    private void initializeComponents() {
        fe_tree = new JTree();
        fe_tree.setBackground(Color.decode("#191c2a"));
        fe_tree.setForeground(Color.WHITE);

        // Setting UIManager properties for tree selection is generally unreliable.
        // The custom renderer handles the background/foreground correctly.

        fe_tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree, Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                // Consistent dark theme colors
                setBackground(Color.decode("#191c2a"));
                setForeground(Color.WHITE);
                setOpaque(true);

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObj = node.getUserObject();

                if (userObj instanceof SFile sfile) {
                    setText(sfile.getPath().getFileName().toString());
                    if (Files.isDirectory(sfile.getPath())) {
                        setIcon(UIManager.getIcon("FileView.directoryIcon"));
                    } else {
                        setIcon(UIManager.getIcon("FileView.fileIcon"));
                    }
                } else {
                    // This handles the root node (which is a String)
                    setText(userObj.toString());
                    setIcon(UIManager.getIcon("FileView.directoryIcon"));
                }
                return this;
            }
        });

        contextMenu = new JPopupMenu();
        renameItem =  new JMenuItem("Rename");
        deleteItem = new JMenuItem("Delete");
        createFolderItem = new JMenuItem("Create folder");
        addFileItem = new JMenuItem("Add file");

        // Use a consistent order, putting file actions together
        contextMenu.add(addFileItem);
        contextMenu.add(createFolderItem);
        contextMenu.addSeparator();
        contextMenu.add(renameItem);
        contextMenu.add(deleteItem);
    }

    // ... setupLayout remains unchanged ...

    private void setupLayout() {
        setLayout(new BorderLayout());
        JScrollPane treeScroll = new JScrollPane(fe_tree);

        treeScroll.setPreferredSize(new Dimension(150, 0));

        // ---- DARK COLORS ----
        Color BG = Color.decode("#191c2a");
        Color TITLE = Color.decode("#ffffff");
        Color SCROLL_TRACK = Color.decode("#2a2f45");
        Color SCROLL_THUMB = Color.decode("#3b425c");

        // ---- BORDER WITH DARK THEME ----
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BG),
                " ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("JetBrains Mono", Font.PLAIN, 12),
                TITLE
        );
        treeScroll.setBorder(border);

        // ---- FIX WHITE BACKGROUNDS ----
        treeScroll.setBackground(BG);                   // JScrollPane background
        treeScroll.getViewport().setBackground(BG);    // Viewport background
        this.setBackground(BG);                        // Panel background

        // ---- CUSTOM DARK SCROLLBAR ----
        JScrollBar vBar = treeScroll.getVerticalScrollBar();
        JScrollBar hBar = treeScroll.getHorizontalScrollBar();

        vBar.setBackground(SCROLL_TRACK);
        hBar.setBackground(SCROLL_TRACK);

        vBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = SCROLL_THUMB;
                this.trackColor = SCROLL_TRACK;
            }
        });

        hBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = SCROLL_THUMB;
                this.trackColor = SCROLL_TRACK;
            }
        });

        add(treeScroll, BorderLayout.CENTER);
        setPreferredSize(new Dimension(150, 0));
    }

    static public void refreshFile()
    {
        FileExplorer fe = FileExplorer.getInstance();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) fe_tree.getLastSelectedPathComponent();
        if (node == null) return;

        Object obj = node.getUserObject();
        if (obj instanceof SFile sfile) {
            try {
                FileManager.getInstance().refreshAll();
                String filename = sfile.getPath().getFileName().toString();
                ComponentHandler.getTextEditor().textEditorLabel.setText(filename);

                fe.textEditor.setTextArea(true);
                fe.loadFileContent(sfile);
                fe.textEditor.updateUnsavedIndicator(false);

                TextEditor.getInstance().revalidate();
                TextEditor.getInstance().repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setupEventListeners() {
        fe_tree.addTreeSelectionListener(e -> {
            textEditor.saveCurrentFileContent();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) fe_tree.getLastSelectedPathComponent();
            if (node == null) return;

            Object obj = node.getUserObject();
            if (obj instanceof SFile sfile) {

                if (Files.isDirectory(sfile.getPath())) {
                    System.out.println("Selected folder: " + sfile.getPath().getFileName());
                    return;
                }

                String filename = sfile.getPath().getFileName().toString();
                ComponentHandler.getTextEditor().textEditorLabel.setText(filename);
                textEditor.setTextArea(true);
                loadFileContent(sfile);

                textEditor.updateUnsavedIndicator(false);

            }
        });

        fe_tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showContextMenu(e);
                }
            }
        });

        addFileItem.addActionListener(e -> {
            if (FileManager.getInstance().getRootdir() == null) {
                JOptionPane.showMessageDialog(null, "Please open a root directory", "No set project directory", JOptionPane.ERROR_MESSAGE);
                return;
            }
            textEditor.handleAddFileAction();
        });

        renameItem.addActionListener(e -> {
            if (FileManager.getInstance().getRootdir() == null) {
                JOptionPane.showMessageDialog(null, "Please open a root directory", "No set project directory", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) fe_tree.getLastSelectedPathComponent();
            if (node == null || !(node.getUserObject() instanceof SFile sfile)) return;

            boolean isDirectory = Files.isDirectory(sfile.getPath());
            String itemType = isDirectory ? "folder" : "file";


            String currentName = sfile.getPath().getFileName().toString();
            String newName = JOptionPane.showInputDialog(null,
                    "Enter new name for " + itemType + ":", currentName);

            if (newName == null || newName.isBlank() || newName.equals(currentName)) return;

            if (!isDirectory && !fileManager.isAllowedFile(newName)) {
                JOptionPane.showMessageDialog(null,
                        "Invalid file extension for the current project language (" + fileManager.getLanguage() + ").",
                        "Invalid Extension", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean success = fileManager.renameFile(sfile, newName);

            if (success) {
                JOptionPane.showMessageDialog(null, itemType + " renamed successfully to " + newName);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to rename " + itemType + ".", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        createFolderItem.addActionListener(e -> {
            if (FileManager.getInstance().getRootdir() == null) {
                JOptionPane.showMessageDialog(null, "Please open a root directory", "No set project directory", JOptionPane.ERROR_MESSAGE);
                return;
            }
            handleCreateFolderAction();
        });
        deleteItem.addActionListener(e -> {
            if (FileManager.getInstance().getRootdir() == null) {
                JOptionPane.showMessageDialog(null, "Please open a root directory", "No set project directory", JOptionPane.ERROR_MESSAGE);
                return;
            }
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) fe_tree.getLastSelectedPathComponent();

            if (node == null || !(node.getUserObject() instanceof SFile sfile)) return;

            boolean isDirectory = Files.isDirectory(sfile.getPath());
            String itemType = isDirectory ? "folder" : "file";

            int confirm = JOptionPane.showConfirmDialog(null,
                    "Delete the " + itemType + ": " + sfile.getPath().getFileName() + "?\n(This action cannot be undone.)",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {

                boolean success;

                if (isDirectory) {
                    success = fileManager.deleteFolder(sfile.getPath());
                } else {
                    success = fileManager.deleteFile(sfile);
                }

                if (success) {
                    // Rely on FileWatcher for surgical node removal.

                    // UX Fix: Update the Text Editor state immediately
                    if (sfile.equals(fileManager.getCurrentFile()) || isDirectory) {
                        textEditor.setCodeAreaText("");
                        fileManager.setCurrentFile(null);
                        textEditor.getSetEntryPointButton().setText("Set Entry Point");
                    }

                    JOptionPane.showMessageDialog(null, itemType + " deleted successfully.");
                } else {
                    JOptionPane.showMessageDialog(null, "Failed to delete " + itemType + ". Check permissions or if the folder is in use.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void showContextMenu(MouseEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) fe_tree.getLastSelectedPathComponent();

        if (node != null && node.getUserObject() instanceof SFile) {
            renameItem.setVisible(true);
            deleteItem.setVisible(true);
            addFileItem.setVisible(true);
        } else {
            renameItem.setVisible(false);
            deleteItem.setVisible(false);
            addFileItem.setVisible(true);
        }

        contextMenu.show(fe_tree, e.getX(), e.getY());
    }

    public void handleCreateFolderAction() {
        FileManager fm = getFileManager();
        if (fm == null) return;
        if (fm.getRootdir() == null) return;

        DefaultMutableTreeNode selectedNode = getSelectedNode();
        Path targetDir = fm.getRootdir();

        // Determine the target directory to create the new folder in
        if (selectedNode != null) {
            Object obj = selectedNode.getUserObject();
            if (obj instanceof SFile sfile) {
                targetDir = Files.isDirectory(sfile.getPath()) ? sfile.getPath() : sfile.getPath().getParent();
            } else {
                // If selected node is the String root node
                targetDir = resolveNodeToPath(selectedNode);
            }
        }
        // If selectedNode is null, targetDir remains the rootdir.

        String newFolderName = JOptionPane.showInputDialog(null, "Enter new folder name:");

        if (newFolderName != null && !newFolderName.isBlank()) {
            if (newFolderName.contains(File.separator) || newFolderName.startsWith(".")) {
                JOptionPane.showMessageDialog(null, "Invalid folder name. Cannot contain path separators or start with '.'", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = fm.createFolder(targetDir, newFolderName);

            if (success) {
                // ⭐️ CLEANUP: Removed manual GUI insertion (model.insertNodeInto, expandPath, setSelectionPath).
                // fm.createFolder() triggered the FileWatcher, which will call addFileNodeToTree,
                // handling the surgical insertion, expansion, and selection.
                JOptionPane.showMessageDialog(null, "Folder created successfully in " + targetDir.getFileName());
            } else {
                JOptionPane.showMessageDialog(null, "Failed to create folder.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ----------------------------------------------------------------------------------
    // Core Tree Building Methods
    // ----------------------------------------------------------------------------------

    public static void buildFileTree() {
        // ... (unchanged) ...
        if (fileManager == null) return;

        Path rootPath = fileManager.getRootdir();

        if (rootPath == null || !Files.exists(rootPath)) {
            fe_tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(".")));
            return;
        }

        // The root node uses the file name string
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootPath.getFileName().toString());

        recursivelyAddNodes(rootNode, rootPath);

        fe_tree.setModel(new DefaultTreeModel(rootNode));
        // Expand the root node by default
        fe_tree.expandPath(new TreePath(rootNode));
    }

    /**
     * Helper to find a specific node in the JTree based on its Path.
     */
    static DefaultMutableTreeNode findNodeByPath(Path targetPath) {
        // ... (unchanged, as it was fixed previously) ...
        if (fe_tree == null || targetPath == null) return null;

        DefaultTreeModel model = (DefaultTreeModel) fe_tree.getModel();
        if (model == null) return null;

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        if (root == null) return null;

        // ⭐️ FIX: Normalize the target path once for consistent comparison
        Path normalizedTargetPath;
        try {
            normalizedTargetPath = targetPath.toAbsolutePath().normalize();
        } catch (Exception e) {
            System.err.println("Error normalizing target path: " + e.getMessage());
            return null;
        }

        FileExplorer explorer = getInstance();
        if (explorer != null && explorer.getFileManager() != null) {
            Path rootDir;
            try {
                rootDir = explorer.getFileManager().getRootdir().toAbsolutePath().normalize();
            } catch (Exception e) {
                // Should not happen if rootDir is set correctly
                return null;
            }

            // 1. Handle the root node path (which has a String user object)
            if (normalizedTargetPath.equals(rootDir)) {
                return root;
            }
        }

        Enumeration<javax.swing.tree.TreeNode> e = root.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            Object userObject = node.getUserObject();

            if (userObject instanceof SFile sfile) {
                // Compare normalized SFile path with the normalized target path
                if (sfile.getPath().toAbsolutePath().normalize().equals(normalizedTargetPath)) {
                    return node;
                }
            }
        }
        return null;
    }


    /**
     * Inserts a single file/folder node into the JTree at the correct parent location.
     * Called by the FileWatcher via FileManager.
     */
    public static void addFileNodeToTree(Path filePath) {
        // ... (unchanged) ...
        FileExplorer explorer = getInstance();
        if (explorer == null || fe_tree == null || filePath == null) return;

        // 1. Find the parent directory node in the existing tree
        Path parentPath = filePath.getParent();
        DefaultMutableTreeNode parentNode = findNodeByPath(parentPath);

        if (parentNode == null) {
            // FALLBACK: If we can't find the parent, rebuild the tree to ensure data integrity.
            System.err.println("Parent node for new file not found. Rebuilding tree as fallback.");
            FileExplorer.buildFileTree();
            return;
        }

        // 2. Create the new node
        SFile newSFile = SFile.open(filePath);
        // Check if the file is recognized by the FileManager before adding
        if (!Files.isDirectory(filePath) && !fileManager.isAllowedFile(filePath.getFileName().toString())) {
            // File not recognized (e.g., .exe, .tmp), do not add to tree
            return;
        }
        DefaultMutableTreeNode newFileNode = new DefaultMutableTreeNode(newSFile);
        DefaultTreeModel model = (DefaultTreeModel) fe_tree.getModel();

        // 3. Insert the node.
        model.insertNodeInto(newFileNode, parentNode, parentNode.getChildCount());

        // 4. Update the view: Expand the parent and make the new node visible/selected
        fe_tree.expandPath(new TreePath(parentNode.getPath()));

        TreePath newPath = new TreePath(newFileNode.getPath());
        fe_tree.setSelectionPath(newPath);

        fe_tree.revalidate();
        fe_tree.repaint();
    }

    public static void removeFileNodeFromTree(Path filePath) {
        if (fe_tree == null || filePath == null) return;

        DefaultMutableTreeNode nodeToRemove = findNodeByPath(filePath);

        if (nodeToRemove != null) {
            DefaultTreeModel model = (DefaultTreeModel) fe_tree.getModel();
            model.removeNodeFromParent(nodeToRemove);

            FileExplorer explorer = getInstance();
            if (explorer != null && explorer.getSelectedFile() != null && explorer.getSelectedFile().getPath().equals(filePath)) {
                explorer.textEditor.setCodeAreaText("");
                explorer.setSelectedFile(null);
                explorer.textEditor.updateUnsavedIndicator(false);
            }

            fe_tree.revalidate();
            fe_tree.repaint();
        }
    }

    private static void recursivelyAddNodes(DefaultMutableTreeNode parentNode, Path parentPath) {
        try {
            List<Path> contents = new ArrayList<>();
            try (Stream<Path> stream = Files.list(parentPath)) {
                stream.forEach(contents::add);
            }

            contents.sort((p1, p2) -> {
                boolean isDir1 = Files.isDirectory(p1);
                boolean isDir2 = Files.isDirectory(p2);

                if (isDir1 && !isDir2) return -1;
                if (!isDir1 && isDir2) return 1;

                return p1.getFileName().toString().compareToIgnoreCase(p2.getFileName().toString());
            });

            for (Path childPath : contents) {
                String fileName = childPath.getFileName().toString();

                if (Files.isHidden(childPath) || fileName.startsWith(".")) {
                    continue;
                }

                if (Files.isDirectory(childPath)) {
                    addFolderNode(parentNode, childPath);
                } else {
                    addFileNode(parentNode, childPath);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading directory for tree build: " + e.getMessage());
        }
    }

    private static void addFileNode(DefaultMutableTreeNode parentNode, Path childPath) {
        // ... (unchanged) ...
        ArrayList<SFile> sFiles = fileManager.getFiles();

        SFile targetSFile = sFiles.stream()
                .filter(sf -> sf.getPath().equals(childPath))
                .findFirst()
                .orElse(null);

        if (targetSFile != null) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(targetSFile);
            parentNode.add(childNode);
        }
    }

    private static void addFolderNode(DefaultMutableTreeNode parentNode, Path childPath) {
        String fileName = childPath.getFileName().toString();
        String fileNameLower = fileName.toLowerCase();

        if (FileManager.IGNORED_FOLDERS.contains(fileNameLower)) {
            System.err.println("Skipping ignored directory: " + fileName);
            return;
        }

        SFile dirSFile = SFile.open(childPath);
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(dirSFile);
        parentNode.add(childNode);

        recursivelyAddNodes(childNode, childPath);
    }

    // ----------------------------------------------------------------------------------
    // Rest of the Getters/Setters/Helpers
    // ----------------------------------------------------------------------------------
    public Path resolveNodeToPath(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();

        if (userObject instanceof SFile sfile) {
            return sfile.getPath();
        }
        if (userObject instanceof String) {
            return fileManager.getRootdir();
        }
        return fileManager.getRootdir();
    }

    public FileManager getFileManager() {
        return fileManager;
    }
    public SFile getSelectedFile() {
        return selectedFile;
    }
    public void setSelectedFile(SFile newFile) {
        if (newFile == null) {
            setSyntaxHightlighting(null);
            this.selectedFile = null;
            textEditor.updateUnsavedIndicator(false);
            ComponentHandler.getTextEditor().textEditorLabel.setText("Select a file!");
        }
        else {
            setSyntaxHightlighting(newFile.getStringPath());
            this.selectedFile = newFile;
        }
        codeArea.setTabSize(4);
        codeArea.setTabsEmulated(true);
    }
    public void setSyntaxHightlighting(String path) {
        String ext = getFileExtension(path);
        switch(ext) {
            case "java" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
            case "py" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            case "cpp", "hpp" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
            case "c", "h" -> codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
            default ->  codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        }
    }
    private void loadFileContent(SFile sfile) {
        setSelectedFile(sfile);
        sfile.attachTo(codeArea);
        textEditor.updateUnsavedIndicator(false);
    }
    public QuestionFile getTestcaseFile() {
        return this.questionFile;
    }
    public void setTestcaseFile(QuestionFile newFile) {
        this.questionFile = newFile;
        newFile.createBoilerplates(fileManager);
    }

    public DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) fe_tree.getLastSelectedPathComponent();
    }

    public JTree getFeTree() {
        return fe_tree;
    }
    public static FileExplorer getInstance() {
        return fe_instance;
    }
    public static String getFileExtension(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastDotIndex = filePath.lastIndexOf('.');

        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));

        if (lastDotIndex > lastSeparatorIndex && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
}