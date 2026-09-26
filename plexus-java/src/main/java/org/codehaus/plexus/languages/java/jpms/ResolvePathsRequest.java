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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.codehaus.plexus.languages.java.version.JavaVersion;

/**
 * Contains all information required to analyze the project
 *
 * @author Robert Scholte
 * @since 1.0.0
 */
public abstract class ResolvePathsRequest<T> {
    private Path jdkHome;

    private Path mainModuleDescriptor;

    private Collection<T> pathElements;

    private Collection<String> additionalModules;

    private boolean includeAllProviders;

    private JavaModuleDescriptor resolvedMainModuleDescriptor;

    private boolean includeStatic;

    private JavaVersion targetRelease = JavaVersion.JAVA_SPECIFICATION_VERSION;

    private ResolvePathsRequest() {}

    /**
     * @deprecated use {@link #ofFiles(Collection)} instead
     */
    @Deprecated
    public static ResolvePathsRequest<File> withFiles(Collection<File> files) {
        return ofFiles(files);
    }

    public static ResolvePathsRequest<File> ofFiles(File... files) {
        return ofFiles(Arrays.asList(files));
    }

    public static ResolvePathsRequest<File> ofFiles(Collection<File> files) {
        ResolvePathsRequest<File> request = new ResolvePathsRequest<File>() {
            @Override
            protected Path toPath(File t) {
                return t.toPath();
            }
        };

        request.pathElements = files;
        return request;
    }

    /**
     * @deprecated use {@link #ofPaths(Collection)} instead
     */
    @Deprecated
    public static ResolvePathsRequest<Path> withPaths(Collection<Path> paths) {
        return ofPaths(paths);
    }

    public static ResolvePathsRequest<Path> ofPaths(Path... paths) {
        return ofPaths(Arrays.asList(paths));
    }

    public static ResolvePathsRequest<Path> ofPaths(Collection<Path> paths) {
        ResolvePathsRequest<Path> request = new ResolvePathsRequest<Path>() {
            @Override
            protected Path toPath(Path t) {
                return t;
            }
        };
        request.pathElements = paths;
        return request;
    }

    /**
     * @deprecated use {@link #ofStrings(Collection)} instead
     */
    @Deprecated
    public static ResolvePathsRequest<String> withStrings(Collection<String> strings) {
        return ofStrings(strings);
    }

    public static ResolvePathsRequest<String> ofStrings(String... strings) {
        return ofStrings(Arrays.asList(strings));
    }

    public static ResolvePathsRequest<String> ofStrings(Collection<String> strings) {
        ResolvePathsRequest<String> request = new ResolvePathsRequest<String>() {
            @Override
            protected Path toPath(String t) {
                return Paths.get(t);
            }
        };
        request.pathElements = strings;
        return request;
    }

    protected abstract Path toPath(T t);

    final ResolvePathsResult<T> createResult() {
        return new ResolvePathsResult<>();
    }

    public Path getMainModuleDescriptor() {
        return mainModuleDescriptor;
    }

    public JavaModuleDescriptor getModuleDescriptor() {
        return resolvedMainModuleDescriptor;
    }

    /**
     * Must be either {@code module-info.java} or {@code module-info.class}
     *
     * @param mainModuleDescriptor
     * @return this request
     */
    public ResolvePathsRequest<T> setMainModuleDescriptor(T mainModuleDescriptor) {
        this.mainModuleDescriptor = toPath(mainModuleDescriptor);
        return this;
    }

    /***
     * Provide a resolved module descriptor
     *
     * @param mainModuleDescriptor
     * @return this request
     */
    public ResolvePathsRequest<T> setModuleDescriptor(JavaModuleDescriptor mainModuleDescriptor) {
        this.resolvedMainModuleDescriptor = mainModuleDescriptor;
        return this;
    }

    public Collection<T> getPathElements() {
        return pathElements;
    }

    /**
     * In case the JRE is Java 8 or before, this jdkHome is used to extract the module name.
     *
     * @param jdkHome
     * @return this request
     */
    public ResolvePathsRequest<T> setJdkHome(T jdkHome) {
        this.jdkHome = toPath(jdkHome);
        return this;
    }

    public Path getJdkHome() {
        return jdkHome;
    }

    /**
     * The module names that are usually passed with {@code --add-modules}
     *
     * @param additionalModules
     * @return this request
     */
    public ResolvePathsRequest<T> setAdditionalModules(Collection<String> additionalModules) {
        this.additionalModules = additionalModules;
        return this;
    }

    public Collection<String> getAdditionalModules() {
        if (additionalModules == null) {
            additionalModules = Collections.emptyList();
        }
        return additionalModules;
    }

    /**
     * Will also include all modules that contain providers for used services, should only be used at runtime (not during compile nor test)
     *
     * @param includeAllProviders
     * @return this request
     */
    public ResolvePathsRequest<T> setIncludeAllProviders(boolean includeAllProviders) {
        this.includeAllProviders = includeAllProviders;
        return this;
    }

    public boolean isIncludeAllProviders() {
        return includeAllProviders;
    }

    /**
     *
     * @return <code>true</code> if the result will include all static dependencies
     * @since 1.0.5
     */
    public boolean isIncludeStatic() {
        return includeStatic;
    }

    /**
     *
     * @param includeStatic <code>true</code> if the result must include all static dependencies
     * @return this request
     * @since 1.0.5
     */
    public ResolvePathsRequest<T> setIncludeStatic(boolean includeStatic) {
        this.includeStatic = includeStatic;
        return this;
    }

    /**
     * The Java release the module descriptors must be resolved for - the {@code javac --release} value, or
     * {@code -target} when {@code --release} is unset. Accepts {@code "1.8"}-style values as well as bare
     * major versions. This picks the applicable versioned {@code module-info.class} of a multi-release jar or
     * output directory, i.e. the highest {@code META-INF/versions/<N>} whose {@code N} does not exceed this
     * release. Defaults to the version of the running JDK, so callers that compile for a different release
     * should set this explicitly.
     *
     * @param targetRelease the target release, never {@code null}
     * @return this request
     * @since 1.7.0
     */
    public ResolvePathsRequest<T> setTargetRelease(JavaVersion targetRelease) {
        this.targetRelease = Objects.requireNonNull(targetRelease, "targetRelease");
        return this;
    }

    /**
     * @return the Java release module descriptors are resolved for, defaults to the running JDK's version
     * @since 1.7.0
     */
    public JavaVersion getTargetRelease() {
        return targetRelease;
    }
}
