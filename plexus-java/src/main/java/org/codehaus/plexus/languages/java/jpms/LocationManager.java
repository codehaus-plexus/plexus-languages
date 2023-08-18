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

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaProvides;

/**
 * Maps artifacts to modules and analyzes the type of required modules
 *
 * @author Robert Scholte
 * @since 1.0.0
 */
@Named
@Singleton
public class LocationManager {
    private SourceModuleInfoParser sourceParser;

    private ManifestModuleNameExtractor manifestModuleNameExtractor;

    public LocationManager() {
        this.sourceParser = new SourceModuleInfoParser();
        this.manifestModuleNameExtractor = new ManifestModuleNameExtractor();
    }

    LocationManager(SourceModuleInfoParser sourceParser) {
        this.sourceParser = sourceParser;
        this.manifestModuleNameExtractor = new ManifestModuleNameExtractor();
    }

    /**
     * @param descriptorPath never {@code null}
     * @return the parsed module descriptor
     * @throws IOException when descriptorPath could not be read
     */
    public ResolvePathResult parseModuleDescriptor(Path descriptorPath) throws IOException {
        JavaModuleDescriptor moduleDescriptor;
        if (descriptorPath.endsWith("module-info.java")) {
            moduleDescriptor = sourceParser.fromSourcePath(descriptorPath);
        } else {
            throw new IOException("Invalid path to module descriptor: " + descriptorPath);
        }
        return new ResolvePathResult()
                .setModuleDescriptor(moduleDescriptor)
                .setModuleNameSource(ModuleNameSource.MODULEDESCRIPTOR);
    }

    /**
     * @param descriptorPath never {@code null}
     * @return the parsed module descriptor
     * @throws IOException when descriptorPath could not be read
     */
    public ResolvePathResult parseModuleDescriptor(File descriptorPath) throws IOException {
        return parseModuleDescriptor(descriptorPath.toPath());
    }

    /**
     * @param descriptorPath never {@code null}
     * @return the parsed module descriptor
     * @throws IOException when descriptorPath could not be read
     */
    public ResolvePathResult parseModuleDescriptor(String descriptorPath) throws IOException {
        return parseModuleDescriptor(Paths.get(descriptorPath));
    }

    /**
     * Resolve a single jar
     *
     * @param request the request
     * @return the {@link ResolvePathResult}, containing the name and optional module descriptor
     * @throws IOException if any occurs
     */
    public <T> ResolvePathResult resolvePath(final ResolvePathRequest<T> request) throws IOException {
        ModuleNameExtractor filenameExtractor = new ModuleNameExtractor() {
            MainClassModuleNameExtractor extractor = new MainClassModuleNameExtractor(request.getJdkHome());

            @Override
            public String extract(Path file) throws IOException {
                if (request.getJdkHome() != null) {
                    return extractor
                            .extract(Collections.singletonMap(file, file))
                            .get(file);
                } else {
                    return CmdModuleNameExtractor.getModuleName(file);
                }
            }
        };

        return resolvePath(
                request.toPath(request.getPathElement()),
                filenameExtractor,
                getBinaryModuleInfoParser(request.getJdkHome()));
    }

    /**
     * Decide for every {@code request.getPathElements()} if it belongs to the modulePath or classPath, based on the
     * {@code request.getMainModuleDescriptor()}.
     *
     * @param request the paths to resolve
     * @return the result of the resolution
     * @throws IOException if a critical IOException occurs
     */
    public <T> ResolvePathsResult<T> resolvePaths(final ResolvePathsRequest<T> request) throws IOException {
        final ResolvePathsResult<T> result = request.createResult();

        Map<T, JavaModuleDescriptor> pathElements =
                new LinkedHashMap<>(request.getPathElements().size());

        final ModuleInfoParser binaryParser = getBinaryModuleInfoParser(request.getJdkHome());

        JavaModuleDescriptor mainModuleDescriptor = getMainModuleDescriptor(request, binaryParser);

        result.setMainModuleDescriptor(mainModuleDescriptor);

        // key = service, value = names of modules that provide this service
        Map<String, Set<String>> availableProviders = new HashMap<>();

        if (mainModuleDescriptor != null && request.isIncludeAllProviders()) {
            collectProviders(mainModuleDescriptor, availableProviders);
        }

        Map<String, JavaModuleDescriptor> availableNamedModules = new HashMap<>();

        Map<String, ModuleNameSource> moduleNameSources = new HashMap<>();

        final Map<T, Path> filenameAutoModules = new HashMap<>();

        // collect all modules from path
        for (final T t : request.getPathElements()) {
            JavaModuleDescriptor moduleDescriptor;
            ModuleNameSource source;

            ModuleNameExtractor nameExtractor = new ModuleNameExtractor() {
                @Override
                public String extract(Path path) throws IOException {
                    if (request.getJdkHome() != null) {
                        filenameAutoModules.put(t, path);
                    } else {
                        return CmdModuleNameExtractor.getModuleName(path);
                    }
                    return null;
                }
            };

            try {
                ResolvePathResult resolvedPath = resolvePath(request.toPath(t), nameExtractor, binaryParser);

                moduleDescriptor = resolvedPath.getModuleDescriptor();

                source = resolvedPath.getModuleNameSource();
            } catch (Exception e) {
                result.getPathExceptions().put(t, e);

                pathElements.put(t, null);

                continue;
            }

            // in case of identical module names, first one wins
            if (moduleDescriptor != null && moduleNameSources.putIfAbsent(moduleDescriptor.name(), source) == null) {
                availableNamedModules.put(moduleDescriptor.name(), moduleDescriptor);

                if (request.isIncludeAllProviders()) {
                    collectProviders(moduleDescriptor, availableProviders);
                }
            }

            pathElements.put(t, moduleDescriptor);
        }
        result.setPathElements(pathElements);

        if (!filenameAutoModules.isEmpty()) {
            MainClassModuleNameExtractor extractor = new MainClassModuleNameExtractor(request.getJdkHome());

            Map<T, String> automodules = extractor.extract(filenameAutoModules);

            for (Map.Entry<T, String> entry : automodules.entrySet()) {
                String moduleName = entry.getValue();

                if (moduleName != null) {
                    JavaModuleDescriptor moduleDescriptor =
                            JavaModuleDescriptor.newAutomaticModule(moduleName).build();

                    moduleNameSources.put(moduleDescriptor.name(), ModuleNameSource.FILENAME);

                    availableNamedModules.put(moduleDescriptor.name(), moduleDescriptor);

                    pathElements.put(entry.getKey(), moduleDescriptor);
                }
            }
        }

        Set<String> requiredNamedModules = new HashSet<>();

        if (mainModuleDescriptor != null) {
            requiredNamedModules.add(mainModuleDescriptor.name());

            selectRequires(
                    mainModuleDescriptor,
                    Collections.unmodifiableMap(availableNamedModules),
                    Collections.unmodifiableMap(availableProviders),
                    requiredNamedModules,
                    true,
                    true,
                    request.isIncludeStatic());
        }

        for (String additionalModule : request.getAdditionalModules()) {
            selectModule(
                    additionalModule,
                    Collections.unmodifiableMap(availableNamedModules),
                    Collections.unmodifiableMap(availableProviders),
                    requiredNamedModules,
                    true,
                    true,
                    request.isIncludeStatic());
        }

        Set<String> collectedModules = new HashSet<>(requiredNamedModules.size());

        for (Entry<T, JavaModuleDescriptor> entry : pathElements.entrySet()) {
            if (entry.getValue() != null
                    && requiredNamedModules.contains(entry.getValue().name())) {
                // Consider strategies how to handle duplicate modules by name
                // For now only add first on modulePath, just ignore others,
                //   This has effectively the same result as putting it on the modulePath, but might better help
                // analyzing issues.
                if (collectedModules.add(entry.getValue().name())) {
                    result.getModulepathElements()
                            .put(
                                    entry.getKey(),
                                    moduleNameSources.get(entry.getValue().name()));
                } else {
                    result.getPathExceptions()
                            .put(
                                    entry.getKey(),
                                    new IllegalStateException(
                                            "Module '" + entry.getValue().name() + "' is already on the module path!"));
                }
            } else {
                result.getClasspathElements().add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * If the jdkHome is specified, its version it considered higher than the runtime java version.
     * In that case ASM must be used to read the module descriptor
     *
     * @param jdkHome
     * @return
     */
    ModuleInfoParser getBinaryModuleInfoParser(final Path jdkHome) {
        final ModuleInfoParser binaryParser;
        if (jdkHome == null) {
            binaryParser = new BinaryModuleInfoParser();
        } else {
            binaryParser = new AsmModuleInfoParser();
        }
        return binaryParser;
    }

    private <T> JavaModuleDescriptor getMainModuleDescriptor(
            final ResolvePathsRequest<T> request, ModuleInfoParser binaryParser) throws IOException {
        JavaModuleDescriptor mainModuleDescriptor;

        Path descriptorPath = request.getMainModuleDescriptor();

        if (descriptorPath != null) {
            if (descriptorPath.endsWith("module-info.java")) {
                mainModuleDescriptor = sourceParser.fromSourcePath(descriptorPath);
            } else if (descriptorPath.endsWith("module-info.class")) {
                mainModuleDescriptor = binaryParser.getModuleDescriptor(descriptorPath.getParent());
            } else {
                throw new IOException("Invalid path to module descriptor: " + descriptorPath);
            }
        } else {
            mainModuleDescriptor = request.getModuleDescriptor();
        }
        return mainModuleDescriptor;
    }

    private ResolvePathResult resolvePath(
            Path path, ModuleNameExtractor fileModulenameExtractor, ModuleInfoParser binaryParser) throws IOException {
        ResolvePathResult result = new ResolvePathResult();

        JavaModuleDescriptor moduleDescriptor = null;

        // either jar or outputDirectory
        if (Files.isRegularFile(path) && !path.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException(
                    "'" + path.toString() + "' not allowed on the path, only outputDirectories and jars are accepted");
        }

        if (Files.isRegularFile(path) || Files.exists(path.resolve("module-info.class"))) {
            moduleDescriptor = binaryParser.getModuleDescriptor(path);
        }

        if (moduleDescriptor != null) {
            result.setModuleNameSource(ModuleNameSource.MODULEDESCRIPTOR);
        } else {
            String moduleName = manifestModuleNameExtractor.extract(path);

            if (moduleName != null) {
                result.setModuleNameSource(ModuleNameSource.MANIFEST);
            } else {
                moduleName = fileModulenameExtractor.extract(path);

                if (moduleName != null) {
                    result.setModuleNameSource(ModuleNameSource.FILENAME);
                }
            }

            if (moduleName != null) {
                moduleDescriptor =
                        JavaModuleDescriptor.newAutomaticModule(moduleName).build();
            }
        }
        result.setModuleDescriptor(moduleDescriptor);

        return result;
    }

    private void selectRequires(
            JavaModuleDescriptor module,
            Map<String, JavaModuleDescriptor> availableModules,
            Map<String, Set<String>> availableProviders,
            Set<String> namedModules,
            boolean isRootModule,
            boolean includeAsTransitive,
            boolean includeStatic) {
        for (JavaModuleDescriptor.JavaRequires requires : module.requires()) {
            // includeTransitive is one level deeper compared to includeStatic
            if (isRootModule
                    || includeStatic
                    || includeAsTransitive
                    || !requires.modifiers().contains(JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC)
                    || requires.modifiers().contains(JavaModuleDescriptor.JavaRequires.JavaModifier.TRANSITIVE)) {
                selectModule(
                        requires.name(),
                        availableModules,
                        availableProviders,
                        namedModules,
                        false,
                        includeStatic,
                        includeStatic);
            }
        }

        for (String uses : module.uses()) {
            if (availableProviders.containsKey(uses)) {
                for (String providerModule : availableProviders.get(uses)) {
                    JavaModuleDescriptor requiredModule = availableModules.get(providerModule);

                    if (requiredModule != null && namedModules.add(providerModule)) {
                        selectRequires(
                                requiredModule,
                                availableModules,
                                availableProviders,
                                namedModules,
                                false,
                                includeAsTransitive,
                                includeStatic);
                    }
                }
            }
        }
    }

    private void selectModule(
            String module,
            Map<String, JavaModuleDescriptor> availableModules,
            Map<String, Set<String>> availableProviders,
            Set<String> namedModules,
            boolean isRootModule,
            boolean includeTransitive,
            boolean includeStatic) {
        JavaModuleDescriptor requiredModule = availableModules.get(module);

        if (requiredModule != null && namedModules.add(module)) {
            selectRequires(
                    requiredModule,
                    availableModules,
                    availableProviders,
                    namedModules,
                    false,
                    includeTransitive,
                    includeStatic);
        }
    }

    private void collectProviders(JavaModuleDescriptor moduleDescriptor, Map<String, Set<String>> availableProviders) {
        for (JavaProvides provides : moduleDescriptor.provides()) {
            // module-info.class uses FQN, i.e. $-separator for subclasses
            final String serviceClassName = provides.service().replace('$', '.');

            Set<String> providingModules = availableProviders.get(serviceClassName);

            if (providingModules == null) {
                providingModules = new HashSet<>();

                availableProviders.put(serviceClassName, providingModules);
            }
            providingModules.add(moduleDescriptor.name());
        }
    }
}
