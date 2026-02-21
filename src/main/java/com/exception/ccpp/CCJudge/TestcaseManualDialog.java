package com.exception.ccpp.CCJudge;

import com.exception.ccpp.GUI.UpdateGUICallback;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestcaseManualDialog {
    public static final String COLOR_FOREGROUND = "#D4D4D4";
    public static final String COLOR_BACKGROUND = "#1E1E1E";
    public static final String COLOR_CURRENTLINE = "#2A2A2A";
    public static final String COLOR_SELECTED = "#264F78";
    private static JDialog dialog;

//    public static class Result {
//        public final String inputsText;
//        public final String expectedText;
//
//        public Result(String inputsText, String expectedText) {
//            this.inputsText = inputsText;
//            this.expectedText = expectedText;
//        }
//    }

    private static RSyntaxTextArea createTextArea()
    {
        RSyntaxTextArea textArea =  new RSyntaxTextArea(8, 40);;
        textArea.setForeground(Color.decode(COLOR_FOREGROUND));
        textArea.setBackground(Color.decode(COLOR_BACKGROUND));
        textArea.setCurrentLineHighlightColor(Color.decode(COLOR_CURRENTLINE));
        textArea.setSelectedTextColor(Color.decode(COLOR_SELECTED));
        textArea.setAntiAliasingEnabled(true);
        textArea.setFractionalFontMetricsEnabled(false);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        return textArea;
    }

    public static void show(QuestionFile tf, UpdateGUICallback gui_cb) {
        if (dialog != null && dialog.isDisplayable()) {
            dialog.requestFocus();
            return;
        };
        dialog = new JDialog((Window)null, "Test Case Editor");
        dialog.setModalityType(Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        // Text areas
        RSyntaxTextArea inputsArea = createTextArea();
        RSyntaxTextArea expectedArea = createTextArea();

        // Scroll panes
        RTextScrollPane inputsScroll = new RTextScrollPane(inputsArea);
        RTextScrollPane expectedScroll = new RTextScrollPane(expectedArea);


        JToolBar toolBar = new JToolBar();
        JTextField searchField = new PlaceholderTextField("Separate with | (Vertical Bar)",30);
        toolBar.add(searchField);
        final JButton magicBtn = new JButton("Do Magic");
        magicBtn.setActionCommand("Get inputs");
        toolBar.add(magicBtn);


        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        center.add(toolBar, gc);

        // Labels
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.gridy = 1;
        center.add(new JLabel("Inputs (newline separated):"), gc);
        gc.gridy = 3;
        center.add(new JLabel("Expected Output:"), gc);

        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1;
        gc.gridy = 2;
        center.add(inputsScroll, gc);
        gc.gridy = 4;
        center.add(expectedScroll, gc);

        dialog.add(center, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel();
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        buttons.add(ok);
        buttons.add(cancel);

        dialog.add(buttons, BorderLayout.SOUTH);

        // Result container
        ok.addActionListener(e -> {
            tf.manualAddTestcase(
                new Testcase(inputsArea.getText().trim().split("\\R"), expectedArea.getText().trim()),
                gui_cb
            );
            inputsArea.setText("");
            expectedArea.setText("");
        });

        cancel.addActionListener(e -> {
            dialog.dispose();
            dialog = null;
        });

        magicBtn.addActionListener(e -> {
            inputsArea.setText(
                getInputs(searchField.getText(),inputsArea.getText())
            );
        });

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }


    public static String getInputs(String prompt, String str)
    {
        String reg = "(?m)^(" + prompt + ")\\s*(.*)$";
        Pattern p = Pattern.compile(reg);
        Matcher m = p.matcher(str);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            out.append(m.group(2)).append("\n");
        }
        return out.toString();
    }

    public static class PlaceholderTextField extends JTextField {
        private String placeholder;

        public PlaceholderTextField(String placeholder, int column) {
            super(column);
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.GRAY);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                Insets insets = getInsets();
                g2.drawString(placeholder, insets.left + 2,
                        getHeight() - insets.bottom - 4);
                g2.dispose();
            }
        }
    }


    public static void main(String[] args) {
        String input = "Op: Q 15\n" +
                "15 added as root\n" +
                "Op: R 9 15\n" +
                "9 added as right of 15\n" +
                "Op: L 2 15\n" +
                "2 added as left of 15\n" +
                "Op: p\n" +
                "Size: 3\n" +
                "+--R: 15\n" +
                "|   +--L: 2\n" +
                "|   +--R: 9\n" +
                "Status: 1\n" +
                "Op: s 2\n" +
                "The sibling of 2 is 9\n" +
                "Op: s 9\n" +
                "The sibling of 9 is 2\n" +
                "Op: x\n" +
                "Exiting\n";

        String prompt = "Op:|Status:";
        System.out.println("----------");
        System.out.println(getInputs(prompt, input));
    }
}

