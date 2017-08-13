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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses a jshell script to extract the module name of  
 *  
 * @author Robert Scholte
 */
public class JShellModuleNameExtractor
    implements ModuleNameExtractor
{
    private final File jdkHome;

    public JShellModuleNameExtractor( File jdkHome )
    {
        this.jdkHome = jdkHome;
    }

    @Override
    public String extract( File file )
    {
        try
        {
            File jsh = File.createTempFile( "modulename_", ".jsh" );
            jsh.deleteOnExit();

            List<String> lines = new ArrayList<>();
            lines.add( "System.out.println(java.lang.module.ModuleFinder.of(java.nio.file.Paths.get(\""
                + file.getAbsolutePath().replace( "\\", "\\\\" )
                + "\")).findAll().stream().findFirst().get().descriptor().name())" );
            lines.add( "/exit" );

            Files.write( jsh.toPath(), lines, Charset.defaultCharset() );

            ProcessBuilder builder =
                new ProcessBuilder( new File( jdkHome, "bin/jshell" ).getAbsolutePath(), jsh.getAbsolutePath() );
            Process p = builder.start();

            try ( BufferedReader reader = new BufferedReader( new InputStreamReader( p.getInputStream() ) ) )
            {
                return reader.readLine();
            }
        }
        catch ( IOException e )
        {
        }
        return null;
    }
}
