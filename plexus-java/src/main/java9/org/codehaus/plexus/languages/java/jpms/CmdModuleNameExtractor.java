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
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Usage:
 * <code>
 * java -classpath &lt;cp&gt; org.codehaus.plexus.languages.java.jpms.CmdModuleNameExtractor &lt;args&gt;
 * </code>
 * <p>
 * Where &lt;cp&gt; is the jar or directory containing the {@code o.c.p.l.j.j.CmdModuleNameExtractor} and
 * where &lt;args&gt; are paths to jars.
 * </p>
 * <p>
 * The result is a properties-file written ot the StdOut, having the jar path as key and the module name as value.<br>
 * Any exception is written to the StdErr.
 * </p> 
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class CmdModuleNameExtractor
{
    public static void main( String[] args )
    {
        Properties properties = new Properties();

        for ( String path : args )
        {
            try
            {
                String moduleName = getModuleName( Paths.get( path ) );
                if ( moduleName != null )
                {
                    properties.setProperty( path, moduleName );
                }
            }
            catch ( Exception e )
            {
                System.err.append( e.getMessage() );
            }
        }

        try
        {
            properties.store( System.out, "" );
        }
        catch ( IOException e )
        {
            System.exit( 1 );
        }
    }

    /**
     * Get the name of the module, using Java 9 code without reflection
     * 
     * @param modulePath the module path
     * @return the module name or null if a name can not be determined
     */
    public static String getModuleName( Path modulePath )
    {
        try
        {
            Set<ModuleReference> moduleReferences = ModuleFinder.of( modulePath ).findAll();

            Optional<ModuleReference> modRef = moduleReferences.stream().findFirst();

            return modRef.isPresent() ? modRef.get().descriptor().name() : null;
        }
        catch ( FindException e )
        {
            if ( Files.exists( modulePath ) && Files.isRegularFile( modulePath ) && modulePath.getFileName().toString().endsWith( ".jar" ) )
            {
                // the automatic module name cannot be determined from the file name for
                // many file naming conventions that ModuleFinder doesn't recognize
                // so, if it's a file that exists, just return null (no module name found)
                return null;
            }
            else
            {
                // rethrow if it's not a .jar file
                throw e;
            }
        }
    }
}
