package org.codehaus.plexus.languages.java.jpms;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SegmentParserTest {

    @ParameterizedTest
    @MethodSource
    void expandBraces(String segment, String... expectedValues) {
        SegmentParser parser = new SegmentParser();
        assertThat(parser.expandBraces(segment)).containsExactlyInAnyOrder(expectedValues);
    }

    static Stream<Arguments> expandBraces() {
        return Stream.of(
                Arguments.of("foo", new String[] {"foo"}),
                Arguments.of("foo/{bar,baz}", new String[] {"foo/bar", "foo/baz"}),
                Arguments.of("foo/{bar,baz}/com", new String[] {"foo/bar/com", "foo/baz/com"}),
                Arguments.of("foo/{bar,baz}/com/{1,2,3}/zzz", new String[] {
                    "foo/bar/com/1/zzz",
                    "foo/baz/com/1/zzz",
                    "foo/bar/com/2/zzz",
                    "foo/baz/com/2/zzz",
                    "foo/bar/com/3/zzz",
                    "foo/baz/com/3/zzz"
                }),
                Arguments.of("foo/{bar,baz/com/{1,2,3}/yyy}/zzz", new String[] {
                    "foo/bar/zzz", "foo/baz/com/1/yyy/zzz", "foo/baz/com/2/yyy/zzz", "foo/baz/com/3/yyy/zzz"
                }),
                Arguments.of("{foo/{bar},zzz}", new String[] {"foo/bar", "zzz"}));
    }

    @ParameterizedTest
    @MethodSource
    void expandBraces_exceptions(String segment, String exceptionMessage) {
        SegmentParser parser = new SegmentParser();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parser.expandBraces(segment));
        assertThat(e.getMessage()).isEqualTo(exceptionMessage);
    }

    static Stream<Arguments> expandBraces_exceptions() {
        return Stream.of(
                Arguments.of("{", "Unbalanced braces, missing }"), Arguments.of("}", "Unbalanced braces, missing {"));
    }

    void bla() {
        String seg = "";
        int markStart = 0;

        Path prefix;
        if (markStart == 0) {
            prefix = getPath("");
        } else if (isSeparator(seg.charAt(markStart - 1))) {
            prefix = getPath(seg.substring(0, markStart - 1));
        } else {
            throw new IllegalArgumentException("illegal use of " + MARKER + " in " + seg);
        }

        prefix.compareTo(null);
    }

    static String MARKER;

    Path getPath(String s) {
        return null;
    }

    boolean isSeparator(char c) {
        return true;
    }
}
