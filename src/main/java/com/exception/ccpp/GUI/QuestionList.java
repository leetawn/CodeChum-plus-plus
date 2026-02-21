package com.exception.ccpp.GUI;

import com.exception.ccpp.CCJudge.CCQuestion;
import com.exception.ccpp.CCJudge.Testcase;
import com.exception.ccpp.CCJudge.TestcaseButton;
import com.exception.ccpp.CCJudge.TestcasesPanel;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionList extends JPanel {
    private Map<CCQuestion, JButton> activeButtons = new LinkedHashMap<>();

    public QuestionList() {
        super();
        this.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));
//        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        setBackground(Color.decode("#191c2a"));
        revalidate();
    }

    public void setQuestions(List<CCQuestion> question)
    {
        clear();
        addQuestions(question);
    }

    private void addQuestion(CCQuestion q)
    {
        JButton btn = new JButton(String.format("%d", activeButtons.size() + 1));
        btn.setFont(new Font(null, Font.BOLD, 18));

        btn.setBackground(Color.white);
        btn.setForeground(Color.black);

        btn.addActionListener((event)-> {
            TextEditor.getInstance().setQuestion(q);
        });
        this.add(btn);
        activeButtons.put(q, btn);

        revalidate();
    }

    private void addQuestions(List<CCQuestion> question)
    {
        for (CCQuestion q : question)
        {
            addQuestion(q);
        }
    }

    public void clear() {
        for (Map.Entry<CCQuestion, JButton> e : activeButtons.entrySet()) {
            this.remove(e.getValue());
        }
        activeButtons.clear();
    }

    public void removeQuestion(CCQuestion ccq) {
        JButton btn = activeButtons.get(ccq);
        if (activeButtons.remove(ccq) != null)
        {
            this.remove(btn);
        }
    }
}
