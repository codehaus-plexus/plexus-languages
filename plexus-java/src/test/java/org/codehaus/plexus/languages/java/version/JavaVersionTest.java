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
}
