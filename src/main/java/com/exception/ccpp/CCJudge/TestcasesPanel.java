package com.exception.ccpp.CCJudge;

import com.exception.ccpp.GUI.TextEditor;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TestcasesPanel extends JPanel {
    private static TestcasesPanel instance;
    final GridBagConstraints gbc = new GridBagConstraints();
    final Insets testcaseInsets;
    final int monitorWidth;
    final int monitorHeight;
    JPanel mainPanel;

    QuestionFile file;
    final Map<Testcase, TCEntry> activeTestcases = new ConcurrentHashMap<>();
    final ArrayList<TestcaseButton> activeBtns = new ArrayList<>();
    final Deque<TCEntry> entryPool = new ConcurrentLinkedDeque<>();

    public  Map<Testcase, TCEntry> getActiveTestcases() {
        return activeTestcases;
    }
    private void clearActiveTescases() {

        for (Map.Entry<Testcase, TCEntry> e : activeTestcases.entrySet()) {
            TCEntry entry = e.getValue();
            mainPanel.remove(entry.btn);
            entryPool.add(entry);
        }
        activeTestcases.clear();
        activeBtns.clear();
    }

    public void setTestcaseFile(QuestionFile file) {
        if (file == null) return;
        if (this.file == file) return;
        if (this.file != null) this.file.testcasesPanel = null; // remove ref callback
        TextEditor.getInstance().closeDiffMenu();
        this.file = file;
        this.file.testcasesPanel = this;
        clearActiveTescases();
        int i = 0;

        /***** CREATE BUTTONS *****/
        TextEditor te = TextEditor.getInstance();
        for (Testcase tc : file.getTestcases().keySet())
        {
            helperAddTestcase(te,tc,i++);
        }
        mainPanel.revalidate();

    }
    private TestcaseButton addTestcase(TextEditor te, Testcase tc, TCEntry entry, int i) {
        TestcaseButton tcBtn = entry.btn;
        StyledDocument expected = entry.expectedDoc;
        try {
            expected.remove(0, expected.getLength());
            expected.insertString(0, tc.getExpectedOutput(), null);
        } catch (BadLocationException e) {}
        tcBtn.addActionListener(e -> {
            System.out.println("OPENING TESTCASE " + (i+1));
            te.setDiffMenuDoc(entry.actualDoc, entry.expectedDoc);
            te.openDiffMenu();
        });
        System.out.println("CREATING TESTCASE " + (i+1));
        gbc.gridy = i;
        gbc.weightx = 1;
        gbc.weighty = 0.0;
        gbc.insets = testcaseInsets;
        mainPanel.add(tcBtn, gbc);
        activeTestcases.put(tc, entry);
        return tcBtn;
    }

    public void addTestcaseCallback(Testcase newFile) {
        helperAddTestcase(TextEditor.getInstance(), newFile, activeTestcases.size());
        mainPanel.revalidate();
    }
    public void removeTestcaseCallback(Testcase tc) {
        TestcaseButton btn = activeTestcases.get(tc).btn;
        if (activeTestcases.remove(tc) != null) System.err.println("REMOVED TESTCASE ");
        int i = 0;
        boolean found = false;
        for (TestcaseButton tb : activeBtns) {
            if (!found && tb == btn) {
                found = true;
                i = tb.getTestcaseNumber();
            } else if (found) {
                tb.setTestcaseNumber(i++);
            }
        }
        activeBtns.remove(btn);
        mainPanel.remove(btn);
        mainPanel.revalidate();
    }

    private void helperAddTestcase(TextEditor te, Testcase tc, int i) {
        TCEntry entry;
        if (entryPool.isEmpty()) entry = new TCEntry(new TestcaseButton(i+1), createDoc(), createDoc());
        else entry = entryPool.pop();
        addTestcase(te, tc, entry, i);
        activeBtns.add(entry.btn);
    }
    private StyledDocument createDoc() {
        StyledDocument doc = new DefaultStyledDocument();
        AbstractDocument adoc = (AbstractDocument) doc;
        adoc.setDocumentFilter(new DocumentFilter() {
            private final int MAX_CHARS = 50_000; // max characters

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                string = enforceMaxLength(fb, string);
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                text = enforceMaxLength(fb, text);
                super.replace(fb, offset, length, text, attrs);
            }

            private String enforceMaxLength(FilterBypass fb, String text) throws BadLocationException {
                if (text == null || text.isEmpty()) return text;

                int currentLength = fb.getDocument().getLength();
                if (text.length() >= MAX_CHARS) {
                    text = text.substring(text.length() - MAX_CHARS);
                    fb.remove(0, currentLength);
                } else if (currentLength + text.length() > MAX_CHARS) {
                    fb.remove(0, (currentLength + text.length()) - MAX_CHARS);
                }
                return text;
            }
        });
        return doc;
    }

    public static TestcasesPanel  getInstance() {
        if (instance == null) instance = new TestcasesPanel();
        return instance;
    }
    private TestcasesPanel() {
        super(new GridBagLayout());
        monitorWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        monitorHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        testcaseInsets = new Insets((int)(monitorHeight * 0.014), (int)(monitorWidth * 0.011), (int)(monitorHeight * 0.004), (int)(monitorWidth * 0.011));

        setBackground(Color.decode("#1f2335"));
        setBorder(BorderFactory.createMatteBorder(
                2, 0, 0, 0,
                Color.decode("#4a77dc")
        ));

        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.decode("#1f2335"));


        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;

        System.out.println("MONITOR WIDTH: " + monitorWidth + " MONITOR HEIGHT: " + monitorHeight);

        gbc.gridy = 20;
        gbc.weighty = 1.0;
        mainPanel.add(Box.createGlue(), gbc);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBackground(Color.decode("#1f2335"));
        scrollPane.getViewport().setBackground(Color.decode("#1f2335"));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.weightx = 1.0;
        wrapperGbc.weighty = 1.0;
        wrapperGbc.fill = GridBagConstraints.BOTH;

        add(scrollPane, wrapperGbc);
    }
    public static class TCEntry {
        public TestcaseButton btn;
        public StyledDocument actualDoc;
        public StyledDocument expectedDoc;
        public TCEntry(TestcaseButton btn, StyledDocument actualDoc, StyledDocument expectedDoc) {
            this.btn = btn;
            this.actualDoc = actualDoc;
            this.expectedDoc = expectedDoc;
        }
    }

}

