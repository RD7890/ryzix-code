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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import openjdk.tools.javac.main.Main.Result;
import openjdk.tools.sjavac.AutoFlushWriter;
import openjdk.tools.sjavac.Log;
import openjdk.tools.sjavac.Util;
import openjdk.tools.sjavac.comp.PooledSjavac;
import openjdk.tools.sjavac.comp.SjavacImpl;

/**
 * Server-mode entry point for sjavac. Listens on a socket and delegates
 * compilation requests to a pooled SjavacImpl.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b></p>
 */
public class ServerMain {

    public static int run(String[] args) {
        Log.setLogForCurrentThread(new Log(
                new AutoFlushWriter(new OutputStreamWriter(System.out)),
                new AutoFlushWriter(new OutputStreamWriter(System.err))));

        String startServerArg = null;
        for (String a : args) {
            if (a.startsWith("--startserver:")) {
                startServerArg = a;
                break;
            }
        }

        if (startServerArg == null) {
            Log.error("Missing --startserver: argument");
            return Result.CMDERR.exitCode;
        }

        String conf = startServerArg.substring("--startserver:".length());
        String portfileName = Util.extractStringOption("portfile", conf, "");
        int poolsize = Integer.parseInt(Util.extractStringOption("poolsize", conf,
                String.valueOf(Runtime.getRuntime().availableProcessors())));
        int keepalive = Integer.parseInt(Util.extractStringOption("keepalive", conf, "120"));

        if (portfileName.isEmpty()) {
            Log.error("portfile option is missing from --startserver argument");
            return Result.CMDERR.exitCode;
        }

        ServerSocket serverSocket = null;
        try {
            PortFile portFile = SjavacServer.getPortFile(portfileName);
            Sjavac sjavac = new PooledSjavac(new SjavacImpl(), poolsize);

            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName(null), 0));
            int port = serverSocket.getLocalPort();

            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(portfileName, "rw");
            raf.setLength(0);
            raf.writeBytes(String.valueOf(port) + "\n");
            raf.close();

            Log.debug("Sjavac server listening on port " + port);

            long lastActivity = System.currentTimeMillis();
            serverSocket.setSoTimeout(1000);

            ExecutorService executor = Executors.newFixedThreadPool(poolsize);

            while ((System.currentTimeMillis() - lastActivity) < keepalive * 1000L) {
                try {
                    Socket socket = serverSocket.accept();
                    lastActivity = System.currentTimeMillis();
                    final Socket s = socket;
                    executor.submit(new Runnable() {
                        public void run() {
                            handleRequest(s, sjavac);
                        }
                    });
                } catch (java.net.SocketTimeoutException e) {
                    // normal timeout, loop again
                }
            }

            executor.shutdown();
            sjavac.shutdown();
        } catch (IOException e) {
            Log.error("Server error: " + e.getMessage());
            return Result.ERROR.exitCode;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return Result.OK.exitCode;
    }

    private static void handleRequest(Socket socket, Sjavac sjavac) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            int argCount = Integer.parseInt(in.readLine().trim());
            String[] args = new String[argCount];
            for (int i = 0; i < argCount; i++) {
                args[i] = in.readLine();
            }

            Result result = sjavac.compile(args);
            out.println(SjavacServer.LINE_TYPE_RC + ":" + result.name());
        } catch (Exception e) {
            Log.error("Error handling client request: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
