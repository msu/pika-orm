// package pika.docs;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class GroupedMethodExtractor {
    public static void main(String[] args) throws IOException {
        String INPUT_JAVA_FILE = "src/main/java/edu/montana/pika/PikaORM.java";
        String OUTPUT_TEXT_FILE = "PikaMethods.md";

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
        
        Pattern classPattern = Pattern.compile("\\b(class|interface|enum)\\s+(\\w+)");
        Pattern methodPattern = Pattern.compile(
                "(?:\\b(public|protected|private|static|final|abstract|synchronized)\\s+)*" +
                "[\\w\\<\\>\\[\\]\\?\\s,]+\\s+" + // return type
                "(\\w+)\\s*" + // method name
                "\\(([^)]*)\\)\\s*" + // parameters
                "(?:throws[\\w\\.,\\s]+)?\\s*\\{");

        Deque<String> classStack = new ArrayDeque<>();
        Deque<Integer> depthStack = new ArrayDeque<>();
        
        int bracketDepth = 0;
        int i = 0;
        int len = code.length();
        
        while (i < len) {
            char c = code.charAt(i);
            if (c == '{') {
                bracketDepth++;
                i++;
            } else if (c == '}') {
                if (!depthStack.isEmpty() && bracketDepth == depthStack.peek()) {
                    classStack.pop();
                    depthStack.pop();
                }
                bracketDepth--;
                i++;
            } else if (Character.isWhitespace(c)) {
                i++;
            } else {
                Matcher classMatcher = classPattern.matcher(code);
                classMatcher.region(i, len);
                if (classMatcher.lookingAt()) {
                    String className = classMatcher.group(2);
                    String fullClassName = classStack.isEmpty() ? className : String.join(".", classStack) + "." + className;
                    classStack.push(className);
                    depthStack.push(bracketDepth + 1);
                    result.put(fullClassName, new ArrayList<>());
                    i = classMatcher.end();
                    continue;
                }
                
                Matcher methodMatcher = methodPattern.matcher(code);
                methodMatcher.region(i, len);
                if (methodMatcher.lookingAt()) {
                    if (!classStack.isEmpty() && bracketDepth == depthStack.peek()) {
                        List<String> classList = new ArrayList<>();
                        classStack.descendingIterator().forEachRemaining(classList::add);
                        String currentClass = String.join(".", classList);
                        
                        String methodName = methodMatcher.group(2);
                        String params = methodMatcher.group(3).trim().replaceAll("\\s+", " ");
                        String methodSig = methodName + "(" + params + ")";
                        
                        result.computeIfAbsent(currentClass, k -> new ArrayList<>()).add(methodSig);
                    }
                    i = methodMatcher.end() - 1;
                    continue;
                }
                
                i++;
            }
        }
        return result;
    }
}