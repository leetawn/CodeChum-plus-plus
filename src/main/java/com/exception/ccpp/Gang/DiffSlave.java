package com.exception.ccpp.Gang;

import com.exception.ccpp.CCJudge.SubmissionRecord;
import com.exception.ccpp.Common.Helpers;
import com.exception.ccpp.GUI.TextEditor;

import javax.swing.text.StyledDocument;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

import static com.exception.ccpp.Gang.SlaveManager.romanArmy;

public class DiffSlave implements Callable<Integer> {
    final String actual;
    final String expected;
    final StyledDocument actualDoc;
    final StyledDocument expectedDoc;

    public DiffSlave(SubmissionRecord rec, StyledDocument actualDoc, StyledDocument expectedDoc) {
        this.actual = rec.output();
        this.expected = rec.expected_output();
        this.actualDoc = actualDoc;
        this.expectedDoc = expectedDoc;
    }

    @Override
    public Integer call() throws Exception {
        String[] actualLines = romanArmy.invoke(new Helpers.SplitTask(actual, 0, actual.length()));
        String[] expectedLines = romanArmy.invoke(new Helpers.SplitTask(expected, 0, expected.length()));

        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.submit(() -> TextEditor.getInstance().displayExpectedDiff(actualLines, expectedLines, expectedDoc));
        int i = TextEditor.getInstance().displayActualDiff(actualLines, expectedLines, actualDoc);
        return i;
    }
}
