/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

package openjdk.tools.sjavac.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages a port file used for sjavac client-server communication.
 * The port file contains the port number that the sjavac server is listening on.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b></p>
 */
public class PortFile {

    private final String filename;
    private int port = 0;
    private RandomAccessFile raf;
    private FileChannel channel;
    private FileLock lock;

    public PortFile(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }

    public boolean exists() {
        return Files.exists(Paths.get(filename));
    }

    public void lock() throws IOException {
        raf = new RandomAccessFile(filename, "rw");
        channel = raf.getChannel();
        lock = channel.lock();
    }

    public void getValues() throws IOException {
        raf.seek(0);
        String line = raf.readLine();
        if (line != null) {
            try {
                port = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                port = 0;
            }
        }
    }

    public void unlock() throws IOException {
        if (lock != null) {
            lock.release();
            lock = null;
        }
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (raf != null) {
            raf.close();
            raf = null;
        }
    }

    public boolean containsPortInfo() {
        return port > 0;
    }

    public int getPort() {
        return port;
    }

    /**
     * Wait up to 60 seconds for valid port values to appear in the port file.
     */
    public void waitForValidValues() throws IOException {
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            if (exists()) {
                try {
                    lock();
                    getValues();
                    unlock();
                    if (containsPortInfo()) {
                        return;
                    }
                } catch (IOException e) {
                    // not ready yet
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for port file", e);
            }
        }
        throw new IOException("Timed out waiting for valid port file values in: " + filename);
    }
}
