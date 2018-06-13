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

import javax.inject.Singleton;

import org.codehaus.plexus.component.annotations.Component;

/**
 * Maps artifacts to modules and analyzes the type of required modules
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
@Singleton
@Component( role = LocationManager.class )
public class LocationManager
{
    private ModuleInfoParser binaryParser;
    
    private SourceModuleInfoParser sourceParser;

    private ManifestModuleNameExtractor manifestModuleNameExtractor;
    
    public LocationManager()
    {
        this.binaryParser = new BinaryModuleInfoParser();
        this.sourceParser = new SourceModuleInfoParser();
        this.manifestModuleNameExtractor = new ManifestModuleNameExtractor();
    }
    
    LocationManager( ModuleInfoParser binaryParser, SourceModuleInfoParser sourceParser )
    {
        this.binaryParser = binaryParser;
        this.sourceParser = sourceParser;
        this.manifestModuleNameExtractor = new ManifestModuleNameExtractor();
    }

    public <T> ResolvePathResult resolvePath( final ResolvePathRequest<T> request  ) throws Exception
    {
        ModuleNameExtractor filenameExtractor = new ModuleNameExtractor()
        {
            MainClassModuleNameExtractor extractor = new MainClassModuleNameExtractor( request.getJdkHome() );
            
            @Override
            public String extract( Path file )
                throws IOException
            {
                if ( request.getJdkHome() != null )
                {
                    return extractor.extract( Collections.singletonMap( file, file ) ).get( file );
                }
                else
                {
                    return CmdModuleNameExtractor.getModuleName( file );
                }
            }
        };
        
        return resolvePath( request.toPath( request.getPathElement() ), filenameExtractor );
    }
    
    /**
     * Decide for every {@code request.getPathElements()} if it belongs to the modulePath or classPath, based on the
     * {@code request.getMainModuleDescriptor()}.
     * 
     * @param request the paths to resolve
     * @return the result of the resolution
     * @throws IOException if a critical IOException occurs
     */
    public <T> ResolvePathsResult<T> resolvePaths( final ResolvePathsRequest<T> request )
        throws IOException
    {
        final ResolvePathsResult<T> result = request.createResult();
        
        Map<T, JavaModuleDescriptor> pathElements = new LinkedHashMap<>( request.getPathElements().size() );

        JavaModuleDescriptor mainModuleDescriptor;
        
        Path descriptorPath = request.getMainModuleDescriptor();
        
        if ( descriptorPath != null )
        {
            if ( descriptorPath.endsWith( "module-info.java" ) )
            {
                mainModuleDescriptor = sourceParser.fromSourcePath( descriptorPath );
            }
            else if ( descriptorPath.endsWith( "module-info.class" ) )
            {
                mainModuleDescriptor = binaryParser.getModuleDescriptor( descriptorPath.getParent() );
            }
            else
            {
                throw new IOException( "Invalid path to module descriptor: " + descriptorPath );
            }
        }
        else
        {
            mainModuleDescriptor = null;
        }

        Map<String, JavaModuleDescriptor> availableNamedModules = new HashMap<>(); 
        
        Map<String, ModuleNameSource> moduleNameSources = new HashMap<>();
        
        // start from root
        result.setMainModuleDescriptor( mainModuleDescriptor );
        
        final Map<T, Path> filenameAutoModules = new HashMap<>();
        
        // collect all modules from path
        for ( final T t : request.getPathElements() )
        {
            JavaModuleDescriptor moduleDescriptor;
            ModuleNameSource source;
            
            ModuleNameExtractor nameExtractor = new ModuleNameExtractor()
            {
                @Override
                public String extract( Path path )
                    throws IOException
                {
                    if ( request.getJdkHome() != null )
                    {
                        filenameAutoModules.put( t, path );
                    }
                    else
                    {
                        return CmdModuleNameExtractor.getModuleName( path );
                    }
                    return null;
                }
            };
           
            try
            {
                ResolvePathResult resolvedPath = resolvePath( request.toPath( t ), nameExtractor );
                
                moduleDescriptor = resolvedPath.getModuleDescriptor();

                source = resolvedPath.getModuleNameSource();
            }
            catch ( Exception e )
            {
                result.getPathExceptions().put( t, e );
                continue;
            }
            
            if ( moduleDescriptor != null )
            {
                moduleNameSources.put( moduleDescriptor.name(), source );
                
                availableNamedModules.put( moduleDescriptor.name(), moduleDescriptor );
            }
            
            pathElements.put( t, moduleDescriptor );
            
        }
        result.setPathElements( pathElements );
        
        if ( !filenameAutoModules.isEmpty() ) 
        {
            MainClassModuleNameExtractor extractor = new MainClassModuleNameExtractor( request.getJdkHome() );
            
            Map<T, String> automodules = extractor.extract( filenameAutoModules );
            
            for ( Map.Entry<T, String> entry : automodules.entrySet() )
            {
                String moduleName = entry.getValue();
                
                if ( moduleName != null )
                {
                    JavaModuleDescriptor moduleDescriptor = JavaModuleDescriptor.newAutomaticModule( moduleName ).build();
                    
                    moduleNameSources.put( moduleDescriptor.name(), ModuleNameSource.FILENAME );
                    
                    availableNamedModules.put( moduleDescriptor.name(), moduleDescriptor );
                    
                    pathElements.put( entry.getKey(), moduleDescriptor );
                }
            }
        }
        
        if ( mainModuleDescriptor != null )
        {
            Set<String> requiredNamedModules = new HashSet<>();
            
            requiredNamedModules.add( mainModuleDescriptor.name() );
            
            requiredNamedModules.addAll( request.getAdditionalModules() );
            
            select( mainModuleDescriptor, Collections.unmodifiableMap( availableNamedModules ), requiredNamedModules );

            // in case of identical module names, first one wins
            Set<String> collectedModules = new HashSet<>( requiredNamedModules.size() );

            for ( Entry<T, JavaModuleDescriptor> entry : pathElements.entrySet() )
            {
                if ( entry.getValue() != null && requiredNamedModules.contains( entry.getValue().name() ) )
                {
                    if ( collectedModules.add( entry.getValue().name() ) )
                    {
                        result.getModulepathElements().put( entry.getKey(),
                                                            moduleNameSources.get( entry.getValue().name() ) );
                    }
                }
                else
                {
                    result.getClasspathElements().add( entry.getKey() );
                }
            }
        }

        return result;
    }

    private ResolvePathResult resolvePath( Path path, ModuleNameExtractor fileModulenameExtractor ) throws Exception
    {
        ResolvePathResult result = new ResolvePathResult();
        JavaModuleDescriptor moduleDescriptor = null;
        
        // either jar or outputDirectory
        if ( Files.isRegularFile( path ) || Files.exists( path.resolve( "module-info.class" ) ) )
        {
            moduleDescriptor = binaryParser.getModuleDescriptor( path );
        }

        if ( moduleDescriptor != null ) 
        {
            result.setModuleNameSource( ModuleNameSource.MODULEDESCRIPTOR );
        }
        else
        {
            String moduleName = manifestModuleNameExtractor.extract( path );

            if ( moduleName != null )
            {
                result.setModuleNameSource( ModuleNameSource.MANIFEST );
            }
            else 
            {
                moduleName = fileModulenameExtractor.extract( path );
                
                if ( moduleName != null )
                {
                    result.setModuleNameSource( ModuleNameSource.FILENAME );
                }
            }

            if ( moduleName != null )
            {
                moduleDescriptor = JavaModuleDescriptor.newAutomaticModule( moduleName ).build();
            }
        }
        
        result.setModuleDescriptor( moduleDescriptor );
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
