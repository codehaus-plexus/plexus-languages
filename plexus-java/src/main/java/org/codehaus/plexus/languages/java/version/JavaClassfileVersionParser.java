package org.codehaus.plexus.languages.java.version;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

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
 * This class is intented to be package-private and consumed by
 * {@link JavaClassfileVersion}.
 *
 * @author Jorge Solórzano
 */
final class JavaClassfileVersionParser {

    private JavaClassfileVersionParser() {}

    /**
     * Reads the bytecode of a Java class file and returns the {@link JavaClassfileVersion}.
     *
     * @param in {@code byte[]} of the Java class file
     * @return the {@link JavaClassfileVersion} of the input stream
     */
    public static JavaClassfileVersion of(byte[] bytes) {
        try (final DataInputStream data = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (0xCAFEBABE != data.readInt()) {
                throw new IOException("Invalid java class file header");
            }
            int minor = data.readUnsignedShort();
            int major = data.readUnsignedShort();
            return new JavaClassfileVersion(major, minor);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
