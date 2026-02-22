package com.exception.ccpp.Common;
import com.exception.ccpp.GUI.FileExplorer;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

import static com.exception.ccpp.Gang.SlaveManager.romanArmy;
import static com.exception.ccpp.Gang.SlaveManager.slaveWorkers;


public class Helpers {
    public static String[] concatStringArrays(String[]... arrays) {
        // compute total length
        int total = 0;
        for (String[] arr : arrays) {
            total += arr.length;
        }

        // create result
        String[] result = new String[total];

        // copy arrays in order
        int pos = 0;
        for (String[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }

        return result;
    }

    public static String[] mergeStrings(String[] a, String[] b) {
        String[] combined = new String[a.length + b.length];

        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return combined;
    }

    public static String joinStringArrays(String[] a, String[] b, String elemSep, String arraySep) {
        String joinedA = String.join(elemSep, a);
        String joinedB = String.join(elemSep, b);
        return joinedA + arraySep + joinedB;
    }

    public static String stripAnsi(String input) {
        String regex = "\\x1b\\[[\\d;?]*[a-zA-Z]";
        return input.replaceAll(regex, "");
    }
    public static String stripCRLines(String input) {
        String regex = "([^\\n\\r]*\\r(?!\\n))|(\\r)";
        return input.replaceAll(regex, "");
    }
    public static String stripAnsiCRLines(String input) {
        return input;
        //return stripCRLines(stripAnsi(input));
    }


    public static String getPythonLineSeparator() throws IOException, InterruptedException {
        // Command to execute: python -c "import os; print(repr(os.linesep))"
        // Adjust "python" if your interpreter is named "python3"
        ProcessBuilder pb = new ProcessBuilder("python", "-c", "import os; print(repr(os.linesep))");

        // Start the process
        Process p = pb.start();

        // Use a BufferedReader to capture the standard output (stdout)
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {

            // Read the single line of output from the Python script
            String line = reader.readLine();

            // Wait for the process to finish
            int exitCode = p.waitFor();

            if (exitCode == 0 && line != null) {
                // The output will be something like "'\r\n'" or "'\n'".
                // We strip the surrounding single quotes to get the pure sequence.
                return line.replace("'", "").replace("\"", "");
            } else {
                // If Python failed or returned no output, throw an error
                throw new IOException("Python command failed with exit code: " + exitCode);
            }
        }
    }

    public static String getSyntaxHightlighting(String path) {
        String ext = FileExplorer.getFileExtension(path);
        return switch(ext) {
            case "java" -> SyntaxConstants.SYNTAX_STYLE_JAVA;
            case "py" -> SyntaxConstants.SYNTAX_STYLE_PYTHON;
            case "cpp", "hpp" -> SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
            case "c", "h" -> SyntaxConstants.SYNTAX_STYLE_C;
            default ->  SyntaxConstants.SYNTAX_STYLE_NONE;
        };
    }
    
    

    public static void main2(String[] args) {
        // 1. Your specific byte array
        byte[] bytes = {
            27, 91, 48, 109, 27, 91, 48, 75, 69, 110,
            116, 101, 114, 32, 97, 32, 110, 117, 109, 98,
            101, 114, 58, 27, 91, 48, 75, 27, 91, 50,
            54, 71, 49, 48, 48, 27, 91, 63, 50, 53,
            108, 13, 69, 110, 116, 101, 114, 32, 97, 32,
            110, 117, 109, 98, 101, 114, 58, 32, 32, 32,
            32, 32, 32, 32, 32, 32, 32, 49, 48, 48,
            27, 91, 48, 75, 13, 10, 69, 110, 116, 101,
            114, 32, 97, 32, 110, 117, 109, 98, 101, 114,
            32, 121, 58, 27, 91, 48, 75, 27, 91, 49,
            57, 71, 27, 91, 63, 50, 53, 104, 50, 48,
            48, 27, 91, 63, 50, 53, 108, 13, 69, 110,
            116, 101, 114, 32, 97, 32, 110, 117, 109, 98,
            101, 114, 32, 121, 58, 32, 50, 48, 48, 27,
            91, 48, 75, 13, 10, 83, 117, 109, 58, 32,
            51, 48, 48, 27, 91, 48, 75, 13, 10, 68,
            73, 68, 68, 89, 33, 33, 33, 27, 91, 48,
            75, 13, 10, 27, 91, 48, 75, 27, 91, 63,
            50, 53, 104, 10
        };

        // 2. Convert bytes to String
        String dirtyString = new String(bytes);

        String regex = "\\x1b\\[[\\d;?]*[a-zA-Z]";


        System.out.println("Dirty String:");
        for (char c : dirtyString.toCharArray()) {
            if (c == '\r') System.out.print("\\r");
            else if (c == '\n') System.out.print("\\n\n");
            else System.out.print(c);
        }
        System.out.println();
        // 4. Clean the string
        String cleanString = stripAnsi(dirtyString);
        String stripedString = stripCRLines(cleanString.trim());

        // 5. Output (using replace to visualize the \r for this demo)
        System.out.println("Cleaned Output:");
        System.out.println(cleanString.trim());

        System.out.println("\nDebug View (showing \\r):");
        for (char c : cleanString.trim().toCharArray()) {
            if (c == '\r') System.out.print("\\r");
            else if (c == '\n') System.out.print("\\n\n");
            else System.out.print(c);
        }

        System.out.println("\n\nCleaned Striped Output:");
        System.out.println(stripedString);
        System.out.println("\nDebug View (showing \\r):");
        for (char c : stripedString.toCharArray()) {
            if (c == '\r') System.out.print("\\r");
            else if (c == '\n') System.out.print("\\n\n");
            else System.out.print(c);
        }
    }


    public static class SplitTask extends RecursiveTask<String[]> {
        private final String str;
        private final int start, end;
        private static final int THRESHOLD = 1_000_000;

        public SplitTask(String str, int start, int end) {
            this.str = str;
            this.start = start;
            this.end = end;
        }

        @Override
        protected String[] compute() {
            int length = end - start;
            if (length <= THRESHOLD) {
                return str.substring(start, end).split("\\R", -1);
            } else {
                int mid = start + length / 2;

                SplitTask left = new SplitTask(str, start, mid);
                SplitTask right = new SplitTask(str, mid, end);

                left.fork();
                String[] rightResult = right.compute();
                String[] leftResult = left.join();

                // merge left and right
                String[] merged = new String[leftResult.length + rightResult.length];
                System.arraycopy(leftResult, 0, merged, 0, leftResult.length);
                System.arraycopy(rightResult, 0, merged, leftResult.length, rightResult.length);
                return merged;
            }
        }
    }

    public static String[] parallelSplit(String text) {
        return romanArmy.invoke(new SplitTask(text, 0, text.length()));
    }

    public static String[] parallelSplit(String text, int chunkSize) {
        // Step 1: split all lines (single-threaded)
        String[] allLines = text.split("\\R", -1);

        // Step 2: divide into chunks for parallel processing
        int numChunks = (allLines.length + chunkSize - 1) / chunkSize;
        ForkJoinPool pool = ForkJoinPool.commonPool();

        List<RecursiveTask<String[]>> tasks = new ArrayList<>();
        for (int i = 0; i < numChunks; i++) {
            final int start = i * chunkSize;
            final int end = Math.min(allLines.length, (i + 1) * chunkSize);
            tasks.add(new RecursiveTask<>() {
                @Override
                protected String[] compute() {
                    return Arrays.copyOfRange(allLines, start, end);
                }
            });
        }

        // Invoke all tasks in parallel
        List<String[]> chunks = tasks.stream()
                .map(pool::invoke)
                .collect(Collectors.toList());

        // Step 3: flatten into a single String[]
        int totalLength = Arrays.stream(chunks.toArray(new String[0][]))
                .mapToInt(arr -> arr.length)
                .sum();

        String[] result = new String[totalLength];
        int pos = 0;
        for (String[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, pos, chunk.length);
            pos += chunk.length;
        }

        return result;
    }

    public static void main(String[] args) {
        try {
            String lineSep = getPythonLineSeparator();
            System.out.println("The Python os.linesep value is: " + lineSep);

        } catch (IOException | InterruptedException e) {
            System.err.println("Error accessing Python: " + e.getMessage());
        }
        slaveWorkers.shutdown();
    }
}
