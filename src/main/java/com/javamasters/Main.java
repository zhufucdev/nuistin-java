package com.javamasters;

import com.javamasters.data.impl.LocalMachineLibrary;
import com.javamasters.view.MainWindow;

public class Main {
    public static void main(String[] args) {
        var library = new LocalMachineLibrary();
        var mainWindow = new MainWindow(library);
        mainWindow.setVisible(true);
    }
}