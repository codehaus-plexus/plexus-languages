package org.codehaus.plexus.languages.java.jpms;

import static org.junit.Assert.assertArrayEquals;

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import org.junit.Test;

public class BinaryModuleInfoParserTest
{
    private BinaryModuleInfoParser parser = new BinaryModuleInfoParser();

    @Test
    public void testJarDescriptor() throws Exception
    {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jar.descriptor/asm-6.0_BETA.jar" ) );
        
        assertNotNull( descriptor);
        assertEquals( "org.objectweb.asm", descriptor.name() );
        assertEquals( false, descriptor.isAutomatic() );

        assertEquals( 1, descriptor.requires().size() );
        assertEquals( "java.base", descriptor.requires().iterator().next().name() );

        Set<JavaExports> expectedExports = JavaModuleDescriptor.newAutomaticModule( "_" )
                .exports( "org.objectweb.asm" )
                .exports( "org.objectweb.asm.signature" )
                .build()
                .exports();
        assertEquals( expectedExports, descriptor.exports() );
    }

    @Test
    public void testMultiReleaseJarDescriptor() throws Exception
    {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jar.mr.descriptor/jloadr-1.0-SNAPSHOT.jar" ), JavaVersion.parse( "17" ) );
        
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

        Set<JavaRequires> expectedRequires = JavaModuleDescriptor.newAutomaticModule( "_" )
            .requires( "java.base" )
            .requires( "java.xml" )
            .requires​( Collections.singleton( JavaRequires.JavaModifier.STATIC ), "com.google.common" )
            .build()
            .requires();

        assertEquals( expectedRequires, descriptor.requires() );
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
    
    @Test
    public void testUses() throws Exception
    {
        try ( InputStream is = Files.newInputStream( Paths.get( "src/test/resources/dir.descriptor.uses/out/module-info.class" ) ) )
        {
            JavaModuleDescriptor descriptor = parser.parse( is );
            
            assertNotNull( descriptor);
            assertEquals( new HashSet<>( Arrays.asList( "org.apache.logging.log4j.spi.Provider",
                                                        "org.apache.logging.log4j.util.PropertySource",
                                                        "org.apache.logging.log4j.message.ThreadDumpMessage$ThreadInfoFactory" ) ),
                          descriptor.uses() );
        }
    }
    
    @Test
    public void testProvides() throws Exception
    {
        JavaModuleDescriptor descriptor = parser.getModuleDescriptor( Paths.get( "src/test/resources/jar.service/threeten-extra-1.4.jar" ) );
        
        assertNotNull( descriptor );
        assertEquals( 1, descriptor.provides().size() );
        
        JavaProvides provides = descriptor.provides().iterator().next();
        assertEquals( "java.time.chrono.Chronology", provides.service() );
        assertArrayEquals( new String[] { "org.threeten.extra.chrono.BritishCutoverChronology",
            "org.threeten.extra.chrono.CopticChronology", "org.threeten.extra.chrono.DiscordianChronology",
            "org.threeten.extra.chrono.EthiopicChronology", "org.threeten.extra.chrono.InternationalFixedChronology",
            "org.threeten.extra.chrono.JulianChronology", "org.threeten.extra.chrono.PaxChronology",
            "org.threeten.extra.chrono.Symmetry010Chronology", "org.threeten.extra.chrono.Symmetry454Chronology" },
                           provides.providers().toArray( new String[0] ) );

    }

}
