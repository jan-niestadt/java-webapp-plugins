package org.ivdnt.test;

import java.util.Locale;

public class PluginToUpper implements StringProcessingPlugin {

    public String getName() {
        return "upper";
    }

    public String getDescription() {
        return "Convert input to uppercase";
    }

    public String process(String value) {
        return value.toUpperCase(Locale.ROOT) + " TEST";
    }
}
