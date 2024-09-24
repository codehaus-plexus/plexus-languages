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
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManifestModuleNameExtractorTest {
    private ManifestModuleNameExtractor extractor = new ManifestModuleNameExtractor();

    @Test
    void testNoManifestInJar() throws Exception {
        assertNull(extractor.extract(Paths.get("src/test/test-data/jar.name/plexus-java-1.0.0-SNAPSHOT.jar")));
    }

    @Test
    void testManifestInJar() throws Exception {
        assertEquals(
                "org.codehaus.plexus.languages.java",
                extractor.extract(Paths.get("src/test/test-data/jar.manifest.with/plexus-java-1.0.0-SNAPSHOT.jar")));
    }

    @Test
    void testNoManifestInDir() throws Exception {
        assertNull(extractor.extract(Paths.get("src/test/test-data/empty/out")));
    }

    @Test
    void testEmptyManifestInDir() throws Exception {
        assertNull(extractor.extract(Paths.get("src/test/test-data/manifest.without/out")));
    }

    @Test
    void testManifestInDir() throws Exception {
        assertEquals("auto.by.manifest", extractor.extract(Paths.get("src/test/test-data/dir.manifest.with/out")));
    }
}
