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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Iterator;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires;
import org.junit.Test;

public class AsmModuleInfoParserTest
{
    private ModuleInfoParser parser = new AsmModuleInfoParser();

    @Test
    public void testJarDescriptor() throws Exception
    {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jar.descriptor/asm-6.0_BETA.jar" ) );
        
        assertNotNull( descriptor);
        assertEquals( "org.objectweb.asm", descriptor.name() );
        assertEquals( false, descriptor.isAutomatic() );

        assertEquals( 1, descriptor.requires().size() );
        assertEquals( "java.base", descriptor.requires().iterator().next().name() );

        assertEquals( 2, descriptor.exports().size() );
        assertEquals( "org.objectweb.asm", descriptor.exports().iterator().next().source() );
    }

    @Test
    public void testMultiReleaseJarDescriptor() throws Exception
    {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jar.mr.descriptor/jloadr-1.0-SNAPSHOT.jar" ) );
        
        assertNotNull( descriptor);
        assertEquals( "de.adito.jloadr", descriptor.name() );
        assertEquals( false, descriptor.isAutomatic() );
    }

    @Test
    public void testIncompleteMultiReleaseJarDescriptor() throws Exception
    {
        // this jar is missing the Multi-Release: true entry in the Manifest
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jar.mr.incomplete.descriptor/jloadr-1.0-SNAPSHOT.jar" ) );
        
        assertNull( descriptor);
    }

    @Test
    public void testClassicJar() throws Exception
    {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jar.empty/plexus-java-1.0.0-SNAPSHOT.jar" ) );
        
        assertNull( descriptor);
    }
    
    @Test
    public void testOutputDirectoryDescriptor()
        throws Exception
    {
        JavaModuleDescriptor descriptor =
            parser.getModuleDescriptor( Paths.get( "src/test/resources/dir.descriptor/out" ) );

        assertNotNull( descriptor );
        assertEquals( "org.codehaus.plexus.languages.java.demo", descriptor.name() );
        assertEquals( false, descriptor.isAutomatic() );

        assertEquals( 3, descriptor.requires().size() );

        Iterator<JavaRequires> requiresIter = descriptor.requires().iterator();

        JavaRequires requires = requiresIter.next();
        assertEquals( "java.base", requires.name() );
        assertFalse( requires.modifiers​().contains( JavaRequires.JavaModifier.STATIC ) );

        requires = requiresIter.next();
        assertEquals( "java.xml", requires.name() );
        assertFalse( requires.modifiers​().contains( JavaRequires.JavaModifier.STATIC ) );

        requires = requiresIter.next();
        assertEquals( "com.google.common", requires.name() );
        assertTrue( requires.modifiers​().contains( JavaRequires.JavaModifier.STATIC ) );
    }

    @Test( expected = NoSuchFileException.class )
    public void testClassicOutputDirectory() throws Exception
    {
        parser.getModuleDescriptor( Paths.get( "src/test/resources/dir.empty/out" ) );
    }

    @Test
    public void testJModDescriptor() throws Exception
    {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jmod.descriptor/first-jmod-1.0-SNAPSHOT.jmod" ) );
        
        assertNotNull( descriptor);
        assertEquals( "com.corporate.project", descriptor.name() );
        assertEquals( false, descriptor.isAutomatic() );

        assertEquals( 1, descriptor.requires().size() );
        assertEquals( "java.base", descriptor.requires().iterator().next().name() );

        assertEquals ( 1, descriptor.exports().size() );
        assertEquals ( "com.corporate.project",  descriptor.exports().iterator().next().source() );
    }
    
    @Test( expected = IOException.class )
    public void testInvalidFile() throws Exception
    {
        parser.getModuleDescriptor( Paths.get( "src/test/resources/nonjar/pom.xml" ) );
    }

}
