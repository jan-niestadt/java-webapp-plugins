package org.ivdnt.test;

/** Duplicate all vowels */
public class DuplicateVowels implements StringProcessor {

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
