package com.exception.ccpp.CCJudge;

import com.exception.ccpp.FileManagement.CCFile;
import com.exception.ccpp.FileManagement.CrypticWriter;
import com.exception.ccpp.FileManagement.FileManager;
import com.exception.ccpp.FileManagement.SFile;
import com.exception.ccpp.GUI.UpdateGUICallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// Testcase contains output and inputs
// Question contains descriptions and boilerplate code
public class QuestionFile extends CCFile implements TerminalCallback{
        private Map<Testcase, String> testcases = new LinkedHashMap<>();
        private List<CCBoilerplate> boilerplates = new ArrayList<>();
        private String description;
        private String title;
        private String author;

    TestcasesPanel testcasesPanel = null;


    private QuestionFile(Path path, boolean should_read) {
        super(path);
        description = "";
        author = "Anonymous";
        title = "Activity";
        if (should_read)
            read();
    }

    /************ GETTERS *******************/
    public Map<Testcase, String>  getTestcases() { return testcases; }
    public List<CCBoilerplate> getBoilerplates() { return boilerplates; }
    public String getDescription() { return description; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }

    /************ SETTERS *******************/
    public void setDescription(String description) { this.description = description; }
    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }

    /************ BASIC OPS *******************/
    public void deleteTestcase(Testcase tc) {
        testcases.remove(tc);
        if (testcasesPanel != null) {testcasesPanel.removeTestcaseCallback(tc); }
    }
    public Testcase addTestcase(String[] inputs, String expected) {
        Testcase nTC = new Testcase(inputs, expected);
        testcases.put(nTC, UUID.randomUUID().toString());
        return nTC;
    }
    public void addBoilerplate(String code, String filename, boolean is_hidden, boolean is_readonly) {
        boilerplates.add(new CCBoilerplate(code, filename, is_hidden, is_readonly));
    }
    public void createBoilerplates(FileManager fm) {
        System.err.println(boilerplates.size());
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

    /************ GUI OPS *******************/
    public void openTestcaseDialog(FileManager fm, UpdateGUICallback gui_cb) {
        TerminalApp.getInstance().stopSetAll(fm,this, gui_cb).start();
    }
    public void manualAddTestcase(Testcase res, UpdateGUICallback gui_cb)
    {
        onTerminalExit(res.getInputs(), res.getExpectedOutput());
        gui_cb.updateGUI();
    }

    /************ I/O *******************/

    public static QuestionFile open(String path) { return open(path, true); }
    public static QuestionFile open(Path path)   { return open(path, true); }
    public static QuestionFile open(String path, boolean should_read) {
        return open(Paths.get(path), should_read);
    }
    public static QuestionFile open(Path path, boolean should_read) {
        CCFile f = getCached(path);
        if (f == null) return new QuestionFile(path, should_read);
        assert f instanceof QuestionFile;
        return (QuestionFile) f;
    }

    @Override
    protected void read() {
        if (path == null) return;

        try {
            List<Object> retrievedData = CrypticWriter.readEncryptedData(path);
            if (retrievedData.size() >= 5) {
                testcases = (LinkedHashMap<Testcase, String>) retrievedData.get(0);
                boilerplates = (ArrayList<CCBoilerplate>) retrievedData.get(1);
                description = (String) retrievedData.get(2);
                author = (String) retrievedData.get(3);
                title = (String) retrievedData.get(4);
            }
        } catch (Exception e) {
            System.err.println("TestcaseFile.load() error loading " + path);
            e.printStackTrace();
        }
    }

    @Override
    public void write() {
        if (path == null) return;

        List<Object> data = new ArrayList<>();
        data.add(testcases);
        data.add(boilerplates);
        data.add(description);
        data.add(author);
        data.add(title);
        try {
            CrypticWriter.writeEncryptedData(data, path);
        } catch (Exception e) {
            System.err.println("TestcaseFile.writeOut() error writing " + path);
            e.printStackTrace();
        }
    }

    /************ CALLBACKS *******************/
    @Override
    public void onTerminalExit(String[] inputs, String expected) {
        Testcase nTC = addTestcase(inputs, expected);
        if (testcasesPanel != null) testcasesPanel.addTestcaseCallback(nTC);
    }
}
