package org.codehaus.plexus.languages.java.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @author Robert Scholte
 * @since 1.0.0
 * 
 * @see <a href="http://www.oracle.com/technetwork/java/javase/namechange-140185.html">Java SE Naming and Versions</a>
 * @see <a href="http://openjdk.java.net/jeps/223">JEP 223: New Version-String Scheme</a>
 * @see <a href="http://openjdk.java.net/jeps/322">JEP 322: Time-Based Release Versioning</a>
 */
public class JavaVersion implements Comparable<JavaVersion>
{
    /**
     * Represents the System property {@code java.specification.version}
     */
    public static final JavaVersion JAVA_SPECIFICATION_VERSION = parse( System.getProperty( "java.specification.version" ) );

    /**
     * Represents the System property {@code java.version}
     */
    public static final JavaVersion JAVA_VERSION = parse( System.getProperty( "java.version" ) );
    
    private static final Pattern startingDigits = Pattern.compile( "(\\d+)(.*)" );
    
    private String rawVersion;

    private JavaVersion( String rawVersion )
    {
        this.rawVersion = rawVersion;
    }

    /**
     * Lazy parse the version-scheme.
     * Actual parsing is done when calling {@link #compareTo(JavaVersion)}  
     * 
     * @param s the version string
     * @return the version wrapped in a JavadocVersion
     */
    public static JavaVersion parse( String s ) 
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
            Matcher thisMatcher = startingDigits.matcher( thisSegments[index] );
            
            int thisValue;
            
            if( thisMatcher.find() )
            {
                thisValue = Integer.parseInt( thisMatcher.group( 1 ) );
            }
            else
            {
                thisValue = -1;
            }
            
            Matcher otherMatcher = startingDigits.matcher( otherSegments[index] );
            
            int otherValue;
            
            if( otherMatcher.find() )
            {
                otherValue = Integer.parseInt( otherMatcher.group( 1 ) );
            }
            else
            {
                otherValue = -1;
            }
            
            int compareValue = Integer.compare( thisValue, otherValue );
            
            if ( compareValue != 0 )
            {
                return compareValue;
            }

            compareValue = suffixRate( thisMatcher.group( 2 ) ) - suffixRate( otherMatcher.group( 2 ) );
            if ( compareValue != 0 )
            {
                return compareValue;
            }
            
            // works for now, but needs improvement
            compareValue = thisMatcher.group( 2 ).compareTo( otherMatcher.group( 2 ) );
            
            if ( compareValue != 0 )
            {
                return compareValue;
            }
        }
        
        return ( thisSegments.length - otherSegments.length );
    }
    
    private int suffixRate( String suffix ) {
        if ( "-ea".equals( suffix ) )
        {
            return -100;
        }
        else if ( "".equals( suffix ) )
        {
            return 0;
        }
        else 
        {
            return 10;
        }
    }

    /**
     * Verify if this version is before some other version
     * 
     * @param other the version to compare with
     * @return {@code true} is this is less than {@code other}, otherwise {@code false}
     */
    public boolean isBefore( JavaVersion other )
    {
        return this.compareTo( other ) < 0;
    }

    /**
     * Verify if this version is before some other version
     * 
     * @param other the version to compare with
     * @return {@code true}  is this is less than {@code other}, otherwise {@code false}
     */
    public boolean isBefore( String other )
    {
        return this.compareTo( parse( other ) ) < 0;
    }

    /**
     * Verify if this version is at least some other version
     * 
     * @param other the version to compare with
     * @return  {@code true}  is this is greater than or equal to {@code other}, otherwise {@code false}
     */
    public boolean isAtLeast( JavaVersion other )
    {
        return this.compareTo( other ) >= 0;
    }

    /**
     * Verify if this version is at least some other version
     * 
     * @param other the version to compare with
     * @return  {@code true} is this is greater than or equal to {@code other}, otherwise {@code false}
     */
    public boolean isAtLeast( String other )
    {
        return this.compareTo( parse( other ) ) >= 0;
    }

    @Override
    public String toString()
    {
        return rawVersion;
    }
}
