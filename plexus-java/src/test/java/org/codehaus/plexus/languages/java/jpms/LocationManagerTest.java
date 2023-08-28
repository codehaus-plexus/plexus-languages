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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationManagerTest {
    private BinaryModuleInfoParser asmParser;

    private SourceModuleInfoParser qdoxParser;

    private LocationManager locationManager;

    final Path mockModuleInfoJava = Paths.get("src/test/resources/mock/module-info.java");

    @BeforeEach
    void onSetup() {
        asmParser = mock(BinaryModuleInfoParser.class);
        qdoxParser = mock(SourceModuleInfoParser.class);
        locationManager = new LocationManager(qdoxParser) {
            @Override
            ModuleInfoParser getBinaryModuleInfoParser(Path jdkHome) {
                return asmParser;
            }
        };
    }

    @Test
    void testNoPaths() throws Exception {
        ResolvePathsResult<File> result =
                locationManager.resolvePaths(ResolvePathsRequest.ofFiles(Collections.emptyList()));
        assertThat(result.getMainModuleDescriptor()).isNull();
        assertThat(result.getPathElements()).hasSize(0);
        assertThat(result.getModulepathElements()).hasSize(0);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testWithUnknownRequires() throws Exception {
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule("base")
                .requires("java.base")
                .requires("jdk.net")
                .build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<File> request = ResolvePathsRequest.ofFiles(Collections.emptyList())
                .setMainModuleDescriptor(mockModuleInfoJava.toFile());

        ResolvePathsResult<File> result = locationManager.resolvePaths(request);

        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(0);
        assertThat(result.getModulepathElements()).hasSize(0);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testManifestWithReflectRequires() throws Exception {
        Path abc = Paths.get("src/test/resources/dir.manifest.with/out");
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule("base")
                .requires("auto.by.manifest")
                .build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(abc)).setMainModuleDescriptor(mockModuleInfoJava);

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getModulepathElements().get(abc)).isEqualTo(ModuleNameSource.MANIFEST);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testDirDescriptorWithReflectRequires() throws Exception {
        Path abc = Paths.get("src/test/resources/dir.descriptor/out");
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule("base")
                .requires("dir.descriptor")
                .build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(abc)).setMainModuleDescriptor(mockModuleInfoJava);

        when(asmParser.getModuleDescriptor(abc))
                .thenReturn(JavaModuleDescriptor.newModule("dir.descriptor").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getModulepathElements().get(abc)).isEqualTo(ModuleNameSource.MODULEDESCRIPTOR);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testJarWithAsmRequires() throws Exception {
        Path abc = Paths.get("src/test/resources/jar.descriptor/asm-6.0_BETA.jar");
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule("base")
                .requires("org.objectweb.asm")
                .build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(abc)).setMainModuleDescriptor(mockModuleInfoJava);

        when(asmParser.getModuleDescriptor(abc))
                .thenReturn(JavaModuleDescriptor.newModule("org.objectweb.asm").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getModulepathElements().get(abc)).isEqualTo(ModuleNameSource.MODULEDESCRIPTOR);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testIdenticalModuleNames() throws Exception {
        Path pj1 = Paths.get("src/test/resources/jar.empty/plexus-java-1.0.0-SNAPSHOT.jar");
        Path pj2 = Paths.get("src/test/resources/jar.empty.2/plexus-java-2.0.0-SNAPSHOT.jar");
        JavaModuleDescriptor descriptor =
                JavaModuleDescriptor.newModule("base").requires("plexus.java").build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Arrays.asList(pj1, pj2)).setMainModuleDescriptor(mockModuleInfoJava);

        when(asmParser.getModuleDescriptor(pj1))
                .thenReturn(
                        JavaModuleDescriptor.newAutomaticModule("plexus.java").build());
        when(asmParser.getModuleDescriptor(pj2))
                .thenReturn(
                        JavaModuleDescriptor.newAutomaticModule("plexus.java").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(2);
        assertThat(result.getModulepathElements()).containsOnlyKeys(pj1);
        assertThat(result.getModulepathElements()).doesNotContainKey(pj2);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testNonJar() throws Exception {
        Path p = Paths.get("src/test/resources/nonjar/pom.xml");

        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(p)).setMainModuleDescriptor(mockModuleInfoJava);

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathExceptions()).hasSize(1);
    }

    @Test
    void testAdditionalModules() throws Exception {
        Path p = Paths.get("src/test/resources/mock/jar0.jar");

        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule("base").build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths(Collections.singletonList(p))
                .setMainModuleDescriptor(mockModuleInfoJava)
                .setAdditionalModules(Collections.singletonList("plexus.java"));

        when(asmParser.getModuleDescriptor(p))
                .thenReturn(
                        JavaModuleDescriptor.newAutomaticModule("plexus.java").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testResolvePath() throws Exception {
        Path abc = Paths.get("src/test/resources/mock/jar0.jar");
        ResolvePathRequest<Path> request = ResolvePathRequest.ofPath(abc);

        when(asmParser.getModuleDescriptor(abc))
                .thenReturn(JavaModuleDescriptor.newModule("org.objectweb.asm").build());

        ResolvePathResult result = locationManager.resolvePath(request);

        assertThat(result.getModuleDescriptor())
                .isEqualTo(JavaModuleDescriptor.newModule("org.objectweb.asm").build());
        assertThat(result.getModuleNameSource()).isEqualTo(ModuleNameSource.MODULEDESCRIPTOR);
    }

    @Test
    void testNoMatchingProviders() throws Exception {
        Path abc = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path def = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(def).setMainModuleDescriptor(abc).setIncludeAllProviders(true);

        when(qdoxParser.fromSourcePath(abc))
                .thenReturn(JavaModuleDescriptor.newModule("abc").uses("device").build());
        when(asmParser.getModuleDescriptor(def))
                .thenReturn(JavaModuleDescriptor.newModule("def")
                        .provides("tool", Arrays.asList("java", "javac"))
                        .build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(0);
        assertThat(result.getClasspathElements()).hasSize(1);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testMainModuleDescriptorWithProviders() throws Exception {
        Path abc = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path def = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(def).setMainModuleDescriptor(abc).setIncludeAllProviders(true);

        when(qdoxParser.fromSourcePath(abc))
                .thenReturn(JavaModuleDescriptor.newModule("abc").uses("tool").build());
        when(asmParser.getModuleDescriptor(def))
                .thenReturn(JavaModuleDescriptor.newModule("def")
                        .provides("tool", Arrays.asList("java", "javac"))
                        .build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testMainModuleDescriptorWithProvidersDontIncludeProviders() throws Exception {
        Path abc = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path def = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths(def).setMainModuleDescriptor(abc);

        when(qdoxParser.fromSourcePath(abc))
                .thenReturn(JavaModuleDescriptor.newModule("abc").uses("tool").build());
        when(asmParser.getModuleDescriptor(def))
                .thenReturn(JavaModuleDescriptor.newModule("def")
                        .provides("tool", Arrays.asList("java", "javac"))
                        .build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(0);
        assertThat(result.getClasspathElements()).hasSize(1);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testTransitiveProviders() throws Exception {
        Path abc = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path def = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path ghi = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths(def, ghi)
                .setMainModuleDescriptor(abc)
                .setIncludeAllProviders(true);

        when(qdoxParser.fromSourcePath(abc))
                .thenReturn(
                        JavaModuleDescriptor.newModule("abc").requires("ghi").build());
        when(asmParser.getModuleDescriptor(def))
                .thenReturn(JavaModuleDescriptor.newModule("def")
                        .provides("tool", Arrays.asList("java", "javac"))
                        .build());
        when(asmParser.getModuleDescriptor(ghi))
                .thenReturn(JavaModuleDescriptor.newModule("ghi").uses("tool").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(2);
        assertThat(result.getModulepathElements()).hasSize(2);
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testDontIncludeProviders() throws Exception {
        Path abc = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path def = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path ghi = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(def, ghi).setMainModuleDescriptor(abc);

        when(qdoxParser.fromSourcePath(abc))
                .thenReturn(
                        JavaModuleDescriptor.newModule("abc").requires("ghi").build());
        when(asmParser.getModuleDescriptor(def))
                .thenReturn(JavaModuleDescriptor.newModule("def")
                        .provides("tool", Arrays.asList("java", "javac"))
                        .build());
        when(asmParser.getModuleDescriptor(ghi))
                .thenReturn(JavaModuleDescriptor.newModule("ghi").uses("tool").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(2);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getClasspathElements()).hasSize(1);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testAllowAdditionalModulesWithoutMainDescriptor() throws Exception {
        Path def = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path ghi = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(def, ghi).setAdditionalModules(Collections.singleton("def"));

        when(asmParser.getModuleDescriptor(def))
                .thenReturn(JavaModuleDescriptor.newModule("def").build());
        when(asmParser.getModuleDescriptor(ghi))
                .thenReturn(JavaModuleDescriptor.newModule("ghi").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(2);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getClasspathElements()).hasSize(1);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testReuseModuleDescriptor() throws Exception {
        Path def = Paths.get("src/test/resources/mock/jar0.jar");

        ResolvePathRequest<Path> request1 = ResolvePathRequest.ofPath(def);
        when(asmParser.getModuleDescriptor(def))
                .thenReturn(JavaModuleDescriptor.newModule("def").build());

        ResolvePathResult result1 = locationManager.resolvePath(request1);

        ResolvePathsRequest<Path> request2 = ResolvePathsRequest.ofPaths(def);
        request2.setModuleDescriptor(result1.getModuleDescriptor());

        ResolvePathsResult<Path> result2 = locationManager.resolvePaths(request2);

        assertThat(result1.getModuleDescriptor()).isEqualTo(result2.getMainModuleDescriptor());
    }

    @Test
    void testParseModuleDescriptor() throws Exception {
        Path descriptorPath = Paths.get("src/test/resources/src.dir/module-info.java");
        when(qdoxParser.fromSourcePath(descriptorPath))
                .thenReturn(JavaModuleDescriptor.newModule("a.b.c").build());

        ResolvePathResult result = locationManager.parseModuleDescriptor(descriptorPath);
        assertThat(result.getModuleNameSource()).isEqualTo(ModuleNameSource.MODULEDESCRIPTOR);
        assertThat(result.getModuleDescriptor().name()).isEqualTo("a.b.c");

        locationManager.parseModuleDescriptor(descriptorPath.toFile());
        assertThat(result.getModuleNameSource()).isEqualTo(ModuleNameSource.MODULEDESCRIPTOR);
        assertThat(result.getModuleDescriptor().name()).isEqualTo("a.b.c");

        locationManager.parseModuleDescriptor(descriptorPath.toString());
        assertThat(result.getModuleNameSource()).isEqualTo(ModuleNameSource.MODULEDESCRIPTOR);
        assertThat(result.getModuleDescriptor().name()).isEqualTo("a.b.c");
    }

    @Test
    void testTransitiveStatic() throws Exception {
        Path moduleA = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path moduleB = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path moduleC = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(moduleB, moduleC).setMainModuleDescriptor(moduleA);

        when(qdoxParser.fromSourcePath(moduleA))
                .thenReturn(JavaModuleDescriptor.newModule("moduleA")
                        .requires("moduleB")
                        .build());
        when(asmParser.getModuleDescriptor(moduleB))
                .thenReturn(JavaModuleDescriptor.newModule("moduleB")
                        .requires(Collections.singleton(JavaModifier.STATIC), "moduleC")
                        .build());
        when(asmParser.getModuleDescriptor(moduleC))
                .thenReturn(JavaModuleDescriptor.newModule("moduleC").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(2);
        assertThat(result.getModulepathElements()).hasSize(1);
        assertThat(result.getClasspathElements()).hasSize(1);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testDirectStatic() throws Exception {
        Path moduleA = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path moduleB = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path moduleC = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        Path moduleD = Paths.get("src/test/resources/mock/jar2.jar"); // any existing file
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(moduleB, moduleC, moduleD).setMainModuleDescriptor(moduleA);
        // .setIncludeStatic( true );

        when(qdoxParser.fromSourcePath(moduleA))
                .thenReturn(JavaModuleDescriptor.newModule("moduleA")
                        .requires("moduleB")
                        .requires(Collections.singleton(JavaModifier.STATIC), "moduleD")
                        .build());
        when(asmParser.getModuleDescriptor(moduleB))
                .thenReturn(JavaModuleDescriptor.newModule("moduleB")
                        .requires(Collections.singleton(JavaModifier.STATIC), "moduleC")
                        .build());
        when(asmParser.getModuleDescriptor(moduleC))
                .thenReturn(JavaModuleDescriptor.newModule("moduleC").build());
        when(asmParser.getModuleDescriptor(moduleD))
                .thenReturn(JavaModuleDescriptor.newModule("moduleD").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(3);
        assertThat(result.getModulepathElements()).containsOnlyKeys(moduleB, moduleD);
        assertThat(result.getClasspathElements()).containsOnly(moduleC);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testDuplicateModule() throws Exception {
        Path moduleA = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path moduleB = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path moduleC = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file

        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(moduleB, moduleC).setMainModuleDescriptor(moduleA);

        when(qdoxParser.fromSourcePath(moduleA))
                .thenReturn(JavaModuleDescriptor.newModule("moduleA")
                        .requires("anonymous")
                        .build());
        when(asmParser.getModuleDescriptor(moduleB))
                .thenReturn(JavaModuleDescriptor.newModule("anonymous").build());
        when(asmParser.getModuleDescriptor(moduleC))
                .thenReturn(JavaModuleDescriptor.newModule("anonymous").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(2);
        assertThat(result.getModulepathElements()).containsOnlyKeys(moduleB);
        // with current default the duplicate will be ignored
        assertThat(result.getClasspathElements()).hasSize(0);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    @Test
    void testStaticTransitive() throws Exception {
        Path moduleA = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path moduleB = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path moduleC = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        Path moduleD = Paths.get("src/test/resources/mock/jar2.jar"); // any existing file
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(moduleB, moduleC, moduleD).setMainModuleDescriptor(moduleA);

        when(qdoxParser.fromSourcePath(moduleA))
                .thenReturn(JavaModuleDescriptor.newModule("moduleA")
                        .requires("moduleB")
                        .build());
        when(asmParser.getModuleDescriptor(moduleB))
                .thenReturn(JavaModuleDescriptor.newModule("moduleB")
                        .requires(new HashSet<>(Arrays.asList(JavaModifier.STATIC, JavaModifier.TRANSITIVE)), "moduleC")
                        .build());
        when(asmParser.getModuleDescriptor(moduleC))
                .thenReturn(JavaModuleDescriptor.newModule("moduleC")
                        .requires(new HashSet<>(Arrays.asList(JavaModifier.STATIC)), "moduleD")
                        .build());
        when(asmParser.getModuleDescriptor(moduleD))
                .thenReturn(JavaModuleDescriptor.newModule("moduleD").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getPathElements()).hasSize(3);
        assertThat(result.getModulepathElements()).containsOnlyKeys(moduleB, moduleC);
        assertThat(result.getClasspathElements()).containsOnly(moduleD);
        assertThat(result.getPathExceptions()).hasSize(0);
    }

    /**
     * test case for <a href="https://issues.apache.org/jira/browse/MCOMPILER-481">MCOMPILER-481</a>
     */
    @Test
    void includeDeeperRequiresStatic() throws Exception {
        Path moduleA = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java
        Path moduleB = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path moduleC = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths(moduleA, moduleB, moduleC)
                .setMainModuleDescriptor(moduleA)
                .setIncludeStatic(true);
        when(qdoxParser.fromSourcePath(moduleA))
                .thenReturn(JavaModuleDescriptor.newModule("moduleA")
                        .requires("moduleB")
                        .build());
        when(asmParser.getModuleDescriptor(moduleB))
                .thenReturn(JavaModuleDescriptor.newModule("moduleB")
                        .requires(Collections.singleton(JavaModifier.STATIC), "moduleC")
                        .build());
        when(asmParser.getModuleDescriptor(moduleC))
                .thenReturn(JavaModuleDescriptor.newModule("moduleC").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getModulepathElements()).containsOnlyKeys(moduleB, moduleC);
    }

    /**
     * test case for <a href="https://issues.apache.org/jira/browse/MCOMPILER-482">MCOMPILER-482</a>
     */
    @Test
    void includeDeeperRequiresStaticTransitive() throws Exception {
        Path moduleA = Paths.get("src/test/resources/mock/module-info.java"); // some file called module-info.java core
        Path moduleB = Paths.get("src/test/resources/mock/jar0.jar"); // any existing file
        Path moduleC = Paths.get("src/test/resources/mock/jar1.jar"); // any existing file
        Path moduleD = Paths.get("src/test/resources/mock/jar2.jar"); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths(moduleA, moduleB, moduleC, moduleD)
                .setMainModuleDescriptor(moduleA)
                .setIncludeStatic(true);
        when(qdoxParser.fromSourcePath(moduleA))
                .thenReturn(JavaModuleDescriptor.newModule("moduleA")
                        .requires("moduleB")
                        .build());
        when(asmParser.getModuleDescriptor(moduleB))
                .thenReturn(JavaModuleDescriptor.newModule("moduleB")
                        .requires("moduleC")
                        .requires(new HashSet<>(Arrays.asList(JavaModifier.STATIC, JavaModifier.TRANSITIVE)), "moduleD")
                        .build());
        when(asmParser.getModuleDescriptor(moduleC))
                .thenReturn(JavaModuleDescriptor.newModule("moduleC")
                        .requires(new HashSet<>(Arrays.asList(JavaModifier.STATIC, JavaModifier.TRANSITIVE)), "moduleD")
                        .build());
        when(asmParser.getModuleDescriptor(moduleD))
                .thenReturn(JavaModuleDescriptor.newModule("moduleD").build());

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);
        assertThat(result.getModulepathElements()).containsOnlyKeys(moduleB, moduleC, moduleD);
    }
}
