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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AsmModuleInfoParserTest {
    private final AsmModuleInfoParser parser = new AsmModuleInfoParser();

    /**
     * A class file major version newer than what the bundled ASM release supports (e.g. major
     * version 72 emitted by JDK 28, see codehaus-plexus/plexus-languages#165) used to make
     * ClassReader throw IllegalArgumentException. Patch a known-good module-info.class to claim
     * that unsupported major version and verify it still parses, with the same descriptor as the
     * unpatched file (see {@code BinaryModuleInfoParserTest#requires()}).
     */
    @Test
    void parsesModuleInfoWithMajorVersionNewerThanAsmSupports() throws Exception {
        byte[] classBytes =
                Files.readAllBytes(Paths.get("src/test/test-data/dir.descriptor.requires/out/module-info.class"));

        // bytes 6-7 are the big-endian major version; 72 is one past ASM 9.10.1's newest (71/V27)
        classBytes[6] = 0x00;
        classBytes[7] = 0x48;

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
}
