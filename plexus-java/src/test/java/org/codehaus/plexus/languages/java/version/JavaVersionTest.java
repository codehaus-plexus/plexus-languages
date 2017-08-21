package org.codehaus.plexus.languages.java.version;

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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JavaVersionTest
{
    /**
     * Parsing is lazy, only triggered when comparing
     * 
     * @throws Exception
     */
    @Test
    public void testParse()
        throws Exception
    {
        assertTrue( JavaVersion.parse( "1.4" ).compareTo( JavaVersion.parse( "1.4.2" ) ) < 0 );
        assertTrue( JavaVersion.parse( "1.4" ).compareTo( JavaVersion.parse( "1.5" ) ) < 0 );
        assertTrue( JavaVersion.parse( "1.8" ).compareTo( JavaVersion.parse( "9" ) ) < 0 );

        assertTrue( JavaVersion.parse( "1.4" ).compareTo( JavaVersion.parse( "1.4" ) ) == 0 );
        assertTrue( JavaVersion.parse( "1.4.2" ).compareTo( JavaVersion.parse( "1.4.2" ) ) == 0 );
        assertTrue( JavaVersion.parse( "9" ).compareTo( JavaVersion.parse( "9" ) ) == 0 );

        assertTrue( JavaVersion.parse( "1.4.2" ).compareTo( JavaVersion.parse( "1.4" ) ) > 0 );
        assertTrue( JavaVersion.parse( "1.5" ).compareTo( JavaVersion.parse( "1.4" ) ) > 0 );
        assertTrue( JavaVersion.parse( "9" ).compareTo( JavaVersion.parse( "1.8" ) ) > 0 );
    }
    
    @Test
    public void testVersionNamingExamples()
    {
        // All GA (FCS) versions are ordered based on the standard dot-notation. For example: 1.3.0 < 1.3.0_01 < 1.3.1 < 1.3.1_01.
        // Source: http://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html
        
        assertTrue( JavaVersion.parse( "1.3.0" ).compareTo( JavaVersion.parse( "1.3.0_01" ) ) < 0 );
        assertTrue( JavaVersion.parse( "1.3.0_01" ).compareTo( JavaVersion.parse( "1.3.1" ) ) < 0 );
        assertTrue( JavaVersion.parse( "1.3.1" ).compareTo( JavaVersion.parse( "1.3.1_01" ) ) < 0 );
        
        assertTrue( JavaVersion.parse( "1.3.0" ).compareTo( JavaVersion.parse( "1.3.0-b24" ) ) < 0 );
    }

    @Test
    public void testJEP223Short() {
        // http://openjdk.java.net/jeps/223
        assertTrue( JavaVersion.parse( "9-ea" ).compareTo( JavaVersion.parse( "9" ) ) < 0 );
        assertTrue( JavaVersion.parse( "9" ).compareTo( JavaVersion.parse( "9.0.1" ) ) < 0 );
        assertTrue( JavaVersion.parse( "9.0.1" ).compareTo( JavaVersion.parse( "9.0.2" ) ) < 0 );
        assertTrue( JavaVersion.parse( "9.0.2" ).compareTo( JavaVersion.parse( "9.1.2" ) ) < 0 );
        assertTrue( JavaVersion.parse( "9.1.2" ).compareTo( JavaVersion.parse( "9.1.3" ) ) < 0 );
        assertTrue( JavaVersion.parse( "9.1.3" ).compareTo( JavaVersion.parse( "9.1.4" ) ) < 0 );
        assertTrue( JavaVersion.parse( "9.1.4" ).compareTo( JavaVersion.parse( "9.2.4" ) ) < 0 );
    }
}
