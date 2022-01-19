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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaModule;
import com.thoughtworks.qdox.model.JavaModuleDescriptor;

/**
 * Extract information from module with QDox
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
class SourceModuleInfoParser
{

    public org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor fromSourcePath( Path modulePath )
                    throws IOException
    {
        File moduleDescriptor = modulePath.toFile();

        org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.Builder builder;
        if ( moduleDescriptor.exists() )
        {
            JavaModuleDescriptor descriptor = new JavaProjectBuilder().addSourceFolder( moduleDescriptor.getParentFile() ).getDescriptor();

            builder = org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.newModule( descriptor.getName() );
            
            for ( JavaModuleDescriptor.JavaRequires requires : descriptor.getRequires() )
            {
                if ( requires.isStatic() || requires.isTransitive() )
                {
                    Set<org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier> modifiers =
                        new LinkedHashSet<>( 2 );
                    if ( requires.isStatic() )
                    {
                        modifiers.add( org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC );
                    }
                    if ( requires.isTransitive() )
                    {
                        modifiers.add( org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier.TRANSITIVE );
                    }
                    builder.requires( modifiers , requires.getModule().getName() );
                }
                else
                {
                    builder.requires( requires.getModule().getName() );
                }
            }
            
            for ( JavaModuleDescriptor.JavaExports exports : descriptor.getExports() )
            {
                if ( exports.getTargets().isEmpty()  )
                {
                    builder.exports( exports.getSource().getName() );
                }
                else
                {
                    Set<String> targets = new LinkedHashSet<>();
                    for ( JavaModule module : exports.getTargets() )
                    {
                        targets.add( module.getName() );
                    }
                    builder.exports( exports.getSource().getName(), targets );
                }
            }
            
            for ( JavaModuleDescriptor.JavaUses uses : descriptor.getUses() )
            {
                builder.uses( uses.getService().getName() );
            }
            
            for ( JavaModuleDescriptor.JavaProvides provides : descriptor.getProvides() )
            {
                List<String> providers = new ArrayList<>( provides.getProviders().size() );
                for ( JavaClass provider : provides.getProviders() )
                {
                    providers.add( provider.getName() );
                }
                
                builder.provides( provides.getService().getName(), providers );
            }
        }
        else
        {
            builder = org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.newAutomaticModule( null );
        }

        return builder.build();
    }

}
