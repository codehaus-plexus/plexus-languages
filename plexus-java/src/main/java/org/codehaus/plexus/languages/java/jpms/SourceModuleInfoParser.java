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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;

/**
 * Extracts information from a source module descriptor.
 *
 * @author Robert Scholte
 * @since 1.0.0
 */
class SourceModuleInfoParser {

    public JavaModuleDescriptor fromSourcePath(Path modulePath) throws IOException {
        JavaModuleDescriptor.Builder builder;
        if (Files.exists(modulePath)) {
            ModuleDeclaration descriptor = StaticJavaParser.parse(modulePath)
                    .getModule()
                    .orElseThrow(() -> new IOException("Module declaration not found in " + modulePath));

            builder = JavaModuleDescriptor.newModule(descriptor.getName().asString());

            for (ModuleDirective directive : descriptor.getDirectives()) {
                if (directive instanceof ModuleRequiresDirective) {
                    addRequires(builder, (ModuleRequiresDirective) directive);
                } else if (directive instanceof ModuleExportsDirective) {
                    addExports(builder, (ModuleExportsDirective) directive);
                } else if (directive instanceof ModuleUsesDirective) {
                    ModuleUsesDirective uses = (ModuleUsesDirective) directive;
                    builder.uses(uses.getName().asString());
                } else if (directive instanceof ModuleProvidesDirective) {
                    ModuleProvidesDirective provides = (ModuleProvidesDirective) directive;
                    List<String> providers = new ArrayList<>(provides.getWith().size());
                    for (Name provider : provides.getWith()) {
                        providers.add(provider.asString());
                    }
                    builder.provides(provides.getName().asString(), providers);
                }
            }
        } else {
            builder = JavaModuleDescriptor.newAutomaticModule(null);
        }

        return builder.build();
    }

    private static void addRequires(JavaModuleDescriptor.Builder builder, ModuleRequiresDirective requires) {
        if (requires.isStatic() || requires.isTransitive()) {
            Set<JavaModuleDescriptor.JavaRequires.JavaModifier> modifiers = new LinkedHashSet<>(2);
            if (requires.isStatic()) {
                modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC);
            }
            if (requires.isTransitive()) {
                modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.TRANSITIVE);
            }
            builder.requires(modifiers, requires.getName().asString());
        } else {
            builder.requires(requires.getName().asString());
        }
    }

    private static void addExports(JavaModuleDescriptor.Builder builder, ModuleExportsDirective exports) {
        if (exports.getModuleNames().isEmpty()) {
            builder.exports(exports.getName().asString());
        } else {
            Set<String> targets = new LinkedHashSet<>();
            for (Name module : exports.getModuleNames()) {
                targets.add(module.asString());
            }
            builder.exports(exports.getName().asString(), targets);
        }
    }
}
