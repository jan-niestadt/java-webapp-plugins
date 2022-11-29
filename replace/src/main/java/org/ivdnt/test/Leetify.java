package org.ivdnt.test;

/** Convert to leetspeak */
public class Leetify implements StringProcessor {

    @Override
    public String getName() {
        return "leet";
    }

    public String getDescription() {
        return "Replace some letters with their l33tsp34k equivalent";
    }

    public String process(String value) {
        return value.replaceAll("[aA]", "4")
                .replaceAll("[eE]", "3")
                .replaceAll("[oO]", "0")
                .replaceAll("[iI]", "1");
    }
}
