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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the results of the project analyzer
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class ResolvePathsResult<T>
{
    /**
     * Source of the modulename 
     */
    public enum ModuleNameSource
    {
        FILENAME, MANIFEST, MODULEDESCRIPTOR
    }
    
    private JavaModuleDescriptor mainModuleDescriptor;
    
    /**
     * Ordered map, respects the classpath order
     */
    private Map<T, JavaModuleDescriptor> pathElements;
    
    private Map<T, ModuleNameSource> modulepathElements = new LinkedHashMap<>();
    
    private Collection<T> classpathElements = new ArrayList<>();
    
    private Map<T, Exception> pathExceptions = new HashMap<>();
    
    void setMainModuleDescriptor( JavaModuleDescriptor mainModuleDescriptor )
    {
        this.mainModuleDescriptor = mainModuleDescriptor;
    }

    public JavaModuleDescriptor getMainModuleDescriptor()
    {
        return mainModuleDescriptor;
    }

    void setPathElements( Map<T, JavaModuleDescriptor> pathElements )
    {
        this.pathElements = pathElements;
    }
    
    /**
     * Ordered map, respects the classpath order
     */
    public Map<T, JavaModuleDescriptor> getPathElements()
    {
        return pathElements;
    }
    
    void setClasspathElements( Collection<T> classpathElements )
    {
        this.classpathElements = classpathElements;
    }
    
    public Collection<T> getClasspathElements()
    {
        return classpathElements;
    }
    
    void setModulepathElements( Map<T, ModuleNameSource> modulepathElements )
    {
        this.modulepathElements = modulepathElements;
    }
    
    public Map<T, ModuleNameSource> getModulepathElements()
    {
        return modulepathElements;
    }

    void setPathExceptions( Map<T, Exception> pathExceptions )
    {
        this.pathExceptions = pathExceptions;
    }

    public Map<T, Exception> getPathExceptions()
    {
        return pathExceptions;
    }
}
