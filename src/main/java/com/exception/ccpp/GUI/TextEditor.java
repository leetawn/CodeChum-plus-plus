package com.exception.ccpp.GUI;

import com.exception.ccpp.CCJudge.TestcaseButton.TestcaseLogo;
import com.exception.ccpp.CCJudge.Judge.JudgeVerdict;
import com.exception.ccpp.CCJudge.TestcasesPanel.TCEntry;
import com.exception.ccpp.CCJudge.*;
import com.exception.ccpp.CustomExceptions.NotDirException;
import com.exception.ccpp.FileManagement.CCJsonMapper;
import com.exception.ccpp.FileManagement.DialogHelper;
import com.exception.ccpp.Gang.DiffSlave;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.util.SystemFileChooser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.exception.ccpp.FileManagement.SFile;
import com.exception.ccpp.FileManagement.FileManager;

import java.awt.event.*;
import javax.swing.text.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;


import static com.exception.ccpp.Common.Helpers.parallelSplit;
import static com.exception.ccpp.Common.Helpers.SplitTask;
import static com.exception.ccpp.Gang.SlaveManager.romanArmy;

public class TextEditor extends JPanel {

    private JButton runCodeButton;
    private JButton openFolderButton;
    private JButton createFolderButton;
    RoundedButton setEntryPointButton;
    JButton submitCodeButton;
    private RSyntaxTextArea codeArea;

    JComboBox<String> languageSelectDropdown;
    private FileExplorer fileExplorerPanel;

    private JTextPane expectedOutputArea;
    private JButton importTestcaseButton;
    private JButton manageTestcaseButton;

    private SimpleAttributeSet matchStyle;
    private SimpleAttributeSet mismatchStyle;
    private SimpleAttributeSet excessStyle;
    private SimpleAttributeSet defaultStyle;

    private JPanel diffMenu;
    private JPanel tcMenu;
    private boolean is_diff_open;
    private StyledDocument actual_null_doc, expected_null_doc;
    private JEditorPane q_description;
    private JLabel q_title;
    private JLabel q_author;
    private final int ICON_SIZE = 20;

    static String oldLanguage;
    private static TextEditor instance;

    public static final String CCPP_FILE_DESC = "CC++ File";
    public static final String CCPP_EXT = "ccpp";
    public static Document defaultDoc;
    TestcasesPanel testcasesPanel;
    JSplitPane mainSplit;
    JSplitPane centerRightSplit;
    JLabel textEditorLabel;
    JTextPane actualOutputArea;

    public ActivityFile current_activity;
    public CCQuestion current_question;
    public QuestionList q_button_container;

    public static TextEditor getInstance()
    {
        if (instance == null) instance = new TextEditor();
        return instance;
    }

    private TextEditor() {
        initializeComponents();
        initializeBackend();
        initializeStyles();
        setupLayout();
        setupEventListeners();
    }

    public TextEditor setAll(String folderPath, MainMenu mainMenu) {
        if (mainMenu != null) {
            mainMenu.setVisible(false);
        }

        try {
            fileExplorerPanel.updateRootDirectory(folderPath);
            setTextArea(false); // disable editing since no file is selected yet
        } catch (NotDirException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load project folder:\n" + ex.getMessage(),
                    "Invalid Folder",
                    JOptionPane.ERROR_MESSAGE);
        }
        return this;
    }

    private void initializeStyles() {
        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(defaultStyle, Color.BLACK);
        StyleConstants.setBackground(defaultStyle, Color.decode("#1f2335"));

        matchStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(matchStyle, Color.BLACK);
        StyleConstants.setBackground(matchStyle, new Color(176, 237, 184)); // Light Green (Match)

        mismatchStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(mismatchStyle, Color.BLACK);
        StyleConstants.setBackground(mismatchStyle, new Color(228, 163, 159)); // Lacking

        excessStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(excessStyle, Color.BLACK); // Use Black FG for visibility on Yellow
        StyleConstants.setBackground(excessStyle, new Color(245, 224, 59)); // Yellow (Excess)
    }

    private void setupLayout() {
        JPanel panel1 = create_first_panel();
        JPanel panel2 = create_second_panel();
        JPanel panel3 = create_third_panel_Container();

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH; // STRICTLY DO NOT EDIT THIS!
        gbc.insets = new Insets(0, 0, 0, 0); // STRICTLY DO NOT EDIT THIS!
        gbc.gridy = 1; // STRICTLY DO NOT EDIT THIS!
        gbc.weighty = 1; // STRICTLY DO NOT EDIT THIS!
        gbc.weightx = 1; // STRICTLY DO NOT EDIT THIS!

        centerRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel2, panel3);
        centerRightSplit.setDividerLocation(200);
        centerRightSplit.setContinuousLayout(true);
        centerRightSplit.setOneTouchExpandable(true);
        centerRightSplit.setResizeWeight(1.0); // center takes extra space when window resizes

        mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1,centerRightSplit);
        mainSplit.setDividerLocation(800); // initial right panel width
        mainSplit.setContinuousLayout(true);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setResizeWeight(1.0); // center takes extra space when window resizes

        centerRightSplit.setDividerSize(5);     // optional: thin divider
        mainSplit.setDividerSize(5);

        add(mainSplit, gbc);
    }

    private JPanel create_first_panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#000000"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;

        panel.add(create_1_3_panel(), gbc);
        panel.setBackground(Color.decode("#191c2a"));

        return panel;
    }

    private JPanel create_1_1_panel(){
        JPanel panel = new JPanel(); // Don't use FixedSizePanel here either!
        panel.setBackground(Color.decode("#191c2a"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        panel.setPreferredSize(new Dimension(0, 30));

        JLabel label = new JLabel("File Explorer");
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        panel.add(label);

        return panel;
    }

    private ImageIcon getScaledIcon(URL imageUrl, int width, int height) {
        if (imageUrl == null) {
            System.err.println("Image URL is null. Resource not found.");
            return new ImageIcon(); // Return an empty icon to prevent null pointer exceptions
        }

        ImageIcon originalIcon = new ImageIcon(imageUrl);
        Image originalImage = originalIcon.getImage();

        Image scaledImage = originalImage.getScaledInstance(
                width,
                height,
                Image.SCALE_SMOOTH
        );

        return new ImageIcon(scaledImage);
    }

    private void configureToolbarButton(JButton button) {
        button.setText(null);
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);

        Dimension fixedSize = new Dimension(22, 22);
        button.setPreferredSize(fixedSize);
        button.setMinimumSize(fixedSize);
        button.setMaximumSize(fixedSize);
    }

    private JPanel create_1_2_panel(){
        JPanel panel = new JPanel();
        panel.setBackground(Color.decode("#191c2a"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        Class<?> contextClass = this.getClass();

        URL openFolderUrl = contextClass.getResource("/assets/open_folder.png");
        URL createFolderUrl = contextClass.getResource("/assets/create_folder.png");

        ImageIcon openFolderIcon = getScaledIcon(openFolderUrl, ICON_SIZE, ICON_SIZE);
        ImageIcon createFolderIcon = getScaledIcon(createFolderUrl, ICON_SIZE, ICON_SIZE);

        openFolderButton = new JButton(openFolderIcon);
        createFolderButton = new JButton(createFolderIcon);

        openFolderButton.setToolTipText("Open Folder");
        createFolderButton.setToolTipText("Create Folder");

        ToolTipManager.sharedInstance().setInitialDelay(500);

        openFolderButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createFolderButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        configureToolbarButton(openFolderButton);
        configureToolbarButton(createFolderButton);

        panel.add(openFolderButton);
        panel.add(createFolderButton);

        return panel;
    }

    private JTabbedPane create_1_3_panel(){
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#191c2a"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        panel.add(create_1_2_panel(), gbc);
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        panel.add(fileExplorerPanel, gbc);

        int sides = 16;
        int top_bot = 8;
        q_description = new JEditorPane();
        q_description.setContentType("text/html");
        q_description.setEditable(false);
        q_description.setFont(new Font(null, Font.PLAIN, 16)); // size 24
        q_description.setBackground(Color.decode("#191c2a"));
        q_description.setBorder(BorderFactory.createEmptyBorder(0, sides, 0, sides));
        q_title = new JLabel("");
        q_title.setFont(new Font(null, Font.BOLD, 18)); // size 24
        q_author = new JLabel("");
        q_author.setFont(new Font(null, Font.ITALIC, 12)); // size 24


        JScrollPane scrollPane = new JScrollPane(q_description);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        q_button_container = new QuestionList();
        q_button_container.setBorder(BorderFactory.createEmptyBorder(4, sides, 10, sides));
        JScrollPane btnScrollPane = new JScrollPane(q_button_container);
        JScrollBar hBar = btnScrollPane.getHorizontalScrollBar();

        btnScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        btnScrollPane.setColumnHeaderView(hBar);

        btnScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        btnScrollPane.setBorder(BorderFactory.createEmptyBorder());
        btnScrollPane.setMinimumSize(new Dimension(60, 60));
        JPanel wrapper =  new JPanel(new GridBagLayout());

        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.weightx = 1.0;
        wrapperGbc.weighty = 1.0;
        wrapperGbc.fill = GridBagConstraints.BOTH;

        wrapper.add(btnScrollPane, wrapperGbc);

        JPanel q_panel = new JPanel(new GridBagLayout());
        gbc.weighty = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        q_panel.add(wrapper, gbc);

        gbc.insets = new Insets(0, sides, 0, sides);
        gbc.gridy++;
        q_panel.add(q_title, gbc);
        gbc.insets = new Insets(0, sides, top_bot, sides);
        gbc.gridy++;
        q_panel.add(q_author, gbc);
        gbc.weighty = 1;
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        q_panel.add(scrollPane, gbc);
        q_panel.setBackground(Color.decode("#191c2a"));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Color.decode("#191c2a")); // Match FileExplorer background
        tabbedPane.add("File Explorer", panel);
        tabbedPane.add("Question", q_panel);


        return tabbedPane;
    }

    private JPanel create_second_panel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.decode("#1f2335"));
        panel.setBorder(BorderFactory.createMatteBorder(
                1, 1, 1, 1, // top, left, bottom, right thickness
                Color.decode("#000000")
        ));

        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0.02;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(create_2_1_panel(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 0.91;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(create_2_2_panel(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1;
        gbc.weighty = 0.07;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(create_2_3_panel(), gbc);

        return panel;
    }

    private JPanel create_2_1_panel(){
        JPanel panel = new FixedSizePanel(1, 20);
        panel.setBackground(Color.decode("#191c2a"));
        panel.setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0, // top, left, bottom, right thickness
                Color.decode("#000000")
        ));

        panel.setLayout(new BorderLayout());

        Color panelBgColor = Color.decode("#191c2a");
        Color foreColor = Color.WHITE;

        textEditorLabel = new JLabel();
        textEditorLabel.setText("Select a file!");
        textEditorLabel.setForeground(foreColor);
        textEditorLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        textEditorLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // Add textEditorLabel to the WEST (left) region
        panel.add(textEditorLabel, BorderLayout.WEST);

        // Create an INNER panel to hold the two right-side components
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(panelBgColor); // Match parent background

        // Use FlowLayout.RIGHT to align components to the right edge. vgap=0 to keep it centered vertically.
        rightPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        // 1. Language Label
        JLabel label2 = new JLabel();
        label2.setText("Language: ");
        label2.setForeground(foreColor);
        label2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        // Ensure the internal editor component is also set correctly
        Component editor = languageSelectDropdown.getEditor().getEditorComponent();
        if (editor instanceof JTextField textField) {
            textField.setOpaque(false);
            textField.setForeground(foreColor);
            textField.setBackground(panelBgColor); // Ensure the background is dark
        }

        // Add components to the right panel
        rightPanel.add(label2);
        rightPanel.add(languageSelectDropdown);

        // Add margin/padding to the right panel (10px on the right side)
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        // Add the rightPanel to the EAST (right) region of the main panel
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel create_2_2_panel(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.decode("#1f2335"));

        // Add the text editor with scroll pane
        codeArea.setBackground(Color.decode("#1f2335"));
        codeArea.setForeground(Color.WHITE);
        RTextScrollPane editorScroll = new RTextScrollPane(codeArea);
        editorScroll.setBorder(null);
        editorScroll.setBackground(Color.decode("#1f2335"));
        org.fife.ui.rtextarea.Gutter gutter = editorScroll.getGutter();
        gutter.setBorderColor(Color.BLACK);
        panel.add(editorScroll, BorderLayout.CENTER);

        return panel;
    }

    public class FixedSizePanel extends JPanel {
        private final Dimension fixedSize;

        public FixedSizePanel(int width, int height) {
            this.fixedSize = new Dimension(width, height);
        }

        @Override
        public Dimension getPreferredSize() {
            return fixedSize;
        }

        // It's good practice to also fix minimum and maximum sizes
        @Override
        public Dimension getMinimumSize() {
            return fixedSize;
        }

        @Override
        public Dimension getMaximumSize() {
            return fixedSize;
        }
    }

    private JPanel create_2_3_panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#191c2a"));
        panel.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, // top, left, bottom, right thickness
                Color.decode("#000000")
        ));
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 1.0;

        // --- LEFT PANEL ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.22;
        JPanel p1 = create_2_3_1_panel();
        p1.setMinimumSize(new Dimension(150, 60)); // Minimum width
//        p1.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        panel.add(p1, gbc);

        // --- MIDDLE PANEL  ---
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.37;
        JPanel p2 = create_2_3_2_panel();
        p2.setMinimumSize(new Dimension(80, 60)); // Minimum width
//        p2.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        panel.add(p2, gbc);

        // --- RIGHT PANEL ---
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.40;
        JPanel p3 = create_2_3_3_panel();
//        p3.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        panel.add(p3, gbc);

        return panel;
    }

    private JPanel create_2_3_1_panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#191c2a"));
        GridBagConstraints gbc = new GridBagConstraints();

        importTestcaseButton = new RoundedButton("Import Testcase", 15);
        importTestcaseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        importTestcaseButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        importTestcaseButton.setForeground(Color.WHITE);
        importTestcaseButton.setBackground(Color.decode("#568afc"));
        importTestcaseButton.setPreferredSize(new Dimension(145, 50));

        manageTestcaseButton = new RoundedButton("Manage Testcase", 15);
        manageTestcaseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        manageTestcaseButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        manageTestcaseButton.setForeground(Color.WHITE);
        manageTestcaseButton.setBackground(Color.decode("#568afc"));
        manageTestcaseButton.setPreferredSize(new Dimension(145, 50));

        // Import button
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(importTestcaseButton, gbc);

        // Export button
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        panel.add(manageTestcaseButton, gbc);

        return panel;
    }

    private JPanel create_2_3_2_panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#191c2a"));
        GridBagConstraints gbc = new GridBagConstraints();

        setEntryPointButton = new RoundedButton("Set Entry Point", 15);
        setEntryPointButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setEntryPointButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        setEntryPointButton.setForeground(Color.WHITE);
        setEntryPointButton.setBackground(Color.decode("#568afc"));

        Dimension fixedSize = new Dimension(110, 40);
        setEntryPointButton.setMinimumSize(fixedSize);
        setEntryPointButton.setMaximumSize(fixedSize);
        setEntryPointButton.setPreferredSize(fixedSize);

        // Hide initially but keep space
        setEntryPointButton.setVisible(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, -25);
        panel.add(setEntryPointButton, gbc);

        // CRITICAL: Add an invisible placeholder that takes the same space
        JLabel placeholder = new JLabel("");
        placeholder.setPreferredSize(fixedSize);
        placeholder.setMinimumSize(fixedSize);
        placeholder.setMaximumSize(fixedSize);
        placeholder.setBackground(Color.decode("#191c2a"));
        placeholder.setOpaque(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 0, -25);
        panel.add(placeholder, gbc); // Add placeholder behind button

        // Use JLayeredPane or keep button on top
        panel.setComponentZOrder(setEntryPointButton, 0); // Button on top
        panel.setComponentZOrder(placeholder, 1); // Placeholder behind

        return panel;
    }

    private JPanel create_2_3_3_panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#191c2a"));
        GridBagConstraints gbc = new GridBagConstraints();

        // Smaller font sizes and minimal padding

        runCodeButton = new RoundedButton("Run Code", 15);
        runCodeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runCodeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        runCodeButton.setForeground(Color.WHITE);
        runCodeButton.setBackground(Color.decode("#568afc"));
        runCodeButton.setPreferredSize(new Dimension(145, 50));

        submitCodeButton = new RoundedButton("Submit Code", 15);
        submitCodeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitCodeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        submitCodeButton.setForeground(Color.WHITE);
        submitCodeButton.setBackground(Color.decode("#39ca79"));
        submitCodeButton.setPreferredSize(new Dimension(145, 50));

        // Run code button
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.36;
        gbc.insets = new Insets(0, 0, 0, -10);
        panel.add(runCodeButton, gbc);

        // Submit code button
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0.31;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(submitCodeButton, gbc);

        return panel;
    }

    private void initializeComponents() {


        codeArea = new RSyntaxTextArea(20, 60);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFractionalFontMetricsEnabled(false);
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        codeArea.setTabsEmulated(true);
        codeArea.setTabSize(4); // 4 spaces per tab
        defaultDoc = codeArea.getDocument();

        InputStream in = getClass().getClassLoader()
                .getResourceAsStream("org/fife/ui/rsyntaxtextarea/themes/monokai.xml");
        try {
            // Keep the text area's font since it has our e.g. ligature hints
            Theme theme = Theme.load(in, codeArea.getFont());
            theme.apply(codeArea);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }


        languageSelectDropdown = new RoundedComboBox<>(new String[]{"C", "C++", "Java", "Python"});
        languageSelectDropdown.setBackground(Color.decode("#568afc"));
        languageSelectDropdown.setForeground(Color.WHITE);
        ((RoundedComboBox<String>) languageSelectDropdown).setRadius(20);
        Dimension d = languageSelectDropdown.getPreferredSize();
        languageSelectDropdown.setPreferredSize(new Dimension(150, 20));
        languageSelectDropdown.setOpaque(false);

        actualOutputArea = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getUI().getPreferredSize(this).width < getParent().getWidth();
            }
        };
        actualOutputArea.setBackground(Color.decode("#191c2a"));
        actualOutputArea.setCaretColor(Color.WHITE);
        actualOutputArea.setForeground(Color.WHITE);

        expectedOutputArea = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getUI().getPreferredSize(this).width < getParent().getWidth();
            }
        };
        expectedOutputArea.setBackground(Color.decode("#191c2a")) ;
        expectedOutputArea.setCaretColor(Color.WHITE);
        expectedOutputArea.setForeground(Color.WHITE);

        fileExplorerPanel = new FileExplorer(null, codeArea, this);
    }

    // Container for both TestcasePanel and DiffPanel
    private JPanel create_third_panel_Container(){
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.setBorder(BorderFactory.createMatteBorder(
                0, 1, 0, 0, // top, left, bottom, right thickness
                Color.decode("#000000")
        ));

        // DONE@GLENSH: add hide fucntionality
        //  TODO: himo pa oga action listener, show / hide which one
        tcMenu = create_third_panel_Testcase();
        diffMenu = create_third_panel_Diff();
        is_diff_open = false;
        cardPanel.add(tcMenu, "TESTCASE_VIEW");
        cardPanel.add(diffMenu, "DIFF_VIEW");

        return cardPanel;
    }

    // For TestcaseListPanel ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private JPanel create_third_panel_Testcase(){
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#1a1c2a"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        panel.add(create_third_panel_Testcase_content(), gbc);

        return panel;
    }

    private JPanel create_third_panel_Testcase_content(){
        RoundedPanel panel = new RoundedPanel(new GridBagLayout(), 40, "#1f2335");
        panel.setBorderThickness(1);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;

        // TITLE SECTION
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(create_testcases_title(), gbc);

        // Scrollable content section
        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0,0,0,0);
        gbc.fill = GridBagConstraints.BOTH;
        testcasesPanel = TestcasesPanel.getInstance();
        panel.add(testcasesPanel, gbc);

        return panel;
    }

    private JPanel create_testcases_title(){
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#1f2335"));

        JLabel label = new JLabel("Testcases");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(12,12,12,12);
        panel.add(label, gbc);

        return panel;
    }

    private JPanel create_third_panel_Diff() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.decode("#1f2335"));

        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(create_3_1_panel(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(create_3_2_panel(), gbc);

        return panel;
    }

    private JPanel create_3_1_panel(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createMatteBorder(
                0, 0, 0, 0, // top, left, bottom, right thickness
                Color.decode("#000000")
        ));
        panel.setBackground(Color.decode("#1f2335"));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.decode("#1f2335"));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

        JButton backButton = new JButton("X");
        backButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(Color.decode("#1f2335"));
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> closeDiffMenu());
        headerPanel.add(backButton, BorderLayout.WEST);

        JLabel label = new JLabel();
        label.setText("Actual Output");
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setBorder(BorderFactory.createEmptyBorder(-5, -50, 0, 0));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        headerPanel.add(label, BorderLayout.CENTER);

        panel.add(headerPanel, BorderLayout.NORTH);

        actualOutputArea.setBackground(Color.decode("#1f2335"));
        actualOutputArea.setForeground(Color.WHITE);
        JScrollPane actualScroll = new JScrollPane(actualOutputArea);
        actualScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        actualScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        actualScroll.setBackground(Color.decode("#1f2335"));
        actualScroll.setBorder(null);
        panel.add(actualScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel create_3_2_panel(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, // top, left, bottom, right thickness
                Color.decode("#000000")
        ));
        panel.setBackground(Color.decode("#1f2335"));

        JLabel label = new JLabel();
        label.setText("Expected Output");
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        label.setBorder(BorderFactory.createEmptyBorder(15, 5, 0, 0));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        panel.add(label, BorderLayout.NORTH);

        // Add the expected output area
        expectedOutputArea.setBackground(Color.decode("#1f2335"));
        expectedOutputArea.setForeground(Color.WHITE);
        JScrollPane expectedScroll = new JScrollPane(expectedOutputArea);
        expectedScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        expectedScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        expectedScroll.setBackground(Color.decode("#1f2335"));
        expectedScroll.setBorder(null);
        panel.add(expectedScroll, BorderLayout.CENTER);

        return panel;
    }

    private void setupEventListeners() {
        // save ctrl+s migrated to main frame

        openFolderButton.addActionListener(new ButtonHandlers.OpenFolderButtonHandler(this));
        languageSelectDropdown.addActionListener(new ButtonHandlers.LanguageSelectHandler(this));
        runCodeButton.addActionListener(new ButtonHandlers.RunButtonHandler(this));
        submitCodeButton.addActionListener(new ButtonHandlers.SubmitButtonHandler(this));
        createFolderButton.addActionListener(e -> {
            fileExplorerPanel.handleCreateFolderAction();
        });
        setEntryPointButton.addActionListener(new ButtonHandlers.SetEntryPointButtonHandler(this));
        importTestcaseButton.addActionListener(new ButtonHandlers.ImportTestcaseButtonHandler(this));
        manageTestcaseButton.addActionListener(new ButtonHandlers.ManageTestcaseButtonHandler(this));

    }

    private void initializeBackend() {

        Font font = codeArea.getFont();
        codeArea.setFont(codeArea.getFont().deriveFont(font.getStyle(),16));
        setCodeAreaText("No selected file. Select a file to start editing.");
        codeArea.setEditable(false);

        oldLanguage = getCurrentSelectedLanguage();
        actualOutputArea.setEditable(false);
        actualOutputArea.setCaretColor(Color.decode("#1f2335"));
        expectedOutputArea.setEditable(false);
        expectedOutputArea.setCaretColor(Color.decode("#1f2335"));
        actualOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        expectedOutputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        actual_null_doc = actualOutputArea.getStyledDocument();
        expected_null_doc = expectedOutputArea.getStyledDocument();
    }




    /**********************/
    /**********************/
    /**********************/
    /**********************/
    /**********************/
    /**********************/
    /**********************/
    /**********************/
    /**********************/
    public void setDiffMenuDoc(StyledDocument actual, StyledDocument expected) {
        if (actual == null || null == expected) {
            actual = actual_null_doc;
            expected = expected_null_doc;
        };

        actualOutputArea.setDocument(actual);
        expectedOutputArea.setDocument(expected);
        actualOutputArea.revalidate();
        expectedOutputArea.revalidate();
    }

    public void openDiffMenu() {
        is_diff_open = true;
        diffMenu.setVisible(is_diff_open);
        tcMenu.setVisible(!is_diff_open);
    }
    public void closeDiffMenu() {
        is_diff_open = false;
        diffMenu.setVisible(is_diff_open);
        tcMenu.setVisible(!is_diff_open);
    }

    public void setCodeAreaText(String codeAreaText) {
        this.codeArea.setDocument(TextEditor.defaultDoc);
        codeArea.setText(codeAreaText);
    }

    private int BUFFER_MAX_CHARS = 10000;
    public int displayActualDiff(String[] actualLines, String[] expectedLines, StyledDocument doc) {
        if (doc == null) return -1;

        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {}

        int maxLines = Math.max(actualLines.length, expectedLines.length);

        AttributeSet prevStyle;
        AttributeSet styleToApply = null;
        StringBuilder buffer = new StringBuilder();
        boolean excess = false, mismatch = false;
        for (int i = 0; i < maxLines; i++) {
            String actualLine = (i < actualLines.length) ? actualLines[i].trim() : "";
            String expectedLine = (i < expectedLines.length) ? expectedLines[i].trim() : "";
            int maxLength = Math.max(actualLine.length(), expectedLine.length());

            // full line, char by char
            for (int j = 0; j < maxLength; j++) {
                char actualChar = (j < actualLine.length()) ? actualLine.charAt(j) : 0;
                char expectedChar = (j < expectedLine.length()) ? expectedLine.charAt(j) : 0;

                char charToProcess = actualChar; // Start with the actual character

                prevStyle = styleToApply;
                if (expectedChar == 0 && actualChar != 0) {
                    styleToApply = excessStyle;
                    excess = true;
                }
                else if (expectedChar != 0 && actualChar == expectedChar) {
                    styleToApply = matchStyle; // Green
                }
                else {
                    styleToApply = mismatchStyle;
                    mismatch = true;
                    if (actualChar == 0) charToProcess = ' ';
                }

                // flush first
                if (prevStyle != null && !buffer.isEmpty())
                    if (prevStyle != styleToApply || buffer.length() > BUFFER_MAX_CHARS) {
                        final AttributeSet final_style = prevStyle;
                        final String final_str = buffer.toString();
                        SwingUtilities.invokeLater(() ->
                        {
                            try {
                                doc.insertString(doc.getLength(), final_str, final_style);
                            } catch (BadLocationException ignored) {}
                        });
                        buffer.delete(0, buffer.length());
                    }

                if (charToProcess == '\t') buffer.append("    "); // 4 spaces for Tab
                else if (charToProcess == '\r' || charToProcess == '\n') buffer.append("\u2424"); // Symbol for Newline
                else if (charToProcess == 0 || charToProcess == ' ') buffer.append(" ");
                else buffer.append(charToProcess);
            }

            // newline if not the last line
            if (i < maxLines - 1) {
                buffer.append("\n");
            }
        }
        if (!buffer.isEmpty()) {
            final AttributeSet final_style = styleToApply;
            final String final_str = buffer.toString();
            SwingUtilities.invokeLater(() ->
            {
                try {
                    doc.insertString(doc.getLength(), final_str, final_style);
                } catch (BadLocationException ignored) {}
            });
        }
        if (mismatch) { return 2; }
        if (excess) { return 1; }
        return 0;
    }

    public void displayExpectedDiff(String[] actualLines, String[] expectedLines, StyledDocument doc) {
        if (doc == null) return;

        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {}

        int maxLines = Math.max(actualLines.length, expectedLines.length);

        AttributeSet prevStyle;
        AttributeSet styleToApply = null;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            String actualLine = (i < actualLines.length) ? actualLines[i] : "";
            String expectedLine = (i < expectedLines.length) ? expectedLines[i] : "";

            for (int j = 0; j < expectedLine.length(); j++) {
                char actualChar = (j < actualLine.length()) ? actualLine.charAt(j) : 0;
                char expectedChar = (j < expectedLine.length()) ? expectedLine.charAt(j) : 0;

                char charToProcess = expectedChar; // Focus on the expected character

                if (expectedChar == 0) {
                    continue;
                }

                prevStyle = styleToApply;
                if (actualChar == expectedChar) {
                    styleToApply = matchStyle; // Green
                } else {
                    styleToApply = mismatchStyle; // Red
                }

                if (prevStyle != null && !buffer.isEmpty())
                    if (prevStyle != styleToApply || buffer.length() > BUFFER_MAX_CHARS) {
                        final AttributeSet final_style = prevStyle;
                        final String final_str = buffer.toString();
                        SwingUtilities.invokeLater(() ->
                        {
                            try {
                                doc.insertString(doc.getLength(), final_str, final_style);
                            } catch (BadLocationException ignored) {}
                        });
                        buffer.delete(0, buffer.length());
                    }

                if (charToProcess == '\t') {
                    buffer.append("    "); // 4 spaces for Tab
                } else if (charToProcess == '\r' || charToProcess == '\n') {
                    buffer.append("\u2424"); // Symbol for Newline
                } else {
                    buffer.append(charToProcess);
                }
            }

            if (i < maxLines - 1 && i < expectedLines.length) { // Only add newline if Expected has more lines
                buffer.append("\n");
            }
        }
        if (!buffer.isEmpty()) {
            final AttributeSet final_style = styleToApply;
            final String final_str = buffer.toString();
            SwingUtilities.invokeLater( () ->
            {
                try {
                    doc.insertString(doc.getLength(), final_str, final_style);
                } catch (BadLocationException ignored) {}
            });
        }
    }

    public void saveCurrentFileContent() {
        SFile currentFile = fileExplorerPanel.getSelectedFile(); // <-- Use the new source of truth

        if (currentFile != null) {
            currentFile.write();
            updateUnsavedIndicator(false);
            System.out.println("File saved: " + currentFile.getStringPath());
        }
    }

    public void handleAddFileAction() {
        FileManager fm = fileExplorerPanel.getFileManager();
        if (fm == null) return;
        if (fm.getRootdir() == null) return;

        DefaultMutableTreeNode selectedNode = fileExplorerPanel.getSelectedNode();
        Path targetDir = fm.getRootdir();
        DefaultMutableTreeNode parentNodeInTree;

        // Logic to determine target directory and parent node (for insertion in JTree)
        if (selectedNode != null) {
            Object obj = selectedNode.getUserObject();

            if (obj instanceof SFile sfile) {
                if (Files.isDirectory(sfile.getPath())) {
                    targetDir = sfile.getPath();
                    parentNodeInTree = selectedNode;
                } else {
                    targetDir = sfile.getPath().getParent();
                    parentNodeInTree = (DefaultMutableTreeNode) selectedNode.getParent();
                }
            } else {
                targetDir = fileExplorerPanel.resolveNodeToPath(selectedNode);
                parentNodeInTree = selectedNode;
            }
        } else {
            parentNodeInTree = (DefaultMutableTreeNode) fileExplorerPanel.getFeTree().getModel().getRoot();
        }

        String fileName = JOptionPane.showInputDialog(this, "Enter new file name (with extension):");
        if (fileName == null || fileName.isBlank()) return;

        // Use the FileManager's language-based validation for new files
        if (!fm.isAllowedFile(fileName)) {
            JOptionPane.showMessageDialog(this,
                    "Invalid file extension for the current project language (" + fm.getLanguage() + ").",
                    "Invalid Extension",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Path newFilePath = targetDir.resolve(fileName);

            if (Files.exists(newFilePath)) {
                JOptionPane.showMessageDialog(this, "File already exists in " + targetDir);
                return;
            }

            saveCurrentFileContent();
            SFile s = SFile.open(newFilePath);
            s.attachTo(codeArea);
            JOptionPane.showMessageDialog(this, "File created: " + newFilePath.getFileName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error creating file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

    }

    public void updateUnsavedIndicator(boolean isDirty) {
        String currentText = textEditorLabel.getText();

        final String ASTERISK_INDICATOR = " *";

        String cleanText = currentText;
        if (currentText.endsWith(ASTERISK_INDICATOR)) {
            cleanText = currentText.substring(0, currentText.length() - ASTERISK_INDICATOR.length());
        }

        if (isDirty) {
            if (!currentText.endsWith(ASTERISK_INDICATOR)) {
                textEditorLabel.setText(cleanText + ASTERISK_INDICATOR);
                System.out.println("LABEL: " + textEditorLabel.getText());
            }
        } else {
            textEditorLabel.setText(cleanText);
        }
    }

    public String getCurrentSelectedLanguage() {
        if (languageSelectDropdown.getSelectedItem().equals("C++")) return "cpp";
        if (languageSelectDropdown.getSelectedItem().equals("Python")) return "py";
        return ((String) languageSelectDropdown.getSelectedItem()).toLowerCase();
    }

    public void setTextArea(boolean ok) {
        this.codeArea.setEditable(ok);
    }

    public static void makeCoolAndNormal() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.out.println("nooooooo garbage ui for now");
        }
    }

    public JButton getSetEntryPointButton() {
        return setEntryPointButton;
    }


    public void setActivityFile(ActivityFile af) {
        current_activity = af;
        List<CCQuestion> questions = af.getQuestions();
        if (questions == null || questions.isEmpty()) return;
        CCQuestion q = questions.get(0);
        setQuestion(q);
        q_button_container.setQuestions(af.getQuestions());
    }

    public void setQuestion(CCQuestion q)
    {
        current_question = q;
        testcasesPanel.setQuestion(q);
        fileExplorerPanel.setQuestion(q);

        q_author.setText(q.getAuthor());
        q_description.setText("<html>" + q.getDescription() + "</html>");
        q_title.setText(q.getTitle());
    }

    boolean canProceedRunCode() {
        FileExplorer fExplorer = FileExplorer.getInstance();
        String sel_lang = (String) this.languageSelectDropdown.getSelectedItem();
        SFile sel_file = fExplorer.getSelectedFile();
        if (sel_lang == null) return false; // idk how we got in this point;
        sel_lang = sel_lang.toLowerCase();

        boolean is_java_python = switch (sel_lang) { case "python", "java" -> true; default -> false; };
        if (is_java_python) {
            if (sel_file == null)
            {
                JOptionPane.showMessageDialog(this, "Yo, compilers aren't smart enough to run null files, so select one.", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            else if (!(fExplorer.getFileExtension(sel_file.getPath().toString()).equals(this.getCurrentSelectedLanguage())))
            {
                JOptionPane.showMessageDialog(this, "can you please select the correct compiler for your language please user please please please", "Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return true;
    }


    /* --------------- Button Handlers --------------- */
}