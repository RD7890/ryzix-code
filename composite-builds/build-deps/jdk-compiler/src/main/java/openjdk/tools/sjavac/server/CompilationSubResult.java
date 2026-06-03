/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */

package openjdk.tools.sjavac.server;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import openjdk.tools.javac.main.Main.Result;
import openjdk.tools.sjavac.pubapi.PubApi;

/**
 * Represents the result of a single compilation sub-task within sjavac.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b></p>
 */
public class CompilationSubResult {

    public Result result;
    public String stdout = "";
    public String stderr = "";

    public Map<String, Set<URI>> packageArtifacts = new HashMap<>();
    public Map<String, Map<String, Set<String>>> packageDependencies = new HashMap<>();
    public Map<String, Map<String, Set<String>>> packageCpDependencies = new HashMap<>();
    public Map<String, PubApi> packagePubapis = new HashMap<>();
    public Map<String, PubApi> dependencyPubapis = new HashMap<>();

    public CompilationSubResult(Result result) {
        this.result = result;
    }
}
