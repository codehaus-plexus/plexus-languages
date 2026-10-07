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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires;
import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Exercises the {@code java.lang.classfile}-based module descriptor parser added under
 * {@code src/main/java24} to prototype "option C" for
 * <a href="https://github.com/codehaus-plexus/plexus-languages/issues/165">#165</a>.
 *
 * <p>This class deliberately never mentions a {@code java.lang.classfile} type: it is compiled at
 * this module's Java 8 baseline like every other test, and it only proves anything once run
 * against the packaged multi-release jar. Directory classpaths (the {@code test} phase) are never
 * multi-release aware, so {@code new BinaryModuleInfoParser()} there always resolves to whichever
 * variant this module's own compilation produced last, regardless of the running JDK; only the
 * jar's {@code Multi-Release: true} manifest entry, honoured by {@code JarFile}/{@code
 * URLClassLoader} once failsafe reruns the suite against {@code target/*.jar} (see this module's
 * pom.xml and {@link LocationManagerIT}), makes the JVM pick the {@code META-INF/versions/24}
 * class on a Java 24+ runtime.
 */
@EnabledForJreRange(min = JRE.JAVA_24, disabledReason = "src/main/java24 is only in the jar's MR layer on a 24+ build")
class ClassFileModuleInfoParserIT {

    private final LocationManager locationManager = new LocationManager();

    private final Path moduleInfo = Paths.get("src/test/test-data/dir.descriptor.requires/out/module-info.class");

    @Test
    void parsesAnOrdinaryModuleDescriptor() throws Exception {
        JavaModuleDescriptor descriptor =
                locationManager.getBinaryModuleInfoParser(null).getModuleDescriptor(moduleInfo.getParent());

        assertNotNull(descriptor);
        assertThat(descriptor.requires()).hasSize(5);
        assertEquals(expectedRequires(), descriptor.requires());
    }

    @Test
    void clampsAModuleDescriptorNewerThanTheRuntimeSupports() throws Exception {
        byte[] classBytes = Files.readAllBytes(moduleInfo);

        // Bytes 6-7 are the big-endian class file major version. This JDK's classfile major
        // version is its feature version + 44 (Java 9 -> 53, ..., Java 24 -> 68, see
        // java.lang.classfile.ClassFile.JAVA_9_VERSION..JAVA_24_VERSION); one past that is a
        // version no released JDK can parse yet, e.g. a toolchain one release ahead of this one.
        int unsupportedMajorVersion = JavaVersion.JAVA_SPECIFICATION_VERSION.getMajorVersion() + 44 + 1;
        classBytes[6] = (byte) (unsupportedMajorVersion >>> 8);
        classBytes[7] = (byte) unsupportedMajorVersion;

        ModuleInfoParser parser = locationManager.getBinaryModuleInfoParser(null);
        JavaModuleDescriptor descriptor;
        try (ByteArrayInputStream in = new ByteArrayInputStream(classBytes)) {
            descriptor = ((AbstractBinaryModuleInfoParser) parser).parse(in);
        }

        assertNotNull(descriptor);
        assertThat(descriptor.requires()).hasSize(5);
        assertEquals(expectedRequires(), descriptor.requires());
    }

    @Test
    void isPreferredOverAsmForAToolchainOnA24PlusRuntime() {
        // #165: a toolchain (jdkHome) used to always fall back to AsmModuleInfoParser, on the
        // assumption that only ASM could tolerate a class file version newer than this process
        // understands. The java.lang.classfile parser clamps just like ASM does (see
        // codehaus-plexus/plexus-languages#239), so on a 24+ runtime it should be used here too.
        ModuleInfoParser parser = locationManager.getBinaryModuleInfoParser(Paths.get("some/jdk/home"));

        assertThat(parser).isNotInstanceOf(AsmModuleInfoParser.class);
    }

    private static Set<JavaRequires> expectedRequires() {
        return JavaModuleDescriptor.newAutomaticModule("_")
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
    }
}
