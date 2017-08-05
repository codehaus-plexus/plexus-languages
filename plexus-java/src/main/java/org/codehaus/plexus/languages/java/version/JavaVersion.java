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

/**
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class JavaVersion implements Comparable<JavaVersion>
{
    private String rawVersion;

    private JavaVersion( String rawVersion )
    {
        this.rawVersion = rawVersion;
    }

    /**
     * Parser only the version-scheme.
     * 
     * @param s the version string
     * @return the version wrapped in a JavadocVersion
     */
    static JavaVersion parse( String s ) 
    {
        return new JavaVersion( s );
    }

    @Override
    public int compareTo( JavaVersion other )
    {
        String[] thisSegments = this.rawVersion.split( "\\." );
        String[] otherSegments = other.rawVersion.split( "\\." );
        
        int minSegments = Math.min( thisSegments.length, otherSegments.length );
        
        for ( int index = 0; index < minSegments; index++ )
        {
            int thisValue = Integer.parseInt( thisSegments[index] );
            int otherValue = Integer.parseInt( otherSegments[index] );
            
            int compareValue = Integer.compare( thisValue, otherValue );
            
            if ( compareValue != 0 )
            {
                return compareValue;
            }
        }
        
        return ( thisSegments.length - otherSegments.length );
    }

    @Override
    public String toString()
    {
        return rawVersion;
    }
}
