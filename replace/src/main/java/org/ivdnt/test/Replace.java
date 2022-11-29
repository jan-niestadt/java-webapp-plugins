package org.ivdnt.test;

import java.util.Locale;

public class Replace implements StringProcessingPlugin {

    public String getDescription() {
        return "Replace some letters with their l33tsp34k equivalent";
    }

    public Replace() {
        // do nothing
    }

    public String process(String value) {
        return value.replaceAll("[aA]", "4")
                .replaceAll("[eE]", "3")
                .replaceAll("[oO]", "0")
                .replaceAll("[iI]", "1");
    }
}
