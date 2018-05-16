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

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Collections;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.Builder;

class BinaryModuleInfoParser implements ModuleInfoParser
{
    @Override
    public JavaModuleDescriptor getModuleDescriptor( Path modulePath ) 
    {
        ModuleReference moduleReference = ModuleFinder.of( modulePath ).findAll().iterator().next();
        
        ModuleDescriptor descriptor = moduleReference.descriptor();
        
        Builder builder = JavaModuleDescriptor.newModule( descriptor.name() );
        
        for ( ModuleDescriptor.Requires requires : descriptor.requires() )
        {
            if ( requires.modifiers().contains( ModuleDescriptor.Requires.Modifier.STATIC ) )
            {
                builder.requires​( Collections.singleton( org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC ),
                                  requires.name() );
            }
            else
            {
                builder.requires( requires.name() );
            }
        }
        
        return builder.build();
    }
}
