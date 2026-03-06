package com.exception.ccpp.GUI;

import com.exception.ccpp.CCJudge.*;
import com.exception.ccpp.CustomExceptions.NotDirException;
import com.exception.ccpp.FileManagement.CCJsonMapper;
import com.exception.ccpp.FileManagement.DialogHelper;
import com.exception.ccpp.FileManagement.FileManager;
import com.exception.ccpp.FileManagement.SFile;
import com.exception.ccpp.Gang.DiffSlave;
import com.formdev.flatlaf.util.SystemFileChooser;
import static com.exception.ccpp.CCJudge.TestcaseButton.TestcaseLogo;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.exception.ccpp.GUI.TextEditor.CCPP_EXT;
import static com.exception.ccpp.GUI.TextEditor.CCPP_FILE_DESC;
import static com.exception.ccpp.Gang.SlaveManager.romanArmy;

public class ButtonHandlers {

    public static class OpenFolderButtonHandler extends ComponentHandler {

        public OpenFolderButtonHandler(TextEditor editor) {
            super(editor);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileExplorer fe = FileExplorer.getInstance();
            FileManager fm = FileManager.getInstance();
            TextEditor te = TextEditor.getInstance();

            SystemFileChooser fileChooser = new SystemFileChooser();
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle("Select Project Root Folder");
            if (fm.getRootdir() != null)  fileChooser.setCurrentDirectory(fm.getRootdir().toFile());

            int result = fileChooser.showOpenDialog(te);

            if (result == SystemFileChooser.APPROVE_OPTION) {
                File selectedDir = fileChooser.getSelectedFile();
                te.saveCurrentFileContent();
                if (selectedDir != null && selectedDir.isDirectory()) {
                    try {
                        fe.updateRootDirectory(selectedDir.getAbsolutePath());
                        te.setTextArea(false);
//                        te.actualOutputArea.setText("Successfully loaded new project: " + selectedDir.getName());
                    } catch (NotDirException ex) {
                        JOptionPane.showMessageDialog(te, "Error loading directory: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
    };

    public static class LanguageSelectHandler extends ComponentHandler {
        public LanguageSelectHandler(TextEditor editor) {
            super(editor);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileManager fileManager = FileManager.getInstance();
            TextEditor te = TextEditor.getInstance();
            String newLanguage = (String)te.languageSelectDropdown.getSelectedItem();
            if (TextEditor.oldLanguage.equals(newLanguage)) return; // no need to reset
            if (newLanguage == null) return;

            boolean showEntryButton = newLanguage.equalsIgnoreCase("Java") ||
                    newLanguage.equalsIgnoreCase("Python");

            // DON'T touch opaque or contentAreaFilled - just use visibility
            te.setEntryPointButton.setVisible(showEntryButton);

            fileManager.setLanguage(newLanguage);
            TextEditor.oldLanguage = newLanguage;

            fileManager.setCurrentFile(null);

            getTextEditor().setEntryPointButton.setText("Set Entry Point");
            getTextEditor().revalidate();
            getTextEditor().repaint();
            System.out.println("Project language changed to: " + newLanguage + ". Entry point reset.");
        }
    }

    public static class RunButtonHandler extends ComponentHandler {
        public RunButtonHandler(TextEditor editor) {
            super(editor);
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            TextEditor te = TextEditor.getInstance();
            te.saveCurrentFileContent();
            if (!te.canProceedRunCode()) return;
            FileManager fm = FileManager.getInstance();
            SwingUtilities.invokeLater(() -> {
                TerminalApp.getInstance().stopSetAll(fm, null, null).start();
            });
        }
    }

    public static class SetEntryPointButtonHandler extends ComponentHandler {
        public SetEntryPointButtonHandler(TextEditor editor) {
            super(editor);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileExplorer fe = FileExplorer.getInstance();
            FileManager fileManager = FileManager.getInstance();
            JTree fe_tree = fe.getFeTree();
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) fe_tree.getLastSelectedPathComponent();
            if (node == null || !(node.getUserObject() instanceof SFile sfile)) return;

            if (Files.isDirectory(sfile.getPath())) {
                JOptionPane.showMessageDialog(null,
                        "yo this is a folder gang you can't set folders as entry points",
                        "Invalid Entry Point", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (getTextEditor().getCurrentSelectedLanguage().equalsIgnoreCase("java")) {
                if (sfile.getStringPath().toLowerCase().endsWith(".java")) fileManager.setCurrentFile(sfile);
                else {
                    JOptionPane.showMessageDialog(null,
                            "i NEED JABAI ENTRY POINT",
                            "Invalid Entry Point", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            if (getTextEditor().getCurrentSelectedLanguage().equalsIgnoreCase("py"))  {
                if (sfile.getStringPath().toLowerCase().endsWith(".py")) fileManager.setCurrentFile(sfile);
                else {
                    JOptionPane.showMessageDialog(null,
                            "i NEED PYTHON ENTRY POINT",
                            "Invalid Entry Point", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
            getTextEditor().setEntryPointButton.setText(String.valueOf(sfile.getPath().getFileName()));
        }
    }

    public static class ManageTestcaseButtonHandler extends ComponentHandler {
        public ManageTestcaseButtonHandler(TextEditor editor) {
            super(editor);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileExplorer fe = FileExplorer.getInstance();
            FileManager fm =  FileManager.getInstance();
            if (fm.getRootdir() == null)
            {
                DialogHelper.showError(getTextEditor(), "Open a project directory first!", "Error");
                return;
            };
            SystemFileChooser fc = DialogHelper.createFileDialog(fm, "Load/Create CC++ Testcase File");
            fc.addChoosableFileFilter(new  SystemFileChooser.FileNameExtensionFilter(CCPP_FILE_DESC, CCPP_EXT, "json"));

            File selected_file = DialogHelper.startDialog(fc, getTextEditor(), "Select a valid file!");
            if (selected_file == null) { return; }
            String file_name = selected_file.getName().toLowerCase();
            if (file_name.endsWith(".ccpp")) {
                try {
                    Path path = selected_file.toPath();
                    ActivityFile tf = ActivityFile.open(path);
                    TestcaseManagerDialog tf_dialog = new TestcaseManagerDialog(SwingUtilities.getWindowAncestor(getTextEditor()), tf);
                    tf_dialog.setVisible(true);
                } catch (Exception ex) {
                    // we don't have to catch NotDir because we only display directories anyway
                    DialogHelper.showError(getTextEditor(), ex.getMessage(), "Error");
                }
            } else if (file_name.endsWith(".json")) {
                try {
                    Path selectedPath = selected_file.toPath();
                    SystemFileChooser ccpp_fc = DialogHelper.createFileDialog(fm, "Save CC++ Testcase File");
                    ccpp_fc.addChoosableFileFilter(new  SystemFileChooser.FileNameExtensionFilter(CCPP_FILE_DESC, CCPP_EXT));
                    File sf = DialogHelper.startSaveDialog(ccpp_fc, getTextEditor(), "Select a valid file!");
                    if (sf == null) { return; }
                    ActivityFile tf = CCJsonMapper.createActivity(selectedPath, sf.toPath());
                    tf.write();
                }catch (Exception ex) {
                    System.err.println("CREATING JSON QTEST ERROR");
                    DialogHelper.showError(getTextEditor(), ex.getMessage(), "Error");
                }
            }
            else DialogHelper.showError(getTextEditor(), "Select a file with a valid extension!", "Error");
        }
    }

    public static class SubmitButtonHandler extends ComponentHandler {
        FileManager fm;
        TextEditor te;
        long startTime;

        public SubmitButtonHandler(TextEditor editor) {
            super(editor);
        }

        private static void setLogo(TestcaseButton btn, Testcase tc, TestcaseLogo status)
        {
            tc.verdict = status;
            btn.setLogo(status);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fm = FileManager.getInstance();
            te = TextEditor.getInstance();

            if (!te.canProceedRunCode()) return;

            fm.saveAll();

            CCQuestion ccq = te.current_question;
            if (ccq == null) {
                JOptionPane.showMessageDialog(te, "Please load a activity file", "No activity", JOptionPane.ERROR_MESSAGE);
                return;
            }
            te.submitCodeButton.setBackground(Color.decode("#0e4728"));
            te.submitCodeButton.setEnabled(false);

            final Map<Testcase, TestcasesPanel.TCEntry> activeTC = TestcasesPanel.getInstance().getActiveTestcases();
            SwingUtilities.invokeLater(() -> {
                for (Map.Entry<Testcase, TestcasesPanel.TCEntry> entry : activeTC.entrySet())
                {
                    entry.getKey().verdict = null;
                    entry.getValue().btn.setLogo(TestcaseLogo.COMPILING);
                }
            });
            startTime = System.currentTimeMillis();
            Judge.judge(FileManager.getInstance(), ccq, (results, verdict) -> {
                long endTime = System.currentTimeMillis();
                System.out.printf("Results finished in : %d milliseconds\n", (endTime - startTime));

                if (results.length <= 0)  {
                    enableButton();
                    return;
                };

                java.util.List<Callable<Integer>> tasks  = new ArrayList<>();
                for (SubmissionRecord s : results)
                {
                    TestcasesPanel.TCEntry entry = activeTC.get(s.testcase());
                    if (verdict == Judge.JudgeVerdict.CE) setLogo(entry.btn, s.testcase(), TestcaseLogo.BLUNDER);
                    tasks.add(new DiffSlave(s, entry.actualDoc, entry.expectedDoc));
                }

                if (verdict == Judge.JudgeVerdict.UE) {
                    enableButton();
                    return;
                };

                System.out.println("[TextEditor] SENDING SLAVES");
                List<Future<Integer>> futures = null;
                try {
                    futures = romanArmy.invokeAll("diff",tasks);
                } catch (InterruptedException ex) {}

                if (verdict == Judge.JudgeVerdict.CE) {
                    enableButton();
                    return;
                };

                int total = 0;
                int status_size = 0;
                int[] status = new int[results.length];
                for (Future<Integer> future : futures) {
                    try {
                        int stat = future.get();
                        status[status_size++] = stat;
                        total += stat;
                    } catch (InterruptedException | ExecutionException ex) {}
                }

                if (status_size != results.length) {
                    System.err.println("[TextEditor] RECEIVED FUTURE NOT EQUAL TO RESULTS");
                }

                for (int i =  0; i < status_size; i++)
                {
                    Testcase tc = results[i].testcase();
                    TestcasesPanel.TCEntry entry = activeTC.get(tc);
                    if (total == 0)  {
                        setLogo(entry.btn, tc, TestcaseButton.TestcaseLogo.BRILLIANT);
                        continue;
                    }
                    switch (status[i]) {
                        case 0: { // best
                            setLogo(entry.btn, tc, TestcaseLogo.BEST);
                            break;
                        }
                        case 1: { // excess
                            setLogo(entry.btn, tc, TestcaseLogo.INACCURACY);
                            break;
                        }
                        case 2: { // mistake
                            setLogo(entry.btn, tc, TestcaseLogo.MISS);
                            break;
                        }
                    }
                }
                enableButton();
            });
        }

        private void enableButton()
        {
            romanArmy.submit("enable",()->{
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                SwingUtilities.invokeLater(() -> {
                    te.submitCodeButton.setBackground(Color.decode("#39ca79"));
                    te.submitCodeButton.setEnabled(true);
                });
            });
        }
    }

    public static class ImportTestcaseButtonHandler extends ComponentHandler {
        public ImportTestcaseButtonHandler(TextEditor editor) {
            super(editor);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            FileExplorer fe = FileExplorer.getInstance();
            FileManager fm =  FileManager.getInstance();
            if (fm.getRootdir() == null)
            {
                DialogHelper.showError(getTextEditor(), "Open a project directory first!", "Error");
                return;
            }
            SystemFileChooser fc = DialogHelper.createFileDialog(fm, "Load CC++ Testcase File");
            fc.addChoosableFileFilter(new SystemFileChooser.FileNameExtensionFilter(CCPP_FILE_DESC, CCPP_EXT));

            File selected_file = DialogHelper.startDialog(fc, getTextEditor(), "Select a valid file!");
            if (selected_file == null) { return; }
            String file_name = selected_file.getName().toLowerCase();

            assert selected_file.isFile(); // no matter what this is physically impossible to be a dir
            if (file_name.endsWith(".ccpp")) {
                ActivityFile tf = ActivityFile.open(selected_file.getPath());
                TextEditor.getInstance().setActivityFile(tf);
            }
        }
    }

}
