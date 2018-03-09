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
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.Test;

public class MainClassModuleNameExtractorTest extends AbstractFilenameModuleNameExtractorTest
{
    @Override
    protected ModuleNameExtractor getExtractor()
    {
        return new ModuleNameExtractor()
        {
            MainClassModuleNameExtractor extractor = new MainClassModuleNameExtractor( Paths.get( System.getProperty( "java.home" ) ) );
            
            @Override
            public String extract( Path file )
                throws IOException
            {
                return extractor.extract( Collections.singletonMap( file, file ) ).get( file );
            }
        };
    }
    
    @Test( expected = Exception.class )
    public void testClassicJarNameStartsWithNumber()
        throws Exception
    {
        MainClassModuleNameExtractor.getModuleName( Paths.get( "src/test/resources/jar.empty.invalid.name/101-1.0.0-SNAPSHOT.jar" ) );
    }
}
