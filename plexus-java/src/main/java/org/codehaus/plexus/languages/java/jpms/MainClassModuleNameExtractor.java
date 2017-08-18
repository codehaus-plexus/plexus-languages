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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Extract the module name by calling the main method with an external JVM
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class MainClassModuleNameExtractor
{
    private final Path jdkHome;

    public MainClassModuleNameExtractor( Path jdkHome )
    {
        this.jdkHome = jdkHome;
    }

    public <T> Map<T, String> extract( Map<T, Path> files )
        throws IOException
    {
        Path workDir = Files.createTempDirectory( "plexus-java_jpms-" );

        try (InputStream is =
            MainClassModuleNameExtractor.class.getResourceAsStream( this.getClass().getSimpleName() + ".class" ))
        {
            Path pckg = workDir.resolve( this.getClass().getPackage().getName().replace( '.', '/' ) );

            Files.createDirectories( pckg );

            Files.copy( is, pckg.resolve( this.getClass().getSimpleName() + ".class" ) );
        }

        try (BufferedWriter argsWriter = Files.newBufferedWriter( workDir.resolve( "args" ), Charset.defaultCharset() ))
        {
            argsWriter.append( "--class-path" );
            argsWriter.newLine();

            argsWriter.append( "." );
            argsWriter.newLine();

            argsWriter.append( this.getClass().getName() );
            argsWriter.newLine();

            for ( Path p : files.values() )
            {
                argsWriter.append( p.toAbsolutePath().toString() );
                argsWriter.newLine();
            }
        }

        ProcessBuilder builder = new ProcessBuilder( jdkHome.resolve( "bin/java" ).toAbsolutePath().toString(),
                                                     "@args" ).directory( workDir.toFile() );

        Process p = builder.start();

        Properties output = new Properties();
        try (InputStream is = p.getInputStream())
        {
            output.load( is );
        }

        Map<T, String> moduleNames = new HashMap<>( files.size() );
        for ( Map.Entry<T, Path> entry : files.entrySet() )
        {
            moduleNames.put( entry.getKey(), output.getProperty( entry.getValue().toAbsolutePath().toString(), null ) );
        }

        try
        {
            Files.walkFileTree( workDir, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                    throws IOException
                {
                    Files.delete( file );
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory( Path dir, IOException exc )
                    throws IOException
                {
                    Files.delete( dir );
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
        catch ( IOException e )
        {
            // noop, we did our best to clean it up
        }

        return moduleNames;
    }

    public static void main( String[] args )
    {
        Properties properties = new Properties();

        for ( String path : args )
        {
            String moduleName = getModuleName( Paths.get( path ) );
            if ( moduleName != null )
            {
                properties.setProperty( path, moduleName );
            }
        }

        try
        {
            properties.store( System.out, "" );
        }
        catch ( IOException e )
        {
            System.exit( 1 );
        }
    }

    public static String getModuleName( Path modulePath )
    {
        String name = null;
        try
        {
            // Use Java9 code to get moduleName, don't try to do it better with own implementation
            Class<?> moduleFinderClass = Class.forName( "java.lang.module.ModuleFinder" );

            Method ofMethod = moduleFinderClass.getMethod( "of", java.nio.file.Path[].class );
            Object moduleFinderInstance =
                ofMethod.invoke( null, new Object[] { new java.nio.file.Path[] { modulePath } } );

            Method findAllMethod = moduleFinderClass.getMethod( "findAll" );

            @SuppressWarnings( "unchecked" )
            Set<Object> moduleReferences = (Set<Object>) findAllMethod.invoke( moduleFinderInstance );

            if ( moduleReferences.isEmpty() )
            {
                return null;
            }

            Object moduleReference = moduleReferences.iterator().next();
            Method descriptorMethod = moduleReference.getClass().getMethod( "descriptor" );
            Object moduleDescriptorInstance = descriptorMethod.invoke( moduleReference );

            Method nameMethod = moduleDescriptorInstance.getClass().getMethod( "name" );
            name = (String) nameMethod.invoke( moduleDescriptorInstance );
        }
        catch ( ReflectiveOperationException e )
        {
            // noop
        }
        catch ( SecurityException e )
        {
            // noop
        }
        catch ( IllegalArgumentException e )
        {
            // noop
        }
        return name;
    }
}
