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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

/**
 * Contains all information required to analyze the project
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public abstract class ResolvePathsRequest<T>
{
    private Path jdkHome;

    private Path mainModuleDescriptor;

    private Collection<T> pathElements;

    private Collection<String> additionalModules;

    private ResolvePathsRequest()
    {
    }

    public static ResolvePathsRequest<File> withFiles( Collection<File> files )
    {
        ResolvePathsRequest<File> request = new ResolvePathsRequest<File>()
        {
            @Override
            protected Path toPath( File t )
            {
                return t.toPath();
            }
        };

        request.pathElements = files;
        return request;
    }

    public static ResolvePathsRequest<Path> withPaths( Collection<Path> paths )
    {
        ResolvePathsRequest<Path> request = new ResolvePathsRequest<Path>() {
            @Override
            protected Path toPath( Path t )
            {
                return t;
            }
        };
        request.pathElements = paths;
        return request;
    }

    public static ResolvePathsRequest<String> withStrings( Collection<String> strings )
    {
        ResolvePathsRequest<String> request = new ResolvePathsRequest<String>() {
            @Override
            protected Path toPath( String t )
            {
                return Paths.get( t );
            }
        };
        request.pathElements = strings;
        return request;
    }

    protected abstract Path toPath( T t );

    final ResolvePathsResult<T> createResult() {
        return new ResolvePathsResult<>();
    }

    public Path getMainModuleDescriptor()
    {
        return mainModuleDescriptor;
    }

    /**
     * Must be either {@code module-info.java} or {@code module-info.class} 
     * 
     * @param mainModuleDescriptor
     * @return this request
     */
    public ResolvePathsRequest<T> setMainModuleDescriptor( T mainModuleDescriptor )
    {
        this.mainModuleDescriptor = toPath( mainModuleDescriptor );
        return this;
    }

    public Collection<T> getPathElements()
    {
        return pathElements;
    }

    /**
     * In case the JRE is Java 8 or before, this jdkHome is used to extract the module name.
     * 
     * @param jdkHome
     */
    public ResolvePathsRequest<T> setJdkHome( T jdkHome )
    {
        this.jdkHome = toPath( jdkHome );
        return this;
    }

    public Path getJdkHome()
    {
        return jdkHome;
    }

    public ResolvePathsRequest<T> setAdditionalModules( Collection<String> additionalModules )
    {
        this.additionalModules = additionalModules;
        return this;
    }

    public Collection<String> getAdditionalModules()
    {
        if ( additionalModules == null )
        {
            additionalModules = Collections.emptyList();
        }
        return additionalModules;
    }
}
