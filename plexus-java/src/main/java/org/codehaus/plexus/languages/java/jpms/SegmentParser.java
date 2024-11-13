package org.codehaus.plexus.languages.java.jpms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class helps with the module module-pattern form of the module source path.
 *
 *
 * @See https://docs.oracle.com/en/java/javase/23/docs/specs/man/javac.html#the-module-source-path-option
 */
class SegmentParser {

    private static final char MODULENAME_MARKER = '*';

    private Consumer<Element> nextAction;

    /**
     * Each segment containing curly braces of the form
     *
     * <code>string1{alt1 ( ,alt2 )* } string2</code>
     *
     * is considered to be replaced by a series of segments formed by "expanding" the braces:
     *
     * <ul>
     * <li><code>string1 alt1 string2</code></li>
     * <li><code>string1 alt2 string2</code></li>
     * </ul>
     * and so on...
     * <p>
     * The braces may be nested.
     * </p>
     * <p>
     * This rule is applied for all such usages of braces.
     * </p>
     *
     * @param segment the segment to expand
     * @return all expanded segments
     * @throws IllegalArgumentException if braces are unbalanced
     * @see <a href="https://docs.oracle.com/en/java/javase/23/docs/specs/man/javac.html#the-module-source-path-option">https://docs.oracle.com/en/java/javase/23/docs/specs/man/javac.html#the-module-source-path-option</a>
     */
    protected List<String> expandBraces(String segment) {
        StringBuilder value = new StringBuilder();

        Root root = new Root();
        nextAction = root::element;

        Deque<Block> blocks = new ArrayDeque<>();

        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            switch (c) {
                case '{': {
                    Block b = new Block(value.toString());

                    nextAction.accept(b);

                    blocks.push(b);

                    nextAction = b::element;

                    value = new StringBuilder();

                    break;
                }
                case ',': {
                    nextAction.accept(new Value(value.toString()));

                    Block b = blocks.peek();

                    nextAction = b::element;

                    value = new StringBuilder();

                    break;
                }
                case '}': {
                    nextAction.accept(new Value(value.toString()));

                    try {
                        Block b = blocks.pop();

                        nextAction = b::tail;
                    } catch (NoSuchElementException e) {
                        throw new IllegalArgumentException("Unbalanced braces, missing {");
                    }

                    value = new StringBuilder();

                    break;
                }
                default:
                    value.append(c);

                    break;
            }
        }

        if (blocks.size() > 0) {
            throw new IllegalArgumentException("Unbalanced braces, missing }");
        }

        if (value.length() > 0) {
            nextAction.accept(new Value(value.toString()));
        }

        return root.resolvePaths();
    }

    private interface Element {
        List<String> resolvePaths(List<String> bases);

        boolean hasModuleNameMarker();
    }

    private final class Value implements Element {

        private final String value;

        public Value(String value) {
            this.value = value;
        }

        @Override
        public List<String> resolvePaths(List<String> bases) {
            return bases.stream().map(b -> b + value).collect(Collectors.toList());
        }

        @Override
        public boolean hasModuleNameMarker() {
            return value.indexOf(MODULENAME_MARKER) >= 0;
        }

        @Override
        public String toString() {
            return "Value [value=" + value + "]";
        }
    }

    private final class Block implements Element {

        private final String value;

        private List<Element> elements = new ArrayList<>();

        private Element tail;

        public Block(String value) {
            this.value = value;
        }

        public void element(Element element) {
            elements.add(element);
        }

        public void tail(Element element) {
            tail = element;
        }

        @Override
        public List<String> resolvePaths(List<String> bases) {
            final List<String> valuedBases = bases.stream().map(b -> b + value).collect(Collectors.toList());

            List<String> newBases;
            if (elements.isEmpty()) {
                newBases = valuedBases;
            } else {
                newBases = elements.stream()
                        .flatMap(e -> e.resolvePaths(valuedBases).stream())
                        .collect(Collectors.toList());
            }

            if (tail == null) {
                return newBases;
            } else {
                return tail.resolvePaths(newBases);
            }
        }

        @Override
        public boolean hasModuleNameMarker() {
            return value.indexOf(MODULENAME_MARKER) >= 0;
        }

        @Override
        public String toString() {
            return "Block [value=" + value + ", elements=" + elements + ", tail=" + tail + "]";
        }
    }

    private final class Root {

        private Element element;

        public void element(Element element) {
            this.element = element;
        }

        public List<String> resolvePaths() {
            return element.resolvePaths(Arrays.asList(""));
        }

        @Override
        public String toString() {
            return "Root [element=" + element + "]";
        }
    }
}
