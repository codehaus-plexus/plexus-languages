package org.codehaus.plexus.languages.java.jpms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class helps with the module module-pattern form of the module source path.
 *
 *
 * @See https://docs.oracle.com/en/java/javase/23/docs/specs/man/javac.html#the-module-source-path-option
 */
class SegmentParser {

    private Consumer<Element> nextAction;

    protected List<String> findAll(String segment) {
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

                    Block b = blocks.poll();

                    nextAction = b::tail;

                    value = new StringBuilder();

                    break;
                }
                default:
                    value.append(c);

                    break;
            }
        }

        if (value.length() > 0) {
            nextAction.accept(new Value(value.toString()));
        }

        return root.resolvePaths();
    }

    private interface Element {
        List<String> resolvePaths(List<String> bases);
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
