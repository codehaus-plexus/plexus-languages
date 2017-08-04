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
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class ProjectPathsAnalyzerTest
{
    @Mock
    private ModuleInfoParser asmParser;

    @Mock
    private ModuleInfoParser reflectParser;
    
    @InjectMocks
    private LocationManager analyzer;

    @Test
    public void testNoPaths() throws Exception
    {
        ResolvePathsResult result = analyzer.resolvePaths( new ResolvePathsRequest() );
        assertThat( result.getMainModuleDescriptor(), nullValue( JavaModuleDescriptor.class) );
        assertThat( result.getPathElements().size(), is( 0 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
    }

    @Test
    public void testWithUnknownRequires() throws Exception
    {
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "java.base" ).requires( "jdk.net" ).build();
        ResolvePathsRequest request = new ResolvePathsRequest().setMainModuleDescriptor( descriptor );
        
        ResolvePathsResult result = analyzer.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 0 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
    }

    @Test
    public void testEmptyWithReflectRequires() throws Exception
    {
        File abc = new File( "src/test/resources/empty/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "a.b.c" ).build();
        ResolvePathsRequest request = new ResolvePathsRequest().setMainModuleDescriptor( descriptor ).setPathElements( Collections.singletonList( abc ) );
        
        when( reflectParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newModule( "def" ).build() );
        
        ResolvePathsResult result = analyzer.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
    }
    
    @Test
    public void testManifestWithoutReflectRequires() throws Exception
    {
        File abc = new File( "src/test/resources/manifest.without/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "any" ).build();
        ResolvePathsRequest request = new ResolvePathsRequest().setMainModuleDescriptor( descriptor ).setPathElements( Collections.singletonList( abc ) );
        
        when( reflectParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newAutomaticModule( "auto.by.manifest" ).build() );
        
        ResolvePathsResult result = analyzer.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 0 ) );
        assertThat( result.getClasspathElements().size(), is( 1 ) );
    }
    
    @Test
    public void testManifestWithReflectRequires() throws Exception
    {
        File abc = new File( "src/test/resources/manifest.with/out" );
        JavaModuleDescriptor descriptor = JavaModuleDescriptor.newModule( "base" ).requires( "auto.by.manifest" ).build();
        ResolvePathsRequest request = new ResolvePathsRequest().setMainModuleDescriptor( descriptor ).setPathElements( Collections.singletonList( abc ) );
        
        when( reflectParser.getModuleDescriptor( abc ) ).thenReturn( JavaModuleDescriptor.newAutomaticModule( "auto.by.manifest" ).build() );
        
        ResolvePathsResult result = analyzer.resolvePaths( request );

        assertThat( result.getMainModuleDescriptor(), is( descriptor) );
        assertThat( result.getPathElements().size(), is( 1 ) );
        assertThat( result.getModulepathElements().size(), is( 1 ) );
        assertThat( result.getClasspathElements().size(), is( 0 ) );
    }

}
