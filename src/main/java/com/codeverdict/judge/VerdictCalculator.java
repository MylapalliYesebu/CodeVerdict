package com.codeverdict.judge;

public class VerdictCalculator {
    public static String calculateVerdict(String actualOutput, String expectedOutput) {
        String actual = normalizeOutput(actualOutput);
        String expected = normalizeOutput(expectedOutput);
        if (expected.equals(actual)) {
            return "ACCEPTED";
        }
        return "WRONG_ANSWER";
    }

    private static String normalizeOutput(String str) {
        if (str == null) return "";
        // replace \r\n with \n, strip trailing spaces per line
        String[] lines = str.replace("\r\n", "\n").split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            // strip trailing spaces
            String line = lines[i].replaceAll("\\s+$", "");
            sb.append(line);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString().trim();
    }
}
