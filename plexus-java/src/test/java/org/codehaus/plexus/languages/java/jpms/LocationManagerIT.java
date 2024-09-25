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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * <strong>NOTE</strong> Eclipse users must disable the <code>Build automatically</code> option,
 * otherwise it'll continually rebuild the project, causing compilations or tests to fail.
 *
 * @author Robert Scholte
 */
@DisabledOnJre(value = JRE.JAVA_8, disabledReason = "Requires Java 9+ Module System")
@ExtendWith(MockitoExtension.class)
class LocationManagerIT {
    @Mock
    private BinaryModuleInfoParser asmParser;

    @Mock
    private SourceModuleInfoParser qdoxParser;

    private LocationManager locationManager;

    final Path mockModuleInfoJava = Paths.get("src/test/test-data/mock/module-info.java");

    @BeforeEach
    void onSetup() {
        locationManager = new LocationManager(qdoxParser) {
            @Override
            ModuleInfoParser getBinaryModuleInfoParser(Path jdkHome) {
                return asmParser;
            }
        };
    }

    @Test
    void testManifestWithoutReflectRequires() throws Exception {
        Path abc = Paths.get("src/test/test-data/manifest.without/out");
        JavaModuleDescriptor descriptor =
                JavaModuleDescriptor.newModule("base").requires("any").build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(abc)).setMainModuleDescriptor(mockModuleInfoJava);

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathExceptions()).isEmpty();
        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).isEmpty();
        assertThat(result.getClasspathElements()).hasSize(1);
    }

    @Test
    void testEmptyWithReflectRequires() throws Exception {
        Path abc = Paths.get("src/test/test-data/empty/out");
        JavaModuleDescriptor descriptor =
                JavaModuleDescriptor.newModule("base").requires("a.b.c").build();
        when(qdoxParser.fromSourcePath(any(Path.class))).thenReturn(descriptor);
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(abc)).setMainModuleDescriptor(mockModuleInfoJava);

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathExceptions()).hasSize(0);
        assertThat(result.getMainModuleDescriptor()).isEqualTo(descriptor);
        assertThat(result.getPathElements()).hasSize(1);
        assertThat(result.getModulepathElements()).hasSize(0);
        assertThat(result.getClasspathElements()).hasSize(1);
    }

    @Test
    void testResolvePathWithException() {
        assertThrows(RuntimeException.class, () -> {
            Path p = Paths.get("src/test/test-data/jar.empty.invalid.name/101-1.0.0-SNAPSHOT.jar");
            ResolvePathRequest<Path> request = ResolvePathRequest.ofPath(p);

            locationManager.resolvePath(request);
        });
    }

    @Test
    void testClassicJarNameStartsWithNumber() throws Exception {
        Path p = Paths.get("src/test/test-data/jar.empty.invalid.name/101-1.0.0-SNAPSHOT.jar");
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(p)).setMainModuleDescriptor(mockModuleInfoJava);

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathExceptions()).hasSize(1);
        assertThat(result.getClasspathElements()).hasSize(1);
    }
}
