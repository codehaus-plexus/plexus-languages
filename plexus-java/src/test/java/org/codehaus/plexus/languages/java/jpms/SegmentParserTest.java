package org.codehaus.plexus.languages.java.jpms;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SegmentParserTest {

    @ParameterizedTest
    @MethodSource
    void findAll(String segment, String... expectedValues) {
        SegmentParser parser = new SegmentParser();
        assertThat(parser.findAll(segment)).containsExactlyInAnyOrder(expectedValues);
    }

    static Stream<Arguments> findAll() {
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
}
