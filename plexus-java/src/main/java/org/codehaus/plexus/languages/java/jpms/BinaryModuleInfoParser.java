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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Extract information from module with ASM
 * 
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
class BinaryModuleInfoParser extends AbstractBinaryModuleInfoParser
{
    @Override
    JavaModuleDescriptor parse( InputStream in )
        throws IOException
    {
        final JavaModuleDescriptorWrapper wrapper = new JavaModuleDescriptorWrapper();

        ClassReader reader = new ClassReader( in );
        reader.accept( new ClassVisitor( Opcodes.ASM6 )
        {
            @Override
            public ModuleVisitor visitModule( String name, int arg1, String arg2 )
            {
                wrapper.builder = JavaModuleDescriptor.newModule( name );

                return new ModuleVisitor( Opcodes.ASM6 )
                {
                    @Override
                    public void visitRequire( String module, int access, String version )
                    {
                        if ( ( access & Opcodes.ACC_STATIC_PHASE ) != 0 )
                        {
                            wrapper.builder.requires​( Collections.singleton( JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC ),
                                                       module );
                        }
                        else
                        {
                            wrapper.builder.requires( module );
                        }
                    }

                    @Override
                    public void visitExport( String pn, int ms, String... targets )
                    {
                        if ( targets == null || targets.length == 0 )
                        {
                            wrapper.builder.exports( pn.replace( '/', '.' ) );
                        }
                        else
                        {
                            wrapper.builder.exports( pn.replace( '/', '.' ), new HashSet<>( Arrays.asList( targets ) ) );
                        }
                    }
                    
                    @Override
                    public void visitUse( String service )
                    {
                        wrapper.builder.uses( service.replace( '/', '.' ) );
                    }
                    
                    @Override
                    public void visitProvide( String service, String... providers )
                    {
                        List<String> renamedProvides = new ArrayList<>( providers.length );
                        for ( String provider : providers )
                        {
                            renamedProvides.add( provider.replace( '/', '.' ) );
                        }
                        wrapper.builder.provides​( service.replace( '/', '.' ), renamedProvides );
                    }
                };
            }
        }, 0 );
        return wrapper.builder.build();
    }

    private static class JavaModuleDescriptorWrapper
    {
        private JavaModuleDescriptor.Builder builder;
    }
}
