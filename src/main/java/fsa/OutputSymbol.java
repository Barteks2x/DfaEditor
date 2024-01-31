package fsa;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public record OutputSymbol(String name, long encoding) {
    private static final List<OutputSymbol> DEFAULT_ALPHABET = createOutputAlphabet("0", "1");

    public static List<OutputSymbol> getDefaultAlphabet() {
        return DEFAULT_ALPHABET;
    }

    public static List<OutputSymbol> createOutputAlphabet(String... names) {
        AtomicLong counter = new AtomicLong(0L);
        return Arrays.stream(names)
                .map(name -> new OutputSymbol(name, counter.getAndIncrement()))
                .toList();
    }

    @Override public int hashCode() {
        return Long.hashCode(encoding);
    }

    @Override
    public String toString() {
        return name + " (" + encoding + ")";
    }
}
