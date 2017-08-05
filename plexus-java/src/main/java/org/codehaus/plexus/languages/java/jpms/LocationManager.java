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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult.ModuleNameSource;

/**
 * Maps artifacts to modules and analyzes the type of required modules
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class LocationManager
{
    private ModuleInfoParser asmParser;

    private ModuleInfoParser reflectParser;
    
    public LocationManager()
    {
        this.asmParser = new AsmModuleInfoParser();
        this.reflectParser = new ReflectModuleInfoParser();
    }
    
    LocationManager( ModuleInfoParser asmParser, ModuleInfoParser reflectParser )
    {
        this.asmParser = asmParser;
        this.reflectParser = reflectParser;
    }

    public <T> ResolvePathsResult<T> resolvePaths( ResolvePathsRequest<T> request )
        throws IOException
    {
        ResolvePathsResult<T> result = request.createResult();
        
        Map<T, JavaModuleDescriptor> pathElements = new LinkedHashMap<>( request.getPathElements().size() );

        JavaModuleDescriptor mainModuleDescriptor = request.getMainModuleDescriptor();

        Map<String, JavaModuleDescriptor> availableNamedModules = new HashMap<>(); 
        
        Map<String, ModuleNameSource> moduleNameSources = new HashMap<>();
        
        // start from root
        result.setMainModuleDescriptor( mainModuleDescriptor );

        // collect all modules from path
        for ( T t : request.getPathElements() )
        {
            Path path =  request.toPath( t );
            File file = path.toFile();
            
            JavaModuleDescriptor moduleDescriptor = null;
            
            // either jar or outputDirectory
            if ( Files.isRegularFile( path ) || Files.exists( path.resolve( "module-info.class" ) ) )
            {
                moduleDescriptor = reflectParser.getModuleDescriptor( file );
                
                if ( moduleDescriptor == null )
                {
                    moduleDescriptor = asmParser.getModuleDescriptor( file );
                }
            }

            ModuleNameSource source = null;
            if ( moduleDescriptor == null )
            {
                String moduleName = new ManifestModuleNameExtractor().extract( file );

                if ( moduleName != null )
                {
                    source = ModuleNameSource.MANIFEST;
                }
                else
                {
                    moduleName = new JarModuleNameExtractor( request.getJdkHome() ).extract( file );
                    
                    if ( moduleName != null )
                    {
                        source = ModuleNameSource.MANIFEST;
                    }
                }

                if ( moduleName != null )
                {
                    moduleDescriptor = JavaModuleDescriptor.newAutomaticModule( moduleName ).build();
                }
            }
            else if ( !moduleDescriptor.isAutomatic() )
            {
                source = ModuleNameSource.MODULEDESCRIPTOR;
            }
            
            if ( moduleDescriptor != null )
            {
                if ( source == null )
                {
                    // name retrieved from java.lang.module.ModuleFinder, source unknown
                    String moduleName = new ManifestModuleNameExtractor().extract( file );

                    if ( moduleName != null )
                    {
                        source = ModuleNameSource.MANIFEST;
                    }
                    else
                    {
                        source = ModuleNameSource.FILENAME;
                    }
                }

                moduleNameSources.put( moduleDescriptor.name(), source );
                
                availableNamedModules.put( moduleDescriptor.name(), moduleDescriptor );
            }
            
            pathElements.put( t, moduleDescriptor );
            
        }
        result.setPathElements( pathElements );
        
        if ( mainModuleDescriptor != null )
        {
            Set<String> requiredNamedModules = new HashSet<>();
            
            select( mainModuleDescriptor, Collections.unmodifiableMap( availableNamedModules ), requiredNamedModules );

            for ( Entry<T, JavaModuleDescriptor> entry : pathElements.entrySet() )
            {
                if ( entry.getValue() != null && requiredNamedModules.contains( entry.getValue().name() ) )
                {
                    result.getModulepathElements().put( entry.getKey(), moduleNameSources.get( entry.getValue().name() ) );
                }
                else
                {
                    result.getClasspathElements().add( entry.getKey() );
                }
            }
        }

        return result;
    }

    

    private void select( JavaModuleDescriptor module, Map<String, JavaModuleDescriptor> availableModules,
                         Set<String> namedModules )
    {
        for ( JavaModuleDescriptor.JavaRequires requires : module.requires() )
        {
            String requiresName = requires.name();
            JavaModuleDescriptor requiredModule = availableModules.get( requiresName );

            if ( requiredModule != null && namedModules.add( requiresName ) )
            {
                select( requiredModule, availableModules, namedModules );
            }
        }
    }
}
