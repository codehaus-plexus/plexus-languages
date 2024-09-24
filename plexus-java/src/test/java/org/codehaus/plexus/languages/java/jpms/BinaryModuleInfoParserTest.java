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
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaExports;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaProvides;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BinaryModuleInfoParserTest {
    private final BinaryModuleInfoParser parser = new BinaryModuleInfoParser();

    @Test
    void testJarDescriptor() throws Exception {
        JavaModuleDescriptor descriptor =
                parser.getModuleDescriptor(Paths.get("src/test/test-data/jar.descriptor/asm-6.0_BETA.jar"));

        assertNotNull(descriptor);
        assertThat(descriptor.name()).isEqualTo("org.objectweb.asm");
        assertFalse(descriptor.isAutomatic());

        assertThat(descriptor.requires()).hasSize(1);
        assertEquals("java.base", descriptor.requires().iterator().next().name());

        Set<JavaExports> expectedExports = JavaModuleDescriptor.newAutomaticModule("_")
                .exports("org.objectweb.asm")
                .exports("org.objectweb.asm.signature")
                .build()
                .exports();
        assertEquals(expectedExports, descriptor.exports());
    }

    @Test
    void testMultiReleaseJarDescriptor() throws Exception {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor(
                Paths.get("src/test/test-data/jar.mr.descriptor/jloadr-1.0-SNAPSHOT.jar"), JavaVersion.parse("17"));

        assertNotNull(descriptor);
        assertEquals("de.adito.jloadr", descriptor.name());
        assertFalse(descriptor.isAutomatic());
    }

    @Test
    void testIncompleteMultiReleaseJarDescriptor() throws Exception {
        // this jar is missing the Multi-Release: true entry in the Manifest
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor(
                Paths.get("src/test/test-data/jar.mr.incomplete.descriptor/jloadr-1.0-SNAPSHOT.jar"));

        assertNull(descriptor);
    }

    @Test
    void testClassicJar() throws Exception {
        JavaModuleDescriptor descriptor =
                parser.getModuleDescriptor(Paths.get("src/test/test-data/jar.empty/plexus-java-1.0.0-SNAPSHOT.jar"));

        assertNull(descriptor);
    }

    @Test
    void testOutputDirectoryDescriptor() throws Exception {
        JavaModuleDescriptor descriptor =
                parser.getModuleDescriptor(Paths.get("src/test/test-data/dir.descriptor/out"));

        assertNotNull(descriptor);
        assertEquals("org.codehaus.plexus.languages.java.demo", descriptor.name());
        assertFalse(descriptor.isAutomatic());

        assertThat(descriptor.requires()).hasSize(3);

        Set<JavaRequires> expectedRequires = JavaModuleDescriptor.newAutomaticModule("_")
                .requires("java.base")
                .requires("java.xml")
                .requires(Collections.singleton(JavaRequires.JavaModifier.STATIC), "com.google.common")
                .build()
                .requires();

        assertEquals(expectedRequires, descriptor.requires());
    }

    @Test
    void testClassicOutputDirectory() {
        assertThrows(
                NoSuchFileException.class,
                () -> parser.getModuleDescriptor(Paths.get("src/test/test-data/dir.empty/out")));
    }

    @Test
    void testJModDescriptor() throws Exception {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor(
                Paths.get("src/test/test-data/jmod.descriptor/first-jmod-1.0-SNAPSHOT.jmod"));

        assertNotNull(descriptor);
        assertEquals("com.corporate.project", descriptor.name());
        assertFalse(descriptor.isAutomatic());

        assertEquals(1, descriptor.requires().size());
        assertEquals("java.base", descriptor.requires().iterator().next().name());

        assertEquals(1, descriptor.exports().size());
        assertEquals(
                "com.corporate.project", descriptor.exports().iterator().next().source());
    }

    @Test
    void testInvalidFile() {
        assertThrows(
                IOException.class, () -> parser.getModuleDescriptor(Paths.get("src/test/test-data/nonjar/pom.xml")));
    }

    @Test
    void testUses() throws Exception {
        try (InputStream is =
                Files.newInputStream(Paths.get("src/test/test-data/dir.descriptor.uses/out/module-info.class"))) {
            JavaModuleDescriptor descriptor = parser.parse(is);

            assertNotNull(descriptor);
            assertEquals(
                    new HashSet<>(Arrays.asList(
                            "org.apache.logging.log4j.spi.Provider",
                            "org.apache.logging.log4j.util.PropertySource",
                            "org.apache.logging.log4j.message.ThreadDumpMessage$ThreadInfoFactory")),
                    descriptor.uses());
        }
    }

    @Test
    void testProvides() throws Exception {
        JavaModuleDescriptor descriptor =
                parser.getModuleDescriptor(Paths.get("src/test/test-data/jar.service/threeten-extra-1.4.jar"));

        assertNotNull(descriptor);
        assertEquals(1, descriptor.provides().size());

        JavaProvides provides = descriptor.provides().iterator().next();
        assertEquals("java.time.chrono.Chronology", provides.service());
        assertArrayEquals(
                new String[] {
                    "org.threeten.extra.chrono.BritishCutoverChronology",
                    "org.threeten.extra.chrono.CopticChronology",
                    "org.threeten.extra.chrono.DiscordianChronology",
                    "org.threeten.extra.chrono.EthiopicChronology",
                    "org.threeten.extra.chrono.InternationalFixedChronology",
                    "org.threeten.extra.chrono.JulianChronology",
                    "org.threeten.extra.chrono.PaxChronology",
                    "org.threeten.extra.chrono.Symmetry010Chronology",
                    "org.threeten.extra.chrono.Symmetry454Chronology"
                },
                provides.providers().toArray(new String[0]));
    }

    @Test
    void testRequires() throws Exception {
        try (InputStream is =
                Files.newInputStream(Paths.get("src/test/test-data/dir.descriptor.requires/out/module-info.class"))) {
            JavaModuleDescriptor descriptor = parser.parse(is);

            assertNotNull(descriptor);
            assertThat(descriptor.requires()).hasSize(5);

            Set<JavaRequires> expectedRequires = JavaModuleDescriptor.newAutomaticModule("_")
                    .requires("java.base")
                    .requires("mod_r")
                    .requires(Collections.singleton(JavaRequires.JavaModifier.STATIC), "mod_r_s")
                    .requires(Collections.singleton(JavaRequires.JavaModifier.TRANSITIVE), "mod_r_t")
                    .requires(
                            new HashSet<>(Arrays.asList(
                                    JavaRequires.JavaModifier.STATIC, JavaRequires.JavaModifier.TRANSITIVE)),
                            "mod_r_s_t")
                    .build()
                    .requires();

            assertEquals(expectedRequires, descriptor.requires());
        }
    }
}
