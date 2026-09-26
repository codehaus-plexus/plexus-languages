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
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleExportInfo;
import java.lang.classfile.attribute.ModuleProvideInfo;
import java.lang.classfile.attribute.ModuleRequireInfo;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.ModuleEntry;
import java.lang.reflect.AccessFlag;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.Builder;

/**
 * Extract information from a module descriptor with the {@code java.lang.classfile} API
 * (JEP 484), final since Java 24. Multi-release counterpart of the {@code src/main/java9}
 * {@code BinaryModuleInfoParser}, which reads the same information with
 * {@code java.lang.module.ModuleDescriptor}.
 *
 * @see <a href="https://openjdk.org/jeps/484">JEP 484: Class-File API</a>
 * @see <a href="https://github.com/codehaus-plexus/plexus-languages/issues/165">plexus-languages#165</a>
 * @since 1.7.0
 */
class BinaryModuleInfoParser extends AbstractBinaryModuleInfoParser {

    // The highest class file major version this JDK's Class-File API knows how to read. A
    // module-info.class produced by a newer toolchain than the running JDK (major version above
    // this) makes ClassFile.of().parse(...) throw IllegalArgumentException, exactly like
    // java.lang.module.ModuleDescriptor.read(...) and, before it was patched, ASM's ClassReader
    // (see AsmModuleInfoParser and codehaus-plexus/plexus-languages#165: "Unsupported class file
    // major version 72" on a JDK that only knows up to 71). The Module attribute format hasn't
    // changed since Java 9, so it's safe to clamp the major version down to the newest one this
    // runtime supports before parsing.
    private static final int NEWEST_MAJOR_VERSION_SUPPORTED = ClassFile.latestMajorVersion();

    @Override
    JavaModuleDescriptor parse(InputStream in) throws IOException {
        byte[] classBytes = toByteArray(in);

        // bytes 6-7 are the big-endian major version
        int majorVersion = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
        if (majorVersion > NEWEST_MAJOR_VERSION_SUPPORTED) {
            classBytes[6] = (byte) (NEWEST_MAJOR_VERSION_SUPPORTED >>> 8);
            classBytes[7] = (byte) NEWEST_MAJOR_VERSION_SUPPORTED;
        }

        ClassModel classModel = ClassFile.of().parse(classBytes);
        ModuleAttribute moduleAttribute = classModel
                .findAttribute(Attributes.module())
                .orElseThrow(() -> new IOException("Not a module-info.class: no Module attribute found"));

        Builder builder = JavaModuleDescriptor.newModule(
                moduleAttribute.moduleName().name().stringValue());

        for (ModuleRequireInfo requires : moduleAttribute.requires()) {
            Set<AccessFlag> requiresFlags = requires.requiresFlags();
            if (requiresFlags.contains(AccessFlag.STATIC_PHASE) || requiresFlags.contains(AccessFlag.TRANSITIVE)) {
                Set<JavaModuleDescriptor.JavaRequires.JavaModifier> modifiers = new LinkedHashSet<>();
                if (requiresFlags.contains(AccessFlag.STATIC_PHASE)) {
                    modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC);
                }
                if (requiresFlags.contains(AccessFlag.TRANSITIVE)) {
                    modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.TRANSITIVE);
                }
                builder.requires(modifiers, requires.requires().name().stringValue());
            } else {
                builder.requires(requires.requires().name().stringValue());
            }
        }

        for (ModuleExportInfo exports : moduleAttribute.exports()) {
            String packageName = exports.exportedPackage().name().stringValue().replace('/', '.');
            if (exports.exportsTo().isEmpty()) {
                builder.exports(packageName);
            } else {
                Set<String> targets = exports.exportsTo().stream()
                        .map(ModuleEntry::name)
                        .map(name -> name.stringValue())
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                builder.exports(packageName, targets);
            }
        }

        for (ClassEntry uses : moduleAttribute.uses()) {
            builder.uses(uses.name().stringValue().replace('/', '.'));
        }

        for (ModuleProvideInfo provides : moduleAttribute.provides()) {
            String service = provides.provides().name().stringValue().replace('/', '.');
            List<String> providers = provides.providesWith().stream()
                    .map(ClassEntry::name)
                    .map(name -> name.stringValue().replace('/', '.'))
                    .collect(Collectors.toList());
            builder.provides(service, providers);
        }

        return builder.build();
    }

    @Override
    boolean isClassFileApiBased() {
        return true;
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
}
