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
import java.util.ArrayList;
import java.util.Collection;

/**
 * Contains all information required to analyze the project
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class ResolvePathsRequest
{
    private File jdkHome;
    
    private JavaModuleDescriptor mainModuleDescriptor;
    
    private Collection<File> pathElements = new ArrayList<>();

    public JavaModuleDescriptor getMainModuleDescriptor()
    {
        return mainModuleDescriptor;
    }

    /**
     * 
     * @param mainModuleDescriptor
     * @return this request
     */
    public ResolvePathsRequest setMainModuleDescriptor( JavaModuleDescriptor mainModuleDescriptor )
    {
        this.mainModuleDescriptor = mainModuleDescriptor;
        return this;
    }

    public Collection<File> getPathElements()
    {
        
        return pathElements;
    }
    
    /**
     * All required jars and outputDirectories 
     * 
     * @param pathElements
     * @return this request
     */
    public ResolvePathsRequest setPathElements( Collection<File> pathElements )
    {
        this.pathElements = new ArrayList<>( pathElements );
        return this;
    }
    
    public void setJdkHome( File jdkHome )
    {
        this.jdkHome = jdkHome;
    }
    
    public File getJdkHome()
    {
        return jdkHome;
    }
    
}
