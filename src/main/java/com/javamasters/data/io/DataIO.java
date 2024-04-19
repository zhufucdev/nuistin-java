package com.javamasters.data.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DataIO {
    InputStream openInputStream() throws IOException;
    OutputStream openOutputStream() throws IOException;
    boolean created();
    boolean available();
}
