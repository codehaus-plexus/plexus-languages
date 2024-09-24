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

public abstract class AbstractFilenameModuleNameExtractorTest {
    protected abstract ModuleNameExtractor getExtractor();

    @Test
    void testJarWithoutManifest() throws Exception {
        String name = getExtractor().extract(Paths.get("src/test/test-data/jar.empty/plexus-java-1.0.0-SNAPSHOT.jar"));
        assertEquals("plexus.java", name);
    }

    @Test
    void testJarWithManifest() throws Exception {
        String name = getExtractor()
                .extract(Paths.get("src/test/test-data/jar.manifest.with/plexus-java-1.0.0-SNAPSHOT.jar"));
        assertEquals("org.codehaus.plexus.languages.java", name);
    }

    @Test
    void testJarUnsupported() throws Exception {
        String name = getExtractor().extract(Paths.get("src/test/test-data/jar.unsupported/jdom-1.0.jar"));
        assertNull(name);
    }

    @Test
    void testJarWithSpacesInPath() throws Exception {
        String name = getExtractor()
                .extract(Paths.get("src/test/test-data/jar with spaces in path/plexus-java-1.0.0-SNAPSHOT.jar"));
        assertEquals("org.codehaus.plexus.languages.java", name);
    }
}
