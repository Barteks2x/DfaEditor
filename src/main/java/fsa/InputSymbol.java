package fsa;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public record InputSymbol(String name, long encoding) {
    private static final List<InputSymbol> DEFAULT_ALPHABET = createInputAlphabet("00", "01", "10", "11");

    public static List<InputSymbol> getDefaultAlphabet() {
        return DEFAULT_ALPHABET;
    }

    public static List<InputSymbol> createInputAlphabet(String... names) {
        AtomicLong counter = new AtomicLong(0L);
        return Arrays.stream(names)
                .map(name -> new InputSymbol(name, counter.getAndIncrement()))
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
