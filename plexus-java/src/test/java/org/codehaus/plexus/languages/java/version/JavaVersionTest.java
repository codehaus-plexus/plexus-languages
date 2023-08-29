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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Parsing is lazy, only triggered when comparing
 */
class JavaVersionTest {
    @Test
    void testParse() {
        assertThat(JavaVersion.parse("1.4").compareTo(JavaVersion.parse("1.4.2")))
                .isLessThan(0);
        assertThat(JavaVersion.parse("1.4").compareTo(JavaVersion.parse("1.5"))).isLessThan(0);
        assertThat(JavaVersion.parse("1.8").compareTo(JavaVersion.parse("9"))).isLessThan(0);

        assertThat(JavaVersion.parse("1.4").compareTo(JavaVersion.parse("1.4"))).isEqualTo(0);
        assertThat(JavaVersion.parse("1.4.2").compareTo(JavaVersion.parse("1.4.2")))
                .isEqualTo(0);
        assertThat(JavaVersion.parse("9").compareTo(JavaVersion.parse("9"))).isEqualTo(0);

        assertThat(JavaVersion.parse("1.4.2").compareTo(JavaVersion.parse("1.4")))
                .isGreaterThan(0);
        assertThat(JavaVersion.parse("1.5").compareTo(JavaVersion.parse("1.4"))).isGreaterThan(0);
        assertThat(JavaVersion.parse("9").compareTo(JavaVersion.parse("1.8"))).isGreaterThan(0);
    }

    @Test
    void testVersionNamingExamples() {
        // All GA (FCS) versions are ordered based on the standard dot-notation. For example: 1.3.0 < 1.3.0_01 < 1.3.1 <
        // 1.3.1_01.
        // Source: http://www.oracle.com/technetwork/java/javase/versioning-naming-139433.html

        assertThat(JavaVersion.parse("1.3.0").compareTo(JavaVersion.parse("1.3.0_01")))
                .isLessThan(0);
        assertThat(JavaVersion.parse("1.3.0_01").compareTo(JavaVersion.parse("1.3.1")))
                .isLessThan(0);
        assertThat(JavaVersion.parse("1.3.1").compareTo(JavaVersion.parse("1.3.1_01")))
                .isLessThan(0);

        assertThat(JavaVersion.parse("1.3.0").compareTo(JavaVersion.parse("1.3.0-b24")))
                .isLessThan(0);
    }

    @Test
    void testJEP223Short() {
        // http://openjdk.java.net/jeps/223
        assertThat(JavaVersion.parse("9-ea").compareTo(JavaVersion.parse("9"))).isLessThan(0);
        assertThat(JavaVersion.parse("9").compareTo(JavaVersion.parse("9.0.1"))).isLessThan(0);
        assertThat(JavaVersion.parse("9.0.1").compareTo(JavaVersion.parse("9.0.2")))
                .isLessThan(0);
        assertThat(JavaVersion.parse("9.0.2").compareTo(JavaVersion.parse("9.1.2")))
                .isLessThan(0);
        assertThat(JavaVersion.parse("9.1.2").compareTo(JavaVersion.parse("9.1.3")))
                .isLessThan(0);
        assertThat(JavaVersion.parse("9.1.3").compareTo(JavaVersion.parse("9.1.4")))
                .isLessThan(0);
        assertThat(JavaVersion.parse("9.1.4").compareTo(JavaVersion.parse("9.2.4")))
                .isLessThan(0);
    }

    @Test
    void testIsAtLeastString() {
        JavaVersion base = JavaVersion.parse("7");
        assertTrue(base.isAtLeast("7"));
        assertFalse(base.isAtLeast("8"));
    }

    @Test
    void testIsAtLeastVersion() {
        // e.g. can I use the module-path, which is supported since java 9
        JavaVersion j9 = JavaVersion.parse("9");
        assertFalse(JavaVersion.parse("8").isAtLeast(j9));
        assertTrue(JavaVersion.parse("9").isAtLeast(j9));
    }

    @Test
    void testIsBeforeString() {
        JavaVersion base = JavaVersion.parse("7");
        assertFalse(base.isBefore("7"));
        assertTrue(base.isBefore("8"));
    }

    @Test
    void testIsBeforeStringVersion() {
        // e.g. can I use -XX:MaxPermSize, which has been removed in Java 9
        JavaVersion j9 = JavaVersion.parse("9");
        assertTrue(JavaVersion.parse("8").isBefore(j9));
        assertFalse(JavaVersion.parse("9").isBefore(j9));
    }

    @Test
    void testEquals() {
        JavaVersion seven = JavaVersion.parse("7");
        JavaVersion other = JavaVersion.parse("7");

        assertThat(seven).isEqualTo(seven);
        assertEquals(seven, other);
        assertNotEquals(null, seven);
        assertNotEquals(seven, new Object());
        assertNotEquals(seven, JavaVersion.parse("8"));
    }

    @Test
    void testHascode() {
        JavaVersion seven = JavaVersion.parse("7");
        JavaVersion other = JavaVersion.parse("7");

        assertEquals(seven.hashCode(), other.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("7", JavaVersion.parse("7").toString());

        assertEquals("!@#$%^&*()", JavaVersion.parse("!@#$%^&*()").toString(), "Raw version should not be parsed");
    }

    @Test
    void testAsMajor() {
        assertEquals(JavaVersion.parse("2"), JavaVersion.parse("1.2").asMajor());
        assertEquals(JavaVersion.parse("5.0"), JavaVersion.parse("5.0").asMajor());
        // only shift one time
        assertEquals("1.2", JavaVersion.parse("1.1.2").asMajor().asMajor().toString());
    }

    @Test
    void testAsMajorEquals() {
        JavaVersion version = JavaVersion.parse("1.2");
        assertEquals(version, version.asMajor());
    }

    @Test
    void testValueWithGroups() {
        assertThat(JavaVersion.parse("1").getValue(1)).isEqualTo("1");
        assertThat(JavaVersion.parse("1").getValue(2)).isEqualTo("1.0");
        assertThat(JavaVersion.parse("1").getValue(3)).isEqualTo("1.0.0");
        assertThat(JavaVersion.parse("2.1").getValue(1)).isEqualTo("2");
        assertThat(JavaVersion.parse("2.1").getValue(2)).isEqualTo("2.1");
        assertThat(JavaVersion.parse("2.1").getValue(3)).isEqualTo("2.1.0");
        assertThat(JavaVersion.parse("3.2.1").getValue(1)).isEqualTo("3");
        assertThat(JavaVersion.parse("3.2.1").getValue(2)).isEqualTo("3.2");
        assertThat(JavaVersion.parse("3.2.1").getValue(3)).isEqualTo("3.2.1");
    }
}
