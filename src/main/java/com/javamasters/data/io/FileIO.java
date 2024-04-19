package com.javamasters.data.io;

import java.io.*;

public class FileIO implements DataIO {
    private File file;

    public FileIO(File file) {
        this.file = file;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return new FileOutputStream(file);
    }

    @Override
    public boolean created() {
        return file.exists();
    }

    @Override
    public boolean available() {
        return file.getParentFile().exists();
    }
}
