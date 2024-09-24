package org.codehaus.plexus.languages.java.version;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

/**
 * Reads the bytecode of a Java class to detect the major, minor and Java
 * version that was compiled.
 *
 * @author Jorge Solórzano
 */
public final class JavaClassfileVersion {

    private final int major;
    private final int minor;

    JavaClassfileVersion(int major, int minor) {
        if (major < 45) {
            throw new IllegalArgumentException("Java class major version must be 45 or above.");
        }
        this.major = major;
        this.minor = minor;
    }

    /**
     * Reads the bytecode of a Java class file and returns the
     * {@link JavaClassfileVersion}.
     *
     * @param bytes {@code byte[]} of the Java class file
     * @return the {@link JavaClassfileVersion} of the byte array
     */
    public static JavaClassfileVersion of(byte[] bytes) {
        return JavaClassfileVersionParser.of(bytes);
    }

    /**
     * Reads the bytecode of a Java class file and returns the
     * {@link JavaClassfileVersion}.
     *
     * @param path {@link Path} of the Java class file
     * @return the {@link JavaClassfileVersion} of the path java class
     */
    public static JavaClassfileVersion of(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] bytes = new byte[8];
            int total = 0;
            while (total < 8) {
                int l = is.read(bytes, total, 8 - total);
                if (l > 0) {
                    total += l;
                }
                if (l == -1) {
                    break;
                }
            }
            return of(bytes);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * JavaVersion of the class file version detected.
     *
     * @return JavaVersion based on the major version of the class file.
     */
    public JavaVersion javaVersion() {
        int javaVer = major - 44;
        String javaVersion = javaVer < 9 ? "1." + javaVer : Integer.toString(javaVer);

        return JavaVersion.parse(javaVersion);
    }

    /**
     * Returns the major version of the parsed classfile.
     *
     * @return the major classfile version
     */
    public int majorVersion() {
        return major;
    }

    /**
     * Returns the minor version of the parsed classfile.
     *
     * @return the minor classfile version
     */
    public int minorVersion() {
        return minor;
    }

    /**
     * Returns if the classfile use preview features.
     *
     * @return {@code true} if the classfile use preview features.
     */
    public boolean isPreview() {
        return minor == 65535;
    }

    /**
     * Returns a String representation of the Java class file version, e.g.
     * {@code 65.0 (Java 21)}.
     *
     * @return String representation of the Java class file version
     */
    @Override
    public String toString() {
        return major + "." + minor + " (Java " + javaVersion() + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + minor;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JavaClassfileVersion)) return false;
        JavaClassfileVersion other = (JavaClassfileVersion) obj;
        if (major != other.major) return false;
        if (minor != other.minor) return false;
        return true;
    }
}
