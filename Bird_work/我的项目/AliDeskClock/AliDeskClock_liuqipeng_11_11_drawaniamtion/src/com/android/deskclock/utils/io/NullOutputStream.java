package com.android.deskclock.utils.io;

import java.io.IOException;
import java.io.OutputStream;

public class NullOutputStream extends OutputStream {

    public static final NullOutputStream NULL_OUTPUT_STREAM = new NullOutputStream();

    public void write(byte[] b, int off, int len) {
        //to /dev/null
    }

    public void write(int b) {
        //to /dev/null
    }

    public void write(byte[] b) throws IOException {
        //to /dev/null
    }

}
