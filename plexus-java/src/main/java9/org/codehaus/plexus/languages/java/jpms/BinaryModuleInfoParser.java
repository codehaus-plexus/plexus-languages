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
import java.io.InputStream;

import java.lang.module.ModuleDescriptor;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.Builder;

class BinaryModuleInfoParser extends AbstractBinaryModuleInfoParser
{
    @Override
    JavaModuleDescriptor parse( InputStream in ) throws IOException
    {
        ModuleDescriptor descriptor = ModuleDescriptor.read( in );
        
        Builder builder = JavaModuleDescriptor.newModule( descriptor.name() );
        
        for ( ModuleDescriptor.Requires requires : descriptor.requires() )
        {
            if ( requires.modifiers().contains( ModuleDescriptor.Requires.Modifier.STATIC )
                || requires.modifiers().contains( ModuleDescriptor.Requires.Modifier.TRANSITIVE ) )
            {
                Set<JavaModuleDescriptor.JavaRequires.JavaModifier> modifiers = new LinkedHashSet<>();
                if ( requires.modifiers().contains( ModuleDescriptor.Requires.Modifier.STATIC ) )
                {
                    modifiers.add( org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC );
                }
                if ( requires.modifiers().contains( ModuleDescriptor.Requires.Modifier.TRANSITIVE ) )
                {
                    modifiers.add( org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier.TRANSITIVE );
                }
                builder.requires( modifiers, requires.name() );
            }
            else
            {
                builder.requires( requires.name() );
            }
        }
        
        for ( ModuleDescriptor.Exports exports : descriptor.exports() )
        {
            if ( exports.targets().isEmpty() )
            {
                builder.exports( exports.source() );
            }
            else
            {
                builder.exports( exports.source(), exports.targets() );
            }
        }
        
        for ( String uses : descriptor.uses() )
        {
            builder.uses( uses );
        }
        
        for ( ModuleDescriptor.Provides provides : descriptor.provides() )
        {
            builder.provides( provides.service(), provides.providers() );
        }
        
        
        return builder.build();
    }
}
