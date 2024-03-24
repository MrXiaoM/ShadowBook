package top.mrxiaom.fix.book.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused"})
public class Util {
    public static Optional<Double> parseDouble(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    public static Optional<Integer> parseInt(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    public static Optional<Long> parseLong(String s) {
        if (s == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    public static <T extends Enum<?>> T valueOr(Class<T> c, String s, T def) {
        if (s == null) return def;
        for (T t : c.getEnumConstants()) {
            if (t.name().equalsIgnoreCase(s)) return t;
        }
        return def;
    }

    public static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }
        return sw.toString();
    }

    public static <T> List<T> split(Pattern regex, String s, Function<RegexResult, T> transform) {
        List<T> list = new ArrayList<>();
        int index = 0;
        Matcher m = regex.matcher(s);
        while (m.find()) {
            int first = m.start();
            int last = m.end();
            if (first > index) {
                T value = transform.apply(new RegexResult(false, s.substring(index, first)));
                if (value != null) list.add(value);
            }
            T value = transform.apply(new RegexResult(true, s.substring(first, last)));
            if (value != null) list.add(value);
            index = last;
        }
        if (index < s.length()) {
            T value = transform.apply(new RegexResult(false, s.substring(index)));
            if (value != null) list.add(value);
        }
        return list;
    }

    public static class RegexResult {
        public boolean isMatched;
        public String text;

        public RegexResult(boolean isMatched, String text) {
            this.isMatched = isMatched;
            this.text = text;
        }
    }
}
