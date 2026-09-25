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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
class AsmModuleInfoParser extends AbstractBinaryModuleInfoParser {

    // The highest class file major version the ASM release on the classpath knows how to read.
    // Opcodes.Vxx values encode the minor version in the upper 16 bits, but for these constants
    // that is always 0, so the constant itself is the plain major version number.
    private static final int NEWEST_MAJOR_VERSION_SUPPORTED_BY_ASM = Opcodes.V27;

    @Override
    JavaModuleDescriptor parse(InputStream in) throws IOException {
        final JavaModuleDescriptorWrapper wrapper = new JavaModuleDescriptorWrapper();

        byte[] classBytes = toByteArray(in);

        // A class file compiled by a newer JDK than this ASM release supports (e.g. class file
        // major version 72 for JDK 28, while ASM 9.10.1 only understands up to 71/V27) makes
        // ClassReader throw IllegalArgumentException before it even looks at the content. The
        // module-info.class Module attribute format hasn't changed since Java 9, so it's safe to
        // clamp the major version down to the newest one ASM supports before handing it the bytes.
        int majorVersion = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
        if (majorVersion > NEWEST_MAJOR_VERSION_SUPPORTED_BY_ASM) {
            classBytes[6] = (byte) (NEWEST_MAJOR_VERSION_SUPPORTED_BY_ASM >>> 8);
            classBytes[7] = (byte) NEWEST_MAJOR_VERSION_SUPPORTED_BY_ASM;
        }

        ClassReader reader = new ClassReader(classBytes);
        reader.accept(
                new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public ModuleVisitor visitModule(String name, int arg1, String arg2) {
                        wrapper.builder = JavaModuleDescriptor.newModule(name);

                        return new ModuleVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitRequire(String module, int access, String version) {
                                if ((access & (Opcodes.ACC_STATIC_PHASE | Opcodes.ACC_TRANSITIVE)) != 0) {
                                    Set<JavaModuleDescriptor.JavaRequires.JavaModifier> modifiers =
                                            new LinkedHashSet<>();
                                    if ((access & Opcodes.ACC_STATIC_PHASE) != 0) {
                                        modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC);
                                    }
                                    if ((access & Opcodes.ACC_TRANSITIVE) != 0) {
                                        modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.TRANSITIVE);
                                    }

                                    wrapper.builder.requires(modifiers, module);
                                } else {
                                    wrapper.builder.requires(module);
                                }
                            }

                            @Override
                            public void visitExport(String pn, int ms, String... targets) {
                                if (targets == null || targets.length == 0) {
                                    wrapper.builder.exports(pn.replace('/', '.'));
                                } else {
                                    wrapper.builder.exports(
                                            pn.replace('/', '.'), new HashSet<>(Arrays.asList(targets)));
                                }
                            }

                            @Override
                            public void visitUse(String service) {
                                wrapper.builder.uses(service.replace('/', '.'));
                            }

                            @Override
                            public void visitProvide(String service, String... providers) {
                                List<String> renamedProvides = new ArrayList<>(providers.length);
                                for (String provider : providers) {
                                    renamedProvides.add(provider.replace('/', '.'));
                                }
                                wrapper.builder.provides(service.replace('/', '.'), renamedProvides);
                            }
                        };
                    }
                },
                0);
        return wrapper.builder.build();
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static class JavaModuleDescriptorWrapper {
        private JavaModuleDescriptor.Builder builder;
    }
}
