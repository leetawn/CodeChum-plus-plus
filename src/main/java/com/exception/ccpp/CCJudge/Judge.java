package com.exception.ccpp.CCJudge;

import com.exception.ccpp.Common.Helpers;
import com.exception.ccpp.Debug.CCLogger;
import com.exception.ccpp.Debug.DebugLog;
import com.exception.ccpp.DockerHandler;
import com.exception.ccpp.FileManagement.FileManager;
import com.exception.ccpp.FileManagement.SFile;
import com.exception.ccpp.ProcCMD;
import com.pty4j.PtyProcessBuilder;

import javax.tools.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.exception.ccpp.Gang.SlaveManager.romanArmy;
import static com.exception.ccpp.Gang.SlaveManager.slaveWorkers;
import static com.exception.ccpp.DockerHandler.DockerImageType;

public class Judge {
    public static final String TASK_TESTCASE_RUNNER = "testcase_runner";
    public static final String TASK_JUDGE = "judge";
    public static final String TASK_CLEANUP = "cleanup";

//    TODO@CCPP2.0 Add centralize tracking of threads for forcibly killing them
//    private static Map<JudgeSlave, Boolean> activeSlaves = new ConcurrentHashMap<>();
    private static ConcurrentLinkedDeque<Process> runningPTY = new ConcurrentLinkedDeque<>();
    private static DebugLog judge_logger = DebugLog.getInstance();
    public static Map<String, String> env;
    static {
        env = new HashMap<>(System.getenv());
        if (!env.containsKey("TERM")) env.put("TERM", "dumb");
    }

    static final long TIME_LIMIT_MS = 2000; // TLE, 2secs
    static final long COMPILE_TIME_LIMIT_SEC = 10;



    /******************** SINGLE-THREADED *************************/
    // will output an Array of SubmissionRecord, check the Submission Record definition
    // for TERMINAL USE ONLY
    static void judgeInteractively(
            ProcCMD cmd,
            FileManager fm,
            String[] testInputs,
            Consumer<SubmissionRecord[]> callback,
            CCLogger logger
    ) {
        if (logger == null) logger = judge_logger;
        final CCLogger f_logger = logger;
        killJudge(true);

        romanArmy.submit(TASK_JUDGE,() -> {
            Future<SubmissionRecord> f = slaveWorkers.submit(TASK_TESTCASE_RUNNER,new JudgeSlave(cmd, fm, testInputs, null, 1, f_logger));
            SubmissionRecord verdict = new SubmissionRecord(JudgeVerdict.RE, "Runtime Error Buddy", null);
            try {
                verdict = f.get();
            } catch (InterruptedException e) {}
            catch (ExecutionException e) { /*Normal shit*/
                f_logger.errln("[Judge.judge]: Execution Error while waiting for submission!");
            }
            callback.accept(new SubmissionRecord[]{verdict});
            killJudge(false);
        });
    }

    /******************** MULTI-THREADED *************************/

    public static void killJudge(boolean includeJudge)
    {
        slaveWorkers.killAll(TASK_TESTCASE_RUNNER, true);
        if (includeJudge) romanArmy.killAll(TASK_JUDGE, true);
        for(Process p : runningPTY)
        {
            p.destroyForcibly();
        }
        runningPTY.clear();
    }

    public static void judge(FileManager fm, CCQuestion tf, BiConsumer<SubmissionRecord[], Integer> callback)  {
        TerminalApp.kill();
        killJudge(true);
        romanArmy.submit(TASK_JUDGE,() -> {
            SubmissionRecord judge_res;
            String rootdir = fm.getRootdir().toString();
            String language = fm.getLanguage();
            DockerImageType dt = switch (language.toLowerCase())
            {
                case "cpp", "c++", "c" -> DockerImageType.C_CPP;
                case "python", "py" -> DockerImageType.PYTHON;
                default -> DockerImageType.JAVA;
            };

            Map<Testcase, String> testcases = tf.getTestcases();
            int testcases_size = testcases.size();

            ArrayList<Testcase>  testcases_keys = new ArrayList<>(testcases.keySet());

            int i = 0;
            SubmissionRecord[] verdicts = new SubmissionRecord[testcases_size];
            for (Testcase tc : testcases_keys) {
                verdicts[i++] = new SubmissionRecord(JudgeVerdict.UE, "Unknown Error", tc);
            }
            // COMPILE

            String container_id = DockerHandler.getContainerInstances(dt).getFirst();
            judge_logger.logln("[Judge.judge]: Compiling...");
            try {
                judge_res = compile(fm, judge_logger, container_id); // WAITS TO FINISH
                if (judge_res.verdict() == JudgeVerdict.CE) {
                    judge_logger.errln("[Judge.judge]: Compiler Error!");
                    recordCopy(verdicts, judge_res, testcases_size);
                    parallelCleanup(fm);

                    end_judge(callback, verdicts, JudgeVerdict.CE);
                    return;
                }
            } catch (Exception e) {
                String fmsg = "Judge System Failure: " + e.getMessage();
                judge_logger.errln(fmsg);
                e.printStackTrace();
                judge_res = new SubmissionRecord(JudgeVerdict.JSF, fmsg, null);
                recordCopy(verdicts, judge_res, testcases_size);
                parallelCleanup(fm);
                end_judge(callback, verdicts, JudgeVerdict.JSF);
                return;
            }
            judge_logger.logln("[Judge.judge]: Compilation Done.");

            // judgeInteractively (Parallel)
            ProcCMD cmd = ExecutionConfig.getExecuteCommand(fm);
            List<Future<SubmissionRecord>> futures = new ArrayList<>();

            cmd.setContainerID(container_id); // temporary
            Callable<SubmissionRecord>[] tasks = new Callable[testcases_size];

            i = 0;
            for (Map.Entry<Testcase, String> entry : testcases.entrySet())
            {
                Testcase tc = entry.getKey();
                String uuid = entry.getValue();
                tasks[i++] = new JudgeSlave(cmd, rootdir, language, tc, uuid, i, null);
            }

            try {
                futures = slaveWorkers.invokeAll(TASK_TESTCASE_RUNNER, List.of(tasks));
            } catch (InterruptedException e) {}

            i = 0;
            int status = 0;
            for  (Future<SubmissionRecord> r : futures) {
                SubmissionRecord sr = null;
                try {
                    sr = r.get();
                } catch (InterruptedException | ExecutionException e) {}

                verdicts[i++]
                        .setVerdict(sr.verdict())
                        .setOutput(sr.output());
                // RE/TLE
                if (status == 0 && sr.verdict() != JudgeVerdict.NONE) { status = sr.verdict(); }
                else if (sr.verdict() == JudgeVerdict.RE) { status = sr.verdict(); }

            }

            if (DebugLog.DEBUG_ENABLED)
                for (int j = 0; j < verdicts.length; j++) {
                    judge_logger.logf("[Judge.judge] Testcase %d exit code: %s\n", j+1, verdicts[j].verdictName());
                }


            judge_logger.logln("[Judge.judge]: Returning Results...\n");
            end_judge(callback, verdicts, status);
        });
    }

    private static void end_judge(BiConsumer<SubmissionRecord[], Integer> callback, SubmissionRecord[] verdicts, Integer status)
    {
        callback.accept(verdicts, status);
        killJudge(false);
    }

    // please terminate all workers
    static void parallelCleanup(FileManager fm) {
        slaveWorkers.submit(TASK_CLEANUP,() -> {
            cleanup(fm);
        });
    }

    static void cleanup(FileManager fm) {
        String language = fm.getLanguage();
        try {
            if (language.equals("java")) {
                for (SFile file : fm.getLanguageFiles()) {
                    String className = file.getPath().toAbsolutePath().toString().replace(".java", ".class");
                    judge_logger.logln("Judge.cleanup: Deleting " + className);
                    Files.deleteIfExists(Paths.get(className));
                }
            } else if (language.equals("cpp") || language.equals("c")) {
                Path p = Paths.get(fm.getRootdir().toAbsolutePath().toString(),"Submission.exe");
                judge_logger.logln("Judge.cleanup: Deleting " + p);
                if (Files.exists(p)) { Files.delete(p); }
                judge_logger.logln("Judge.cleanup: Deleted " + p);
            }
        } catch (IOException ignored) {}
    }

    static SubmissionRecord compile(FileManager fm, CCLogger logger, String container_id) throws IOException, InterruptedException {
        if (logger == null) logger = judge_logger;
        ProcCMD compileCommand = null;
        switch (fm.getLanguage().toLowerCase()) {
            case "java", "c", "cpp", "c++": {
                compileCommand = ExecutionConfig.getCompileCommand(fm, container_id);
                break;
            }
            default: {
                logger.logln("-> No compilation required for " + fm.getLanguage());
                return new SubmissionRecord(JudgeVerdict.NONE, "No compilation required for " + fm.getLanguage(), null);
            }
        }
        System.err.println("-> Compiling: " + String.join(" ", compileCommand.getCmd()));

        ProcessBuilder pb = new ProcessBuilder(compileCommand.getCmd());
        pb.directory(fm.getRootdir().toFile());

        return startCompilation(pb);
    }

    private static SubmissionRecord startJavaCompilation(FileManager fm, CCLogger logger) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return null;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {

            Iterable<Path> sourcePaths = fm.getFiles().stream()
                    .filter(f -> f.getPath().toString().endsWith(".java"))
                    .map(SFile::getPath)
                    .collect(Collectors.toList());

            if (!sourcePaths.iterator().hasNext()) {
                return new SubmissionRecord(JudgeVerdict.CE, "No Java source files (.java) found for compilation.", null);
            }

            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(sourcePaths);

            Path outputDir = fm.getRootdir();
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            Iterable<String> options = Arrays.asList(
                    "-d", outputDir.toAbsolutePath().toString()
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    compilationUnits
            );

            boolean success = task.call();

            if (!success) {
                StringWriter output = new StringWriter();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    output.write(String.format("Error [%s] on line %d in %s:\n%s\n",
                            diagnostic.getKind().toString(),
                            diagnostic.getLineNumber(),
                            diagnostic.getSource() != null ? diagnostic.getSource().getName() : "Unknown File",
                            diagnostic.getMessage(null)));
                }
                String ceMsg = "Compilation Errors (javax.tools):\n" + output.toString();
                logger.logln(ceMsg);
                return new SubmissionRecord(JudgeVerdict.CE, ceMsg, null);
            }

            logger.logln("Java Compilation Success!");
            return new SubmissionRecord(JudgeVerdict.NONE, "Compilation Successful (javax.tools)", null);
        }
    }

    private static SubmissionRecord startCompilation(ProcessBuilder pb) throws IOException, InterruptedException {
        Process compileProcess = pb.start();
        String errorOutput = readStream(compileProcess.getErrorStream());

        if (compileProcess.waitFor(COMPILE_TIME_LIMIT_SEC, TimeUnit.SECONDS) && compileProcess.exitValue() != 0) {
            String ceMsg = "Compiler Errors ("+ compileProcess.exitValue() +"):\n" + errorOutput;
            judge_logger.errln(ceMsg);
            compileProcess.destroy();
            return new SubmissionRecord(JudgeVerdict.CE, ceMsg, null);
        }
        compileProcess.destroy();
        return new SubmissionRecord(JudgeVerdict.NONE, "Compilation Successful", null);
    }

    static String readStream(InputStream is) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static void recordCopy(SubmissionRecord[] dest, SubmissionRecord src, int ex_out_size) {
        for (int i = 0; i < ex_out_size; i++) {
            dest[i]
                    .setVerdict(src.verdict())
                    .setOutput(src.output());
        }
    }

    static class JudgeSlave implements Callable<SubmissionRecord> {
        private static final long INPUT_DELAY_MS = 50;
        private final ProcCMD execCMD;
        private final String rootdir, language;
        private final CCLogger logger;
        private final String uuid;
        private Process process = null;
        private Testcase tc;
        int tc_num;
        private SubmissionRecord result;
        StringBuffer transcript = new StringBuffer();

        JudgeSlave(
                ProcCMD cmd,
                FileManager fm, String[] inputs,
                String expected_output,
                int testcase_number,
                CCLogger logger
        ) {
            this(cmd, fm.getRootdir().toString(), fm.getLanguage(), new Testcase(inputs, expected_output), UUID.randomUUID().toString(), testcase_number, logger);
        }

        JudgeSlave(ProcCMD cmd, String rootdir, String language, Testcase tc, String uuid, int testcase_number, CCLogger logger) {
            if (logger == null) logger = judge_logger;
            this.execCMD = cmd.clone();
            this.uuid = uuid;
            this.tc = tc;
            this.rootdir = rootdir;
            this.language = language;
            this.tc_num = testcase_number;
            this.logger = Judge.judge_logger; // FIXME: Changer to logger;
            result = new SubmissionRecord(JudgeVerdict.ESF, "Execution System Failure (IOException)\n", tc);
        }

        public void halt() {
            if (process != null && process.isAlive()) {
                process.destroy();
                logger.logln("[JudgeSlave]: Process destroyed!");
            }
        }
        public void mutilate() {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                logger.logln("[JudgeSlave]: Process forcefully destroyed!");
            }
        }

        // Slave done with work
        private SubmissionRecord finishQuota() {
            mutilate();
            runningPTY.remove(process);
            return result;
        }

        private void finalizeTranscript()
        {
            try {
                Path s = Paths.get(rootdir, uuid);
                String content = Files.readString(s);
                transcript.append(content);
            } catch (IOException e) {
                System.err.println("Reading Ouputfile failed!");
            }
        }



        @Override
        //throws Exception
        // RETURNS Verdicts: RE/TLE/NONE
        public SubmissionRecord call()  {
            execCMD.setInputs(String.join("\n", tc.getInputs())).setOutputFilename(uuid);
            try {
                process = new PtyProcessBuilder(execCMD.getCmd())
                        .setEnvironment(env)
                        .setRedirectErrorStream(true)
                        .setDirectory(rootdir)
                        .setConsole(true)
                        .start();
            } catch (IOException e) {
                finalizeTranscript();
                return finishQuota();
            }
            runningPTY.add(process);

            try {
                process.waitFor(TIME_LIMIT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                finalizeTranscript();
                return finishQuota().setOutput("Execution Interrupted\n");
            }

            if (process.isAlive()) return tleExit();
            else if (process.exitValue() != 0) return rteExit();
            return goodExit();
        }

        private SubmissionRecord tleExit()
        {
            finalizeTranscript();
            return finishQuota()
                .setVerdict(Judge.JudgeVerdict.TLE)
                .setOutput(Helpers.stripAnsiCRLines(
                        transcript
                            .append("\nTLE (Time Limit Exceeded)\n")
                            .toString()
                    ).trim()
                );
        }

        private SubmissionRecord rteExit() {
            finalizeTranscript();
            String errorOutput = "UnknownError";
            try {
                errorOutput = readStream(process.getErrorStream());
            } catch (IOException e) {}
            System.err.printf("[TC %d]Runtime Error Details:\n%s\n", tc_num, errorOutput);
            return finishQuota()
                    .setVerdict(Judge.JudgeVerdict.RE)
                    .setOutput(Helpers.stripAnsiCRLines(
                                    transcript
                                            .append("RTE (Runtime Error) - Exit Code: ")
                                            .append(process.exitValue())
                                            .append("\n")
                                            .toString()
                            ).trim()
            );
        }

        private SubmissionRecord goodExit()
        {
            finalizeTranscript();
            final String raw = transcript.toString().trim();
            System.out.println(raw);
            return finishQuota().setVerdict(JudgeVerdict.NONE).setOutput(raw);
        }
    }

    static public class JudgeVerdict {
        final public static int
                AC   = 0b000000000, // Accepted
                WA   = 0b000000001, // Wrong Answer
                WAE  = 0b000000011, // Wrong Answer Excess
                CE   = 0b000000101, // Compile Error
                RE   = 0b000001001, // Runtime Error
                TLE  = 0b000010001, // Time Limit Exceeded
                MLE  = 0b000100001, // Memory Limit Exceeded
                UE   = 0b111111111, // Unknown Error. We the devs, idk and idc what the fuck you are facing rn
                ESF  = 0b101000001, // Execution System Failure, Not the Users Fault!
                JSF  = 0b110000001, // Judge System Failure
                NONE = 0b100000001; // USED FOR CREATION ONLY

        public static String getName(int verdictCode)
        {
            return switch (verdictCode) {
                case AC -> "AC";
                case WA, WAE -> "WA";
                case CE -> "CE";
                case RE -> "RE";
                case TLE -> "TLE";
                case MLE -> "MLE";
                case UE -> "UE";
                case ESF -> "ESF";
                case JSF -> "JSF";
                default -> "NONE";
            };
        }
    }

    public static void main(String[] args) {
    }

}