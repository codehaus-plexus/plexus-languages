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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult.ModuleNameSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith( org.mockito.junit.MockitoJUnitRunner.class )
public class LocationManagerTest
{
    @Mock
    private ModuleInfoParser asmParser;

    @Mock
    private QDoxModuleInfoParser qdoxParser;

    private LocationManager locationManager;
    
    final Path mockModuleInfoJava = Paths.get( "src/test/resources/mock/module-info.java");
    
    @Before
    public void onSetup()
    {
        locationManager = new LocationManager( asmParser, qdoxParser );
    }

    @Test
    public void testNoPaths() throws Exception
    {
        ResolvePathsResult<File> result = locationManager.resolvePaths( ResolvePathsRequest.withFiles( Collections.<File>emptyList() ) );
        assertThat( result.getMainModuleDescriptor(), nullValue( JavaModuleDescriptor.class) );
        assertThat( result.getPathElements().size(), is( 0 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

    @Test
    public void testWithUnknownRequires() throws Exception
    {
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "java.base" ).requires( "jdk.net" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<File> request = ResolvePathsRequest.withFiles( Collections.<File>emptyList() ).setMainModuleDescriptor( mockModuleInfoJava.toFile() );
        
        ResolvePathsResult<File> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 0 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

    @Test
    public void testEmptyWithReflectRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/empty/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "a.b.c" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }
    
    @Test
    public void testManifestWithoutReflectRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/manifest.without/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "any" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
//        when( reflectParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newAutomaticModule( "auto.by.manifest" ).build() );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }
    
    @Test
    public void testManifestWithReflectRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/dir.manifest.with/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "auto.by.manifest" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
//        when( reflectParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newAutomaticModule( "auto.by.manifest" ).build() );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().get( abc), is( ModuleNameSource.MANIFEST ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }
    
    @Test
    public void testDirDescriptorWithReflectRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/dir.descriptor/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "dir.descriptor" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        when( asmParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "dir.descriptor" ).build() );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().get( abc), is( ModuleNameSource.MODULEDESCRIPTOR ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

    @Test
    public void testJarWithAsmRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/jar.descriptor/asm-6.0_BETA.jar" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "org.objectweb.asm" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        when( asmParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "org.objectweb.asm" ).build() );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().get( abc), is( ModuleNameSource.MODULEDESCRIPTOR ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }
    
    @Test
    public void testIdenticalModuleNames() throws Exception
    {
        Path pj1 = Paths.get( "src/test/resources/jar.empty/plexus-java-1.0.0-SNAPSHOT.jar" );
        Path pj2 = Paths.get( "src/test/resources/jar.empty.2/plexus-java-2.0.0-SNAPSHOT.jar" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "plexus.java" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Arrays.asList( pj1, pj2 ) ).setMainModuleDescriptor( mockModuleInfoJava );

        when( asmParser.getModuleDescriptor( pj1 ) ).thenReturn( JavaModuleDescriptor.newAutomaticModule( "plexus.java" ).build() );
        when( asmParser.getModuleDescriptor( pj2 ) ).thenReturn( JavaModuleDescriptor.newAutomaticModule( "plexus.java" ).build() );

        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 2 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().containsKey( pj1 ), is( true ) );
        assertThat( result.getModulepathElements().containsKey( pj2 ), is( false ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

    @Test
    public void testClassicJarNameStartsWithNumber() throws Exception
    {
        assumeThat( "Requires at least Java 9", System.getProperty( "java.version" ), not( startsWith( "1." ) ) );
        
        Path p = Paths.get( "src/test/resources/jar.empty.invalid.name/101-1.0.0-SNAPSHOT.jar" );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Arrays.asList( p ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        
        assertThat( result.getPathExceptions().size(), is( 1 ) );
    }

    @Test
    public void testNonJar() throws Exception
    {
        Path p = Paths.get( "src/test/resources/nonjar/pom.xml" );
        when( asmParser.getModuleDescriptor( p ) ).thenThrow( new IOException() );
        
        ResolvePathsRequest<Path> request = ResolvePathsRequest.withPaths( Arrays.asList( p ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        
        assertThat( result.getPathExceptions().size(), is( 1 ) );
    }
    
    @Test
    public void testAdditionalModules() throws Exception
    {
        Path p = Paths.get( "src/test/resources/jar.empty/plexus-java-1.0.0-SNAPSHOT.jar" );
        
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request =
            ResolvePathsRequest.withPaths( Arrays.asList( p ) )
                               .setMainModuleDescriptor( mockModuleInfoJava )
                               .setAdditionalModules( Collections.singletonList( "plexus.java" ) );

        when( asmParser.getModuleDescriptor( p ) ).thenReturn( JavaModuleDescriptor.newAutomaticModule( "plexus.java" ).build() );

        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

}
