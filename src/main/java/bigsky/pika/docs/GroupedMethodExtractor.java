package bigsky.pika.docs;


import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class GroupedMethodExtractor {

    // === Customize your input file here ===
    private static final String INPUT_JAVA_FILE = "src/main/java/bigsky/pika/PikaORM.java";
    private static final String OUTPUT_TEXT_FILE = "PikaMethods.md";

    public static void main(String[] args) throws IOException {
        String code = Files.readString(Paths.get(INPUT_JAVA_FILE));
        code = removeCommentsAndStrings(code);

        Map<String, List<String>> classMethodsMap = extractClassMethods(code);

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(OUTPUT_TEXT_FILE))) {
            for (Map.Entry<String, List<String>> entry : classMethodsMap.entrySet()) {
                writer.write("# " + entry.getKey());
                writer.newLine();
                writer.newLine();

                for (String method : entry.getValue()) {
                    writer.write("### " + method);
                    writer.newLine();
                    writer.newLine();
                    writer.write("```java");
                    writer.newLine();
                    writer.write(method);
                    writer.newLine();
                    writer.write("```");
                    writer.newLine();
                    writer.newLine();

                    writer.write("**Description**:  \n_Describe what this method does..._");
                    writer.newLine();
                    writer.newLine();

                    writer.write("**Parameters**:  ");
                    if (method.contains("(") && !method.contains("()")) {
                        String paramList = method.substring(method.indexOf('(') + 1, method.indexOf(')')).trim();
                        String[] params = splitParameters(paramList);
                        writer.newLine();
                        for (String param : params) {
                            String[] parts = param.trim().split("\\s+");
                            String name = parts.length > 1 ? parts[parts.length - 1] : parts[0];
                            writer.write("- `" + name + "`: _Describe " + name + "_");
                            writer.newLine();
                        }
                    } else {
                        writer.write("  \n_None_");
                        writer.newLine();
                    }

                    writer.newLine();
                    writer.write("**Returns**:  \n_Describe the return value..._");
                    writer.newLine();
                    writer.write("\n---\n\n");
                }
            }
        }

        System.out.println("✅ Formatted markdown file created: " + OUTPUT_TEXT_FILE);
    }

    /**
     * Splits parameter list while respecting generic type brackets
     */
    private static String[] splitParameters(String paramList) {
        if (paramList.trim().isEmpty()) {
            return new String[0];
        }

        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int bracketDepth = 0;

        for (int i = 0; i < paramList.length(); i++) {
            char c = paramList.charAt(i);

            if (c == '<') {
                bracketDepth++;
            } else if (c == '>') {
                bracketDepth--;
            } else if (c == ',' && bracketDepth == 0) {
                // Only split on commas that are not inside generic brackets
                params.add(current.toString().trim());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        // Add the last parameter
        if (current.length() > 0) {
            params.add(current.toString().trim());
        }

        return params.toArray(new String[0]);
    }

    private static String removeCommentsAndStrings(String code) {
        String noBlockComments = code.replaceAll("(?s)/\\*.*?\\*/", "");
        String noLineComments = noBlockComments.replaceAll("//.*(?=\\n)", "");
        String noStrings = noLineComments.replaceAll("\"(\\\\.|[^\"\\\\])*\"", "\"\"");
        return noStrings;
    }

    private static Map<String, List<String>> extractClassMethods(String code) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        Deque<String> classStack = new ArrayDeque<>();

        Pattern classPattern = Pattern.compile("\\b(class|interface|enum)\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(code);

        Pattern methodPattern = Pattern.compile(
                "(public|protected|private|static|final|abstract|synchronized|\\s)*" +
                        "[\\w\\<\\>\\[\\]]+\\s+" + // return type
                        "(\\w+)\\s*" +             // method name
                        "\\(([^)]*)\\)\\s*" +      // parameters
                        "(throws[\\w\\.,\\s]+)?\\s*\\{");

        int index = 0;
        while (index < code.length()) {
            int nextClass = indexOfMatch(classPattern, code, index);
            int nextMethod = indexOfMatch(methodPattern, code, index);

            if ((nextClass != -1 && (nextMethod == -1 || nextClass < nextMethod))) {
                classMatcher.region(index, code.length());
                if (classMatcher.find()) {
                    String className = classMatcher.group(2);
                    String fullClassName = String.join(".", classStack) +
                            (classStack.isEmpty() ? "" : ".") + className;
                    classStack.push(className);
                    result.put(fullClassName, new ArrayList<>());
                    index = classMatcher.end();
                }
            } else if (nextMethod != -1) {
                Matcher methodMatcher = methodPattern.matcher(code);
                methodMatcher.region(index, code.length());
                if (methodMatcher.find()) {
                    if (!classStack.isEmpty()) {
                        List<String> classList = new ArrayList<>();
                        classStack.descendingIterator().forEachRemaining(classList::add);
                        String currentClass = String.join(".", classList);
                        String methodSig = methodMatcher.group(2) + "(" + methodMatcher.group(3).trim() + ")";
                        result.computeIfAbsent(currentClass, k -> new ArrayList<>()).add(methodSig);
                    }
                    index = methodMatcher.end();
                }
            } else {
                break;
            }

            if (index < code.length() && code.charAt(index) == '}') {
                if (!classStack.isEmpty()) {
                    classStack.pop();
                }
                index++;
            } else {
                index++;
            }
        }

        return result;
    }

    private static int indexOfMatch(Pattern pattern, String text, int start) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find(start) ? matcher.start() : -1;
    }
}