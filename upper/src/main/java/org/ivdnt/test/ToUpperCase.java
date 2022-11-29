package org.ivdnt.test;

import java.util.Locale;

/** Convert input string to upper case. */
public class ToUpperCase implements StringProcessor {

    public String getName() {
        return "upper";
    }

    public String getDescription() {
        return "Convert input to uppercase";
    }

    public String process(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}
