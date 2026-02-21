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
public class ActivityFile extends CCFile{
    private List<CCQuestion> questions = new ArrayList<>();
    private String title;

    private ActivityFile(Path path, boolean should_read) {
        super(path);
        title = "Activity";
        if (should_read)
            read();
    }

    public List<CCQuestion> getQuestions() { return questions; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public void addQuestion(CCQuestion question) {
        questions.add(question);
    }


    /************ I/O *******************/

    public static ActivityFile open(String path) { return open(path, true); }
    public static ActivityFile open(Path path)   { return open(path, true); }
    public static ActivityFile open(String path, boolean should_read) {
        return open(Paths.get(path), should_read);
    }
    public static ActivityFile open(Path path, boolean should_read) {
        CCFile f = getCached(path);
        if (f == null) return new ActivityFile(path, should_read);
        assert f instanceof ActivityFile;
        return (ActivityFile) f;
    }

    @Override
    protected void read() {
        if (path == null) return;

        try {
            List<Object> retrievedData = CrypticWriter.readEncryptedData(path);
            if (retrievedData.size() == 2) {
                int i = 0;
                questions = (ArrayList<CCQuestion>) retrievedData.get(i++);
                title = (String) retrievedData.get(i++);
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
        data.add(questions);
        data.add(title);
        try {
            CrypticWriter.writeEncryptedData(data, path);
        } catch (Exception e) {
            System.err.println("TestcaseFile.writeOut() error writing " + path);
            e.printStackTrace();
        }
    }


}
