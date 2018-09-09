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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith( org.mockito.junit.MockitoJUnitRunner.class )
public class LocationManagerTest
{
    @Mock
    private BinaryModuleInfoParser asmParser;

    @Mock
    private SourceModuleInfoParser qdoxParser;

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
        ResolvePathsResult<File> result = locationManager.resolvePaths( ResolvePathsRequest.ofFiles( Collections.<File>emptyList() ) );
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
        ResolvePathsRequest<File> request = ResolvePathsRequest.ofFiles( Collections.<File>emptyList() ).setMainModuleDescriptor( mockModuleInfoJava.toFile() );
        
        ResolvePathsResult<File> result = locationManager.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 0 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }
    
    @Test
    public void testManifestWithReflectRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/dir.manifest.with/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "auto.by.manifest" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
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
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
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
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
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
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Arrays.asList( pj1, pj2 ) ).setMainModuleDescriptor( mockModuleInfoJava );

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
    public void testNonJar() throws Exception
    {
        Path p = Paths.get( "src/test/resources/nonjar/pom.xml" );
        
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Arrays.asList( p ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        
        assertThat( result.getPathExceptions().size(), is( 1 ) );
    }
    
    @Test
    public void testAdditionalModules() throws Exception
    {
        Path p = Paths.get( "src/test/resources/mock/jar0.jar" );
        
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request =
            ResolvePathsRequest.ofPaths( Arrays.asList( p ) )
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
    
    @Test
    public void testResolvePath() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/mock/jar0.jar" );
        ResolvePathRequest<Path> request = ResolvePathRequest.ofPath( abc );
        
        when( asmParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "org.objectweb.asm" ).build() );
        
        ResolvePathResult result = locationManager.resolvePath( request );

        assertThat( result.getModuleDescriptor(), is( JavaModuleDescriptor.newModule( "org.objectweb.asm" ).build() ) );
        assertThat( result.getModuleNameSource(), is( ModuleNameSource.MODULEDESCRIPTOR ) );
    }

    @Test
    public void testNoMatchingProviders() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/mock/module-info.java" ); // some file called module-info.java
        Path def = Paths.get( "src/test/resources/mock/jar0.jar" ); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( def ).setMainModuleDescriptor( abc ).setIncludeAllProviders( true );
        
        when(  qdoxParser.fromSourcePath( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "abc" ).uses( "device" ).build() );
        when(  asmParser.getModuleDescriptor( def ) ).thenReturn( JavaModuleDescriptor.newModule( "def" ).provides​( "tool", Arrays.asList( "java", "javac" ) ).build() );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

    
    @Test
    public void testMainModuleDescriptorWithProviders() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/mock/module-info.java" ); // some file called module-info.java
        Path def = Paths.get( "src/test/resources/mock/jar0.jar" ); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( def ).setMainModuleDescriptor( abc ).setIncludeAllProviders( true );
        
        when(  qdoxParser.fromSourcePath( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "abc" ).uses( "tool" ).build() );
        when(  asmParser.getModuleDescriptor( def ) ).thenReturn( JavaModuleDescriptor.newModule( "def" ).provides​( "tool", Arrays.asList( "java", "javac" ) ).build() );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

    @Test
    public void testMainModuleDescriptorWithProvidersDontIncludeProviders() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/mock/module-info.java" ); // some file called module-info.java
        Path def = Paths.get( "src/test/resources/mock/jar0.jar" ); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( def ).setMainModuleDescriptor( abc );
        
        when(  qdoxParser.fromSourcePath( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "abc" ).uses( "tool" ).build() );
        when(  asmParser.getModuleDescriptor( def ) ).thenReturn( JavaModuleDescriptor.newModule( "def" ).provides​( "tool", Arrays.asList( "java", "javac" ) ).build() );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

    @Test
    public void testTransitiveProviders() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/mock/module-info.java" ); // some file called module-info.java
        Path def = Paths.get( "src/test/resources/mock/jar0.jar" ); // any existing file
        Path ghi = Paths.get( "src/test/resources/mock/jar1.jar" ); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( def, ghi ).setMainModuleDescriptor( abc ).setIncludeAllProviders( true );
        
        when(  qdoxParser.fromSourcePath( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "abc" ).requires( "ghi" ).build() );
        when(  asmParser.getModuleDescriptor( def ) ).thenReturn( JavaModuleDescriptor.newModule( "def" ).provides​( "tool", Arrays.asList( "java", "javac" ) ).build() );
        when(  asmParser.getModuleDescriptor( ghi ) ).thenReturn( JavaModuleDescriptor.newModule( "ghi" ).uses( "tool" ).build() );
        
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        assertThat( result.getPathElements().size(), is( 2 ) );
        assertThat( result.getModulepathElements().size(), is( 2 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }
    
    @Test
    public void testDontIncludeProviders() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/mock/module-info.java" ); // some file called module-info.java
        Path def = Paths.get( "src/test/resources/mock/jar0.jar" ); // any existing file
        Path ghi = Paths.get( "src/test/resources/mock/jar1.jar" ); // any existing file
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( def, ghi ).setMainModuleDescriptor( abc );
        
        when(  qdoxParser.fromSourcePath( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "abc" ).requires( "ghi" ).build() );
        when(  asmParser.getModuleDescriptor( def ) ).thenReturn( JavaModuleDescriptor.newModule( "def" ).provides​( "tool", Arrays.asList( "java", "javac" ) ).build() );
        when(  asmParser.getModuleDescriptor( ghi ) ).thenReturn( JavaModuleDescriptor.newModule( "ghi" ).uses( "tool" ).build() );
        
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        assertThat( result.getPathElements().size(), is( 2 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
        assertThat( result.getPathExceptions().size(), is( 0 ) );
    }

}
