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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Extracts the name of the module by reading the Automatic-Module-Name attribute of the manifest file
 *
 * @author Robert Scholte
 * @since 1.0.0
 */
class ManifestModuleNameExtractor implements ModuleNameExtractor {
    @Override
    public String extract(Path file) throws IOException {
        Manifest manifest = extractManifest(file.toFile());

        String automaticModuleName;
        if (manifest != null) {
            automaticModuleName = manifest.getMainAttributes().getValue("Automatic-Module-Name");
        } else {
            automaticModuleName = null;
        }

        return automaticModuleName;
    }

    private Manifest extractManifest(File file) throws IOException {
        Manifest manifest;
        if (file.isFile()) {
            try (JarFile jarFile = new JarFile(file)) {
                manifest = jarFile.getManifest();
            }
        } else if (new File(file, "META-INF/MANIFEST.MF").exists()) {
            try (InputStream is = new FileInputStream(new File(file, "META-INF/MANIFEST.MF"))) {
                manifest = new Manifest(is);
            }
        } else {
            manifest = null;
        }

        return manifest;
    }
}
