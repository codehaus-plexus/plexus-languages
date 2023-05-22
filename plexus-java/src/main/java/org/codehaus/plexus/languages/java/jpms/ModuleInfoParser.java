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
import java.nio.file.Path;

import org.codehaus.plexus.languages.java.version.JavaVersion;

/**
 * Extract information from the module-info file
 *
 * @author Robert Scholte
 * @since 1.0.0
 */
interface ModuleInfoParser {
    /**
     * Extracts the name from the module-info file
     *
     * @param modulePath the path to the {@code module-info.class}
     * @return the module descriptor
     * @throws IOException when the file could not be parsed
     */
    JavaModuleDescriptor getModuleDescriptor(Path modulePath) throws IOException;

    /**
     * Extracts the name from the module-info file
     *
     * @param modulePath the path to the {@code module-info.class}
     * @param javaVersion the java version in case of a multirelease jar
     * @return the module descriptor
     * @throws IOException when the file could not be parsed
     */
    JavaModuleDescriptor getModuleDescriptor(Path modulePath, JavaVersion javaVersion) throws IOException;
}
