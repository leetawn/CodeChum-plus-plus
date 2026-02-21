package com.exception.ccpp.CCJudge;

import com.exception.ccpp.FileManagement.FileManager;
import com.exception.ccpp.FileManagement.SFile;
import com.exception.ccpp.GUI.UpdateGUICallback;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CCQuestion implements TerminalCallback, Serializable {
    private static final long serialVersionUID = 202602L;
    private Map<Testcase, String> testcases = new LinkedHashMap<>();
    private List<CCBoilerplate> boilerplates = new ArrayList<>();
    private String description;
    private String author;
    private String title;

    TestcasesPanel testcasesPanel = null;


    public CCQuestion() {
        description = "";
        author = "Anonymous";
        title = "Question";
    }

    /************ BASIC OPS *******************/
    public Testcase addTestcase(String[] inputs, String expected) {
        Testcase nTC = new Testcase(inputs, expected);
        testcases.put(nTC, UUID.randomUUID().toString());
        return nTC;
    }
    public void addBoilerplate(String code, String filename, boolean is_hidden, boolean is_readonly) {
        boilerplates.add(new CCBoilerplate(code, filename, is_hidden, is_readonly));
    }
    public void createBoilerplates(FileManager fm) {
        for (CCBoilerplate tc : boilerplates) {
            Path filepath = fm.getRootdir().toAbsolutePath().resolve(tc.filename);
            System.err.printf("boilerplate_file: %s\n",filepath);
            if (Files.exists(filepath) || tc.is_hidden) continue;
            SFile f = SFile.open(filepath);
            f.setContent(tc.code);
            f.setReadOnly(tc.is_readonly);
            f.write();
        }
    }
    public void deleteTestcase(Testcase tc) {
        testcases.remove(tc);
        if (testcasesPanel != null) {testcasesPanel.removeTestcaseCallback(tc); }
    }


    /************ GET/SET *******************/
    public Map<Testcase, String>  getTestcases() { return testcases; }
    public List<CCBoilerplate> getBoilerplates() { return boilerplates; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getTitle() { return title; }

    public void setDescription(String description) { this.description = description; }
    public void setAuthor(String author) { this.author = author; }
    public void setTitle(String title) { this.title = title; }

    /************ GUI OPS *******************/
    public void openTestcaseDialog(FileManager fm, UpdateGUICallback gui_cb) {
        TerminalApp.getInstance().stopSetAll(fm,this, gui_cb).start();
    }
    public void manualAddTestcase(Testcase res, UpdateGUICallback gui_cb)
    {
        onTerminalExit(res.getInputs(), res.getExpectedOutput());
        gui_cb.updateGUI();
    }

    /************ CALLBACKS *******************/
    @Override
    public void onTerminalExit(String[] inputs, String expected) {
        Testcase nTC = addTestcase(inputs, expected);
        if (testcasesPanel != null) testcasesPanel.addTestcaseCallback(nTC);
    }

}
