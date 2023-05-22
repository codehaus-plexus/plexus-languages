package org.codehaus.plexus.languages.java.jpms;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Robert Scholte
 * @since 1.0.0
 */
public abstract class ResolvePathRequest<T> {
    private Path jdkHome;

    private T path;

    private ResolvePathRequest() {}

    public static ResolvePathRequest<File> ofFile(File file) {
        ResolvePathRequest<File> request = new ResolvePathRequest<File>() {
            @Override
            protected Path toPath(File f) {
                return f.toPath();
            }
        };
        request.path = file;
        return request;
    }

    public static ResolvePathRequest<Path> ofPath(Path path) {
        ResolvePathRequest<Path> request = new ResolvePathRequest<Path>() {
            @Override
            protected Path toPath(Path p) {
                return p;
            }
        };
        request.path = path;
        return request;
    }

    public static ResolvePathRequest<String> ofString(String string) {
        ResolvePathRequest<String> request = new ResolvePathRequest<String>() {
            @Override
            protected Path toPath(String s) {
                return Paths.get(s);
            }
        };
        request.path = string;
        return request;
    }

    protected abstract Path toPath(T t);

    public T getPathElement() {
        return path;
    }

    /**
     * In case the JRE is Java 8 or before, this jdkHome is used to extract the module name.
     *
     * @param jdkHome
     * @return this request
     */
    public ResolvePathRequest<T> setJdkHome(T jdkHome) {
        this.jdkHome = toPath(jdkHome);
        return this;
    }

    public Path getJdkHome() {
        return jdkHome;
    }
}
