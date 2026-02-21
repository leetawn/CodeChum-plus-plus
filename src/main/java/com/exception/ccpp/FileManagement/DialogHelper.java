package com.exception.ccpp.FileManagement;

import com.formdev.flatlaf.util.SystemFileChooser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;

public class DialogHelper {
    public static SystemFileChooser createFileDialog(FileManager fm, String title) {
        SystemFileChooser fc = new SystemFileChooser();
        fc.setDialogTitle(title);
        fc.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        Path rootdir = fm.getRootdir();
        fc.setCurrentDirectory(rootdir.toFile());
        return fc;
    }


    public static File startDialog(SystemFileChooser fc, Component parent, String err_msg) {
        if( fc.showDialog( parent, "Open/Save" ) == SystemFileChooser.CANCEL_OPTION ) { return null; }
        File f = fc.getSelectedFile();
        if (f == null) showError(parent, err_msg, "Error");
        return f;
    }

    public static File startOpenDialog(SystemFileChooser fc, Component parent, String err_msg) {
        if( fc.showOpenDialog( parent ) == SystemFileChooser.CANCEL_OPTION ) { return null; }
        File f = fc.getSelectedFile();
        if (f == null) showError(parent, err_msg, "Error");
        return f;
    }

    public static File startSaveDialog(SystemFileChooser fc, Component parent, String err_msg)
    {
        if( fc.showSaveDialog( parent ) == SystemFileChooser.CANCEL_OPTION ) { return null; }
        File f = fc.getSelectedFile();
        if (f == null) showError(parent, err_msg, "Error");
        return f;
    }

    public static void showError(Component parent, String msg, String title)
    {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
    }
}
