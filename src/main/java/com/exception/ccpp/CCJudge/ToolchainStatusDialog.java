package com.exception.ccpp.CCJudge;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ToolchainStatusDialog extends JDialog {

    public ToolchainStatusDialog(Frame owner) {
        super(owner, "Judge Component Status", true);
        setLayout(new BorderLayout(10, 10));
        setSize(600, 400);
        setLocationRelativeTo(owner);

        // Column Names
        String[] columns = {"Component", "Status", "Version",  "Path"};

        // Table Model
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(30);
        setupCustomRenderer(table);

        // Add Components to check
        checkComponent(model, "Docker", "docker");

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        add(footer, BorderLayout.SOUTH);

        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private String getRedistPath(String relativePath) {
        // Points to the app/redist folder created by your MSI
        String base = System.getProperty("user.dir");
        return new File(base, "app/redist/" + relativePath).getAbsolutePath();
    }

    private void checkComponent(DefaultTableModel model, String name, String command) {
        String status;
        String versionInfo;

        try {
            ProcessBuilder pb = new ProcessBuilder(command, "--version");
            if(name.contains("Python")) pb = new ProcessBuilder(command, "-V");

            Process p = pb.start();

            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                versionInfo = r.readLine();
                if (versionInfo == null) versionInfo = "Online";
            }
            status = "READY";
        } catch (Exception e) {
            status = "MISSING / ERROR";
            versionInfo = "Executable Not Found";
        }

        model.addRow(new Object[]{name, status, versionInfo, command});
    }

    private void setupCustomRenderer(JTable table) {
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isS, boolean hasF, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(t, value, isS, hasF, row, col);
                if ("READY".equals(value)) {
                    label.setForeground(new Color(0, 150, 0));
                    label.setText(" ● READY");
                } else {
                    label.setForeground(Color.RED);
                    label.setText(" * MISSING");
                }
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                return label;
            }
        });
    }

    // Demo
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> {
            new ToolchainStatusDialog(null).setVisible(true);
        });
    }
}