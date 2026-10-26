package com.nuxora.app.utils;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiColorParser {

    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[([0-9;]*)m");

    public static CharSequence parse(String input) {
        if (input == null) return "";
        if (!input.contains("\u001B[")) {
            return input;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();
        Matcher matcher = ANSI_PATTERN.matcher(input);
        int lastEnd = 0;
        int currentColor = Color.parseColor("#E2E8F0"); // Default terminal text color

        while (matcher.find()) {
            // Append text before code
            String segment = input.substring(lastEnd, matcher.start());
            if (!segment.isEmpty()) {
                int startSpan = builder.length();
                builder.append(segment);
                builder.setSpan(new ForegroundColorSpan(currentColor), startSpan, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Parse color codes
            String code = matcher.group(1);
            currentColor = resolveAnsiColor(code, currentColor);
            lastEnd = matcher.end();
        }

        if (lastEnd < input.length()) {
            String remaining = input.substring(lastEnd);
            int startSpan = builder.length();
            builder.append(remaining);
            builder.setSpan(new ForegroundColorSpan(currentColor), startSpan, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }

    private static int resolveAnsiColor(String code, int fallback) {
        if (code == null || code.isEmpty() || code.equals("0")) {
            return Color.parseColor("#E2E8F0"); // Reset
        }

        String[] parts = code.split(";");
        for (String part : parts) {
            switch (part) {
                case "30": return Color.parseColor("#4B5563"); // Black/Dark Gray
                case "31": case "91": return Color.parseColor("#EF4444"); // Red / Bright Red
                case "32": case "92": return Color.parseColor("#22C55E"); // Green / Bright Green
                case "33": case "93": return Color.parseColor("#EAB308"); // Yellow / Bright Yellow
                case "34": case "94": return Color.parseColor("#3B82F6"); // Blue / Bright Blue
                case "35": case "95": return Color.parseColor("#A855F7"); // Magenta
                case "36": case "96": return Color.parseColor("#06B6D4"); // Cyan
                case "37": case "97": return Color.parseColor("#F8FAFC"); // White
                case "39": return Color.parseColor("#E2E8F0"); // Default foreground
            }
        }
        return fallback;
    }
}
