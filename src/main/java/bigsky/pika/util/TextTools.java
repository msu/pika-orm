package bigsky.pika.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextTools {

    private TextTools() {
    }

    public static String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    public static String indent(int spaces, String str) {
        return Arrays.stream(str.split("\n"))
                .map(s -> " ".repeat(spaces) + s)
                .collect(Collectors.joining("\n"));
    }

    public static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        char[] chars = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static String snakeCase(String camelCaseString) {
        StringBuilder result = new StringBuilder();
        char[] charArray = camelCaseString.toCharArray();
        boolean lastCharWasUppercase = true;
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (Character.isUpperCase(c)) {
                if (!lastCharWasUppercase) {
                    result.append("_");
                } else if (i > 0 && i + 1 < charArray.length && Character.isLowerCase(charArray[i + 1])) {
                    result.append("_");
                }
                result.append(Character.toLowerCase(c));
                lastCharWasUppercase = true;
            } else {
                lastCharWasUppercase = false;
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String camelCase(String snakeCaseString) {
        StringBuilder result = new StringBuilder();
        char[] charArray = snakeCaseString.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (i == 0) {
                result.append(c);
            } else {
                if (c == '_') {
                    i++;
                    if(i < charArray.length) {
                        result.append(Character.toUpperCase(charArray[i]));
                    }
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    static LinkedHashMap<Pattern, String> INFLECTIONS = new LinkedHashMap<>();

    private static void addInflection(String suffix, String replacement) {
        INFLECTIONS.put(Pattern.compile(".*" + suffix + "$"), replacement);
    }

    static {
        addInflection("[ch](h)", "hes");
        addInflection("(ss)", "sses");
        addInflection("[aeo]l(f)", "ves");
        addInflection("[^d]ea(f)", "ves");
        addInflection("ar(f)", "ves");
        addInflection("[nlw]i(fe)", "ves");
        addInflection("[aeiou](y)", "ys");
        addInflection("(y)", "ies");
    }

    public static String pluralize(String noun) {
        for (Map.Entry<Pattern, String> inflection : INFLECTIONS.entrySet()) {
            Matcher matcher = inflection.getKey().matcher(noun);
            if (matcher.matches()) {
                StringBuilder result = new StringBuilder(noun);
                result.replace(matcher.start(1), matcher.end(1), inflection.getValue());
                return result.toString();
            }
        }
        return noun + "s";
    }

    public static String humanize(String string) {
        String[] splitString = string.split("(?=\\p{Lu})|_");
        StringBuilder result = new StringBuilder();
        for (String str : splitString) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(capitalize(str));
        }
        return result.toString();
    }
}
