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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.codehaus.plexus.languages.java.version.JavaVersion;

abstract class AbstractBinaryModuleInfoParser implements ModuleInfoParser {
    @Override
    public JavaModuleDescriptor getModuleDescriptor(Path modulePath) throws IOException {
        return getModuleDescriptor(modulePath, JavaVersion.JAVA_SPECIFICATION_VERSION);
    }

    @Override
    public JavaModuleDescriptor getModuleDescriptor(Path modulePath, JavaVersion jdkVersion) throws IOException {
        JavaModuleDescriptor descriptor;
        if (Files.isDirectory(modulePath)) {
            Path moduleInfo = modulePath.resolve("module-info.class");
            if (!Files.exists(moduleInfo)) {
                Path versionedModuleInfo = findVersionedModuleInfo(modulePath, jdkVersion);
                if (versionedModuleInfo != null) {
                    moduleInfo = versionedModuleInfo;
                }
            }
            try (InputStream in = Files.newInputStream(moduleInfo)) {
                descriptor = parse(in);
            }
        } else {
            try (JarFile jarFile = new JarFile(modulePath.toFile())) {
                JarEntry moduleInfo;
                if (modulePath.toString().toLowerCase().endsWith(".jmod")) {
                    moduleInfo = jarFile.getJarEntry("classes/module-info.class");
                } else {
                    moduleInfo = jarFile.getJarEntry("module-info.class");

                    if (moduleInfo == null) {
                        Manifest manifest = jarFile.getManifest();

                        if (manifest != null
                                && "true"
                                        .equalsIgnoreCase(
                                                manifest.getMainAttributes().getValue("Multi-Release"))) {
                            int javaVersion = jdkVersion.getMajorVersion();

                            for (int version = javaVersion; version >= 9; version--) {
                                String resource = "META-INF/versions/" + version + "/module-info.class";
                                JarEntry entry = jarFile.getJarEntry(resource);
                                if (entry != null) {
                                    moduleInfo = entry;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (moduleInfo != null) {
                    descriptor = parse(jarFile.getInputStream(moduleInfo));
                } else {
                    descriptor = null;
                }
            }
        }
        return descriptor;
    }

    /**
     * Finds the module descriptor of a multi-release output directory, which has no {@code module-info.class} at its
     * root but under {@code META-INF/versions/<N>}, the way a multi-release jar does.
     *
     * @param directory the output directory
     * @param jdkVersion the highest version to consider
     * @return the path of the highest applicable versioned descriptor, or {@code null} if there is none
     */
    static Path findVersionedModuleInfo(Path directory, JavaVersion jdkVersion) {
        int javaVersion = jdkVersion.getMajorVersion();

        for (int version = javaVersion; version >= 9; version--) {
            Path moduleInfo = directory.resolve("META-INF/versions/" + version + "/module-info.class");
            if (Files.exists(moduleInfo)) {
                return moduleInfo;
            }
        }
        return null;
    }

    abstract JavaModuleDescriptor parse(InputStream in) throws IOException;

    /**
     * Whether this parser reads the {@code Module} attribute with the runtime's own
     * {@code java.lang.classfile} API (JEP 484, final since Java 24) rather than ASM or
     * {@code java.lang.module.ModuleDescriptor}.
     * <p>
     * {@link BinaryModuleInfoParser} is a multi-release class: depending on the JDK actually
     * running the build, {@code new BinaryModuleInfoParser()} resolves to the Java 8 (ASM-backed),
     * Java 9 ({@code java.lang.module}-backed) or Java 24 ({@code java.lang.classfile}-backed)
     * implementation. {@link LocationManager#getBinaryModuleInfoParser(java.nio.file.Path)} cannot
     * reference {@code java.lang.classfile} types directly &mdash; that package doesn't exist
     * before Java 24, and {@code LocationManager} is compiled once at the Java 8 baseline &mdash;
     * so it asks the resolved instance about its own capability instead.
     *
     * @return {@code true} for the {@code src/main/java24} implementation, {@code false} otherwise
     * @since 1.7.0
     */
    boolean isClassFileApiBased() {
        return false;
    }
}
