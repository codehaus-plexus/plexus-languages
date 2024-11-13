package org.codehaus.plexus.languages.java.jpms;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleSourcePathSupport {

    /**
     *
     *
     * @param segments the list of segments, as split by the path-separator
     * @return all possible combinations
     */
    public List<String> expand(List<String> segments) {
        SegmentParser parser = new SegmentParser();

        List<String> result = new ArrayList<>();

        for (String segment : segments) {
            try {
                result.addAll(parser.expandBraces(segment));
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Failed to parse " + segment + ": " + e.getMessage());
            }
        }

        return segments.stream().flatMap(e -> parser.expandBraces(e).stream()).collect(Collectors.toList());
    }
}
