/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

package openjdk.tools.sjavac.server;

import java.io.IOException;

/**
 * The SjavacServer provides protocol constants and factory helpers for
 * the sjavac client-server communication.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b></p>
 */
public class SjavacServer {

    /**
     * Line type prefix used in server responses to carry the return code.
     */
    public static final String LINE_TYPE_RC = "RC";

    /**
     * Create a PortFile instance for the given port file path.
     */
    public static PortFile getPortFile(String filename) throws IOException {
        return new PortFile(filename);
    }

    private SjavacServer() {
        // utility class
    }
}
