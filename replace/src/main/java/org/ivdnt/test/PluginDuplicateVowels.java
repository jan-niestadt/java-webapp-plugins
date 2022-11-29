package org.ivdnt.test;

public class PluginDuplicateVowels implements StringProcessingPlugin {

    @Override
    public String getName() {
        return "dupe";
    }

    public String getDescription() {
        return "Duplicate vowels";
    }

    public String process(String value) {
        return value.replaceAll("[aeouiAEOUI]", "$0$0");
    }
}
