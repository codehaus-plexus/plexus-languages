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

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleInfoClassParserTest {
    private final ModuleInfoClassParser parser = new ModuleInfoClassParser();

    /**
     * This parser never looks at the class file's major/minor version, so a module-info.class compiled
     * by a JDK newer than any released today (e.g. major version 72 for JDK 28, see
     * codehaus-plexus/plexus-languages#165, or an arbitrarily higher one) must still parse, with the same
     * descriptor as the unpatched file (see {@code BinaryModuleInfoParserTest#requires()}).
     */
    @ParameterizedTest
    @ValueSource(ints = {72, 99})
    void parsesModuleInfoWithMajorVersionNewerThanAnyKnownJdk(int majorVersion) throws Exception {
        byte[] classBytes =
                Files.readAllBytes(Paths.get("src/test/test-data/dir.descriptor.requires/out/module-info.class"));

        // bytes 6-7 are the big-endian major version
        classBytes[6] = (byte) (majorVersion >>> 8);
        classBytes[7] = (byte) majorVersion;

        JavaModuleDescriptor descriptor;
        try (ByteArrayInputStream is = new ByteArrayInputStream(classBytes)) {
            descriptor = parser.parse(is);
        }

        assertNotNull(descriptor);
        assertThat(descriptor.requires()).hasSize(5);

        Set<JavaRequires> expectedRequires = JavaModuleDescriptor.newAutomaticModule("_")
                .requires("java.base")
                .requires("mod_r")
                .requires(Collections.singleton(JavaRequires.JavaModifier.STATIC), "mod_r_s")
                .requires(Collections.singleton(JavaRequires.JavaModifier.TRANSITIVE), "mod_r_t")
                .requires(
                        new HashSet<>(
                                Arrays.asList(JavaRequires.JavaModifier.STATIC, JavaRequires.JavaModifier.TRANSITIVE)),
                        "mod_r_s_t")
                .build()
                .requires();

        assertEquals(expectedRequires, descriptor.requires());
    }

    @Test
    void rejectsBadMagic() throws Exception {
        byte[] classBytes =
                Files.readAllBytes(Paths.get("src/test/test-data/dir.descriptor.requires/out/module-info.class"));
        classBytes[0] = 0x00;

        try (ByteArrayInputStream is = new ByteArrayInputStream(classBytes)) {
            assertThrows(java.io.IOException.class, () -> parser.parse(is));
        }
    }

    @Test
    void rejectsClassFileWithoutModuleAttribute() throws Exception {
        // an ordinary compiled class, i.e. one without a Module attribute, must be rejected rather than
        // silently producing a bogus descriptor
        byte[] classBytes = Files.readAllBytes(Paths.get("src/test/test-data/classfile.version/helloworld-17.class"));

        try (ByteArrayInputStream is = new ByteArrayInputStream(classBytes)) {
            assertThrows(java.io.IOException.class, () -> parser.parse(is));
        }
    }
}
