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
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleExportInfo;
import java.lang.classfile.attribute.ModuleProvidesInfo;
import java.lang.classfile.attribute.ModuleRequiresInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaRequires.JavaModifier;

/**
 * Extract information from module using the Class File API
 *
 * @author Robert Scholte
 * @since 1.5.0
 */
class ClassFileApiModuleInfoParser extends AbstractBinaryModuleInfoParser {
    @Override
    JavaModuleDescriptor parse(InputStream in) throws IOException {
        byte[] bytes = in.readAllBytes();
        ClassModel classModel = ClassFile.of().parse(bytes);

        ModuleAttribute moduleAttr = classModel
                .findAttribute(java.lang.classfile.Attributes.module())
                .orElseThrow(() -> new IOException("Not a module-info class file"));

        JavaModuleDescriptor.Builder builder =
                JavaModuleDescriptor.newModule(moduleAttr.moduleName().name().stringValue());

        // Process requires
        for (ModuleRequiresInfo requiresInfo : moduleAttr.requires()) {
            String moduleName = requiresInfo.requires().name().stringValue();
            int flags = requiresInfo.requiresFlagsMask();

            boolean isStatic = (flags & ClassFile.ACC_STATIC_PHASE) != 0;
            boolean isTransitive = (flags & ClassFile.ACC_TRANSITIVE) != 0;

            if (isStatic || isTransitive) {
                Set<JavaModifier> modifiers = new LinkedHashSet<>();
                if (isStatic) {
                    modifiers.add(JavaModifier.STATIC);
                }
                if (isTransitive) {
                    modifiers.add(JavaModifier.TRANSITIVE);
                }
                builder.requires(modifiers, moduleName);
            } else {
                builder.requires(moduleName);
            }
        }

        // Process exports
        for (ModuleExportInfo exportInfo : moduleAttr.exports()) {
            String packageName =
                    exportInfo.exportedPackage().name().stringValue().replace('/', '.');
            if (exportInfo.exportsTo().isEmpty()) {
                builder.exports(packageName);
            } else {
                Set<String> targets = new HashSet<>();
                exportInfo
                        .exportsTo()
                        .forEach(target -> targets.add(target.name().stringValue()));
                builder.exports(packageName, targets);
            }
        }

        // Process uses
        moduleAttr.uses().forEach(usesInfo -> {
            String serviceName = usesInfo.name().stringValue().replace('/', '.');
            builder.uses(serviceName);
        });

        // Process provides
        for (ModuleProvidesInfo providesInfo : moduleAttr.provides()) {
            String serviceName = providesInfo.provides().name().stringValue().replace('/', '.');
            List<String> providers = new ArrayList<>();
            providesInfo
                    .providesWith()
                    .forEach(provider ->
                            providers.add(provider.name().stringValue().replace('/', '.')));
            builder.provides(serviceName, providers);
        }

        return builder.build();
    }
}
