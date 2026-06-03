/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

package openjdk.tools.sjavac.server;

import openjdk.tools.javac.main.Main.Result;

/**
 * The sjavac tool can be run in server mode or in client mode.
 * This interface represents the core compile functionality of an sjavac instance.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b></p>
 */
public interface Sjavac {
    /**
     * Compile sources according to the given args.
     */
    Result compile(String[] args);

    /**
     * Shutdown the sjavac tool.
     */
    void shutdown();
}
