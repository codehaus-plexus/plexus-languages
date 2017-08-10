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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Set;

/**
 * This class is could be replaced with a Java 9 MultiRelease implementation 
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class ReflectModuleInfoParser implements ModuleInfoParser
{
    @Override
    public JavaModuleDescriptor getModuleDescriptor( Path modulePath )
        throws IOException
    {
        JavaModuleDescriptor moduleDescriptor = null;
        
        try
        {
            // Use Java9 code to get moduleName, don't try to do it better with own implementation
            Class moduleFinderClass = Class.forName( "java.lang.module.ModuleFinder" );

            Method ofMethod = moduleFinderClass.getMethod( "of", java.nio.file.Path[].class );
            Object moduleFinderInstance = ofMethod.invoke( null, new Object[] { new java.nio.file.Path[] { modulePath } } );

            Method findAllMethod = moduleFinderClass.getMethod( "findAll" );
            Set<Object> moduleReferences = (Set<Object>) findAllMethod.invoke( moduleFinderInstance );

            if ( moduleReferences.isEmpty() )
            {
                return null;
            }
            Object moduleReference = moduleReferences.iterator().next();
            Method descriptorMethod = moduleReference.getClass().getMethod( "descriptor" );
            Object moduleDescriptorInstance = descriptorMethod.invoke( moduleReference );

            JavaModuleDescriptor.Builder builder = getBuilder( moduleDescriptorInstance );
            
            Method requiresMethod = moduleDescriptorInstance.getClass().getMethod( "requires" );
            Set<Object> requires = (Set<Object>) requiresMethod.invoke( moduleDescriptorInstance );
            
            if ( requires != null )
            {
                for ( Object requiresInstance : requires )
                {
                    Method nameMethod = requiresInstance.getClass().getMethod( "name" );
                    String name = (String) nameMethod.invoke( requiresInstance );
                    
                    builder.requires( name );
                }
            }

            Method exportsMethod = moduleDescriptorInstance.getClass().getMethod( "exports" );
            Set<Object> exports = (Set<Object>) exportsMethod.invoke( moduleDescriptorInstance );
            
            if( exports != null )
            {
                for ( Object exportsInstance : exports )
                {
                    Method sourceMethod = exportsInstance.getClass().getMethod( "source" );
                    String source = (String) sourceMethod.invoke( exportsInstance );

                    Method targetsMethod = exportsInstance.getClass().getMethod( "targets" );
                    Set<String> targets = (Set<String>) targetsMethod.invoke( exportsInstance );

                    if ( targets.isEmpty() )
                    {
                        builder.exports( source );
                    }
                    else
                    {
                        builder.exports( source, targets );
                    }
                }
            }
            
            moduleDescriptor = builder.build();
        }
        catch ( ReflectiveOperationException e )
        {
            e.printStackTrace();
            // do nothing
        }
        catch ( SecurityException e )
        {
            e.printStackTrace();
            // do nothing
        }
        catch ( IllegalArgumentException e )
        {
            e.printStackTrace();
            // do nothing
        }
        return moduleDescriptor;
    }

    private JavaModuleDescriptor.Builder getBuilder( Object moduleDescriptorInstance )
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        JavaModuleDescriptor.Builder builder;
        Method nameMethod = moduleDescriptorInstance.getClass().getMethod( "name" );
        String name = (String) nameMethod.invoke( moduleDescriptorInstance );
        
        Method isAutomaticMethod = moduleDescriptorInstance.getClass().getMethod( "isAutomatic" );
        boolean automatic = (Boolean) isAutomaticMethod.invoke( moduleDescriptorInstance );

        if ( automatic )
        {
            builder = JavaModuleDescriptor.newAutomaticModule( name );
        }
        else
        {
            builder = JavaModuleDescriptor.newModule( name );
        }
        return builder;
    }

}
