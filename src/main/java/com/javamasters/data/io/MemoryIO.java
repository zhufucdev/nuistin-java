package com.javamasters.data.io;

import java.io.*;
import java.util.function.Consumer;

public class MemoryIO implements DataIO {
    private byte[] buffer;
    private final int chunk;
    private boolean created = false, available = true;

    public MemoryIO() {
        this(0);
    }

    public MemoryIO(int initialCapacity) {
        this.chunk = initialCapacity;
    }

    @Override
    public InputStream openInputStream() {
        return new ByteArrayInputStream(buffer);
    }

    @Override
    public OutputStream openOutputStream() {
        available = false;
        created = true;
        return new MemoryOutputStream(chunk)
                .onFlush(b -> buffer = b)
                .onClose(() -> available = true);
    }

    static class MemoryOutputStream extends ByteArrayOutputStream {
        private Consumer<byte[]> updater = null;
        private Runnable closer = null;
        public MemoryOutputStream(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        public void flush() throws IOException {
            super.flush();
            if (updater != null) {
                updater.accept(toByteArray());
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            updater.accept(toByteArray());
            closer.run();
        }

        public MemoryOutputStream onFlush(Consumer<byte[]> updater) {
            this.updater = updater;
            return this;
        }

        public MemoryOutputStream onClose(Runnable closer) {
            this.closer = closer;
            return this;
        }
    }

    @Override
    public boolean created() {
        return created;
    }

    @Override
    public boolean available() {
        return available;
    }
}
