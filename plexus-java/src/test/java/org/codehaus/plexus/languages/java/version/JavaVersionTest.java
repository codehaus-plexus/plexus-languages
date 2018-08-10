package org.codehaus.plexus.languages.java.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

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

/*
 * Parsing is lazy, only triggered when comparing
 */
public class JavaVersionTest
{
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
    
    @Test
    public void testIsAtLeastString() {
        JavaVersion base = JavaVersion.parse( "7" );
        assertTrue( base.isAtLeast( "7" ) );
        assertFalse( base.isAtLeast( "8" ) );
    }

    @Test
    public void testIsAtLeastVersion() {
        // e.g. can I use the module-path, which is supported since java 9
        JavaVersion j9 = JavaVersion.parse( "9" );
        assertFalse( JavaVersion.parse( "8" ).isAtLeast( j9 ) );
        assertTrue( JavaVersion.parse( "9" ).isAtLeast( j9 ) );
    }

    @Test
    public void testIsBeforeString() {
        JavaVersion base = JavaVersion.parse( "7" );
        assertFalse( base.isBefore( "7" ) );
        assertTrue( base.isBefore( "8" ) );
    }

    @Test
    public void testIsBeforeStringVersion() {
        // e.g. can I use -XX:MaxPermSize, which has been removed in Java 9
        JavaVersion j9 = JavaVersion.parse( "9" );
        assertTrue( JavaVersion.parse( "8" ).isBefore( j9 ) );
        assertFalse( JavaVersion.parse( "9" ).isBefore( j9 ) );
    }

    @Test
    public void testEquals() {
        JavaVersion seven = JavaVersion.parse( "7" );
        JavaVersion other = JavaVersion.parse( "7" );
        
        assertEquals( seven, seven );
        assertEquals( seven, other );
        assertNotEquals( seven, null );
        assertNotEquals( seven, new Object() );
        assertNotEquals( seven, JavaVersion.parse( "8" ) );
    }

    @Test
    public void testHascode() {
        JavaVersion seven = JavaVersion.parse( "7" );
        JavaVersion other = JavaVersion.parse( "7" );
        
        assertEquals( seven.hashCode(), other.hashCode() );
    }

    @Test
    public void testToString() {
        assertEquals( "7", JavaVersion.parse( "7" ).toString() );
        
        assertEquals( "Raw version should not be parsed", "!@#$%^&*()", JavaVersion.parse( "!@#$%^&*()" ).toString() );
    }
    
}
