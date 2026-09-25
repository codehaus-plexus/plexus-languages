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

import org.codehaus.plexus.languages.java.version.JavaVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end coverage of {@link ResolvePathsRequest#setTargetRelease(JavaVersion)}, using the real
 * {@link BinaryModuleInfoParser} against a directory that has a versioned module descriptor for both Java 11
 * and Java 17. Runs on every JDK, including Java 8, since the whole point of the target release setting is to
 * pick the applicable versioned descriptor without relying on the version of the JDK that runs the build.
 */
class LocationManagerTargetReleaseTest {
    private final LocationManager locationManager = new LocationManager();

    private final Path dir = Paths.get("src/test/test-data/dir.mr.target.release/out");

    @Test
    void targetReleaseSelectsItsOwnVersionedDescriptor() throws Exception {
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(dir)).setTargetRelease(JavaVersion.parse("11"));

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathElements().get(dir).name()).isEqualTo("dir.mr.target.release.v11");
    }

    @Test
    void targetReleaseWorksIndependentlyOfTheRunningJdk() throws Exception {
        // On a Java 8 runtime compiling through a Java 9+ toolchain, the running JDK's version must not be used
        // to pick the versioned descriptor - this is the scenario from #237 (Maven 3.9 on JDK 8, --release 17).
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(dir)).setTargetRelease(JavaVersion.parse("17"));

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathElements().get(dir).name()).isEqualTo("dir.mr.target.release.v17");
    }

    @Test
    void targetReleaseBelowEveryVersionedDescriptorSelectsNone() throws Exception {
        ResolvePathsRequest<Path> request =
                ResolvePathsRequest.ofPaths(Collections.singletonList(dir)).setTargetRelease(JavaVersion.parse("9"));

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathElements().get(dir)).isNull();
    }

    @Test
    void preReleaseTargetRelease() throws Exception {
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths(Collections.singletonList(dir))
                .setTargetRelease(JavaVersion.parse("11-ea"));

        ResolvePathsResult<Path> result = locationManager.resolvePaths(request);

        assertThat(result.getPathExceptions()).isEmpty();
        assertThat(result.getPathElements().get(dir).name()).isEqualTo("dir.mr.target.release.v11");
    }

    @Test
    void singlePathTargetRelease() throws Exception {
        ResolvePathRequest<Path> request = ResolvePathRequest.ofPath(dir).setTargetRelease(JavaVersion.parse("11"));

        ResolvePathResult result = locationManager.resolvePath(request);

        assertThat(result.getModuleDescriptor().name()).isEqualTo("dir.mr.target.release.v11");
    }

    @Test
    void nullTargetReleaseIsRejected() {
        assertThatThrownBy(() -> ResolvePathsRequest.ofPaths(Collections.singletonList(dir))
                        .setTargetRelease(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ResolvePathRequest.ofPath(dir).setTargetRelease(null))
                .isInstanceOf(NullPointerException.class);
    }
}
