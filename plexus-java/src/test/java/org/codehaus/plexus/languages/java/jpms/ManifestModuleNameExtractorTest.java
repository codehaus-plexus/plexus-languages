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

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class ManifestModuleNameExtractorTest
{
    private ManifestModuleNameExtractor extractor = new ManifestModuleNameExtractor();

    @Test
    public void testNoManifestInJar() throws Exception
    {
        assertNull( extractor.extract( new File( "src/test/resources/jar.name/plexus-java-1.0.0-SNAPSHOT.jar" ) ) );
    }

    @Test
    public void testManifestInJar() throws Exception
    {
        assertEquals( "org.codehaus.plexus.languages.java", extractor.extract( new File( "src/test/resources/jar.manifest/plexus-java-1.0.0-SNAPSHOT.jar" ) ) );
    }

    @Test
    public void testNoManifestInDir() throws Exception
    {
        assertNull( extractor.extract( new File( "src/test/resources/empty/out" ) ) );
    }

    @Test
    public void testEmptyManifestInDir() throws Exception
    {
        assertNull( extractor.extract( new File( "src/test/resources/manifest.without/out" ) ) );
    }

    @Test
    public void testManifestInDir() throws Exception
    {
        assertEquals( "auto.by.manifest", extractor.extract( new File( "src/test/resources/manifest.with/out" ) ) );
    }
}
