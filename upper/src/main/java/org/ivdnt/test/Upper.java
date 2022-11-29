package org.ivdnt.test;

import java.util.Locale;

public class Upper implements StringProcessingPlugin {

    public String getDescription() {
        return "Convert input to uppercase";
    }

    public Upper() {
        // do nothing
    }

    public String process(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}
