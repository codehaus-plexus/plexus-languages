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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/**
 * <strong>NOTE</strong> Eclipse users must disable the <code>Build automatically</code> option, 
 * otherwise it'll continually rebuild the project, causing compilations or tests to fail. 
 *  
 * @author Robert Scholte
 */
@RunWith( org.mockito.junit.MockitoJUnitRunner.class )
public class LocationManagerIT
{
    @Mock
    private BinaryModuleInfoParser asmParser;

    @Mock
    private SourceModuleInfoParser qdoxParser;

    private LocationManager locationManager;
    
    final Path mockModuleInfoJava = Paths.get( "src/test/resources/mock/module-info.java");
    
    @BeforeClass
    public static void assume()
    {
        assumeThat( "Requires at least Java 9", System.getProperty( "java.version" ), not( startsWith( "1." ) ) );
    }
    
    @Before
    public void onSetup()
    {
        locationManager = new LocationManager( asmParser, qdoxParser );
    }
    
    @Test
    public void testManifestWithoutReflectRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/manifest.without/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "any" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getPathExceptions().size(), is( 0 ) );
        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
    }
    
    @Test
    public void testEmptyWithReflectRequires() throws Exception
    {
        Path abc = Paths.get( "src/test/resources/empty/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "a.b.c" ).build();
        when( qdoxParser.fromSourcePath( any( Path.class ) ) ).thenReturn( descriptor );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Collections.singletonList( abc ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );

        assertThat( result.getPathExceptions().size(), is( 0 ) );
        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
    }
    
    @Test( expected = RuntimeException.class )
    public void testResolvePathWithException() throws Exception
    {
        assumeThat( "Requires at least Java 9", System.getProperty( "java.version" ), not( startsWith( "1." ) ) );
        
        Path p = Paths.get( "src/test/resources/jar.empty.invalid.name/101-1.0.0-SNAPSHOT.jar" );
        ResolvePathRequest<Path> request = ResolvePathRequest.ofPath( p );
        
        locationManager.resolvePath( request );
    }
    
    @Test
    public void testClassicJarNameStartsWithNumber() throws Exception
    {
        assumeThat( "Requires at least Java 9", System.getProperty( "java.version" ), not( startsWith( "1." ) ) );
        
        Path p = Paths.get( "src/test/resources/jar.empty.invalid.name/101-1.0.0-SNAPSHOT.jar" );
        ResolvePathsRequest<Path> request = ResolvePathsRequest.ofPaths( Arrays.asList( p ) ).setMainModuleDescriptor( mockModuleInfoJava );
        
        ResolvePathsResult<Path> result = locationManager.resolvePaths( request );
        
        assertThat( result.getPathExceptions().size(), is( 1 ) );
    }
}
