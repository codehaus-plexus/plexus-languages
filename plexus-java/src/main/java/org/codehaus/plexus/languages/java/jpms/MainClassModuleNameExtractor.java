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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Extract the module name by calling the main method with an external JVM
 *
 * @author Robert Scholte
 * @since 1.0.0
 */
public class MainClassModuleNameExtractor {
    private final Path jdkHome;

    public MainClassModuleNameExtractor(Path jdkHome) {
        this.jdkHome = jdkHome;
    }

    public <T> Map<T, String> extract(Map<T, Path> files) throws IOException {
        Path workDir = Files.createTempDirectory("plexus-java_jpms-");

        String classResourcePath = CmdModuleNameExtractor.class.getName().replace('.', '/') + ".class";

        try (InputStream is =
                MainClassModuleNameExtractor.class.getResourceAsStream("/META-INF/versions/9/" + classResourcePath)) {
            if (is == null) {
                return Collections.emptyMap();
            }
            Path target = workDir.resolve(classResourcePath);

            Files.createDirectories(target.getParent());

            Files.copy(is, target);
        }

        try (BufferedWriter argsWriter = Files.newBufferedWriter(workDir.resolve("args"), Charset.defaultCharset())) {
            argsWriter.append("--class-path");
            argsWriter.newLine();

            argsWriter.append(".");
            argsWriter.newLine();

            argsWriter.append(CmdModuleNameExtractor.class.getName());
            argsWriter.newLine();

            for (Path p : files.values()) {
                // make sure the path is surrounded with quotes in case there is space
                argsWriter.append('"');
                // make sure to escape Windows paths
                argsWriter.append(p.toAbsolutePath().toString().replace("\\", "\\\\"));
                argsWriter.append('"');
                argsWriter.newLine();
            }
        }

        ProcessBuilder builder = new ProcessBuilder(
                        jdkHome.resolve("bin/java").toAbsolutePath().toString(), "@args")
                .directory(workDir.toFile());

        Process p = builder.start();

        Properties output = new Properties();
        try (InputStream is = p.getInputStream()) {
            output.load(is);
        }

        Map<T, String> moduleNames = new HashMap<>(files.size());
        for (Map.Entry<T, Path> entry : files.entrySet()) {
            moduleNames.put(
                    entry.getKey(),
                    output.getProperty(entry.getValue().toAbsolutePath().toString(), null));
        }

        try {
            Files.walkFileTree(workDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // noop, we did our best to clean it up
        }

        return moduleNames;
    }
}
