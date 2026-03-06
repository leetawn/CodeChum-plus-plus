package com.exception.ccpp.CCJudge;


import com.exception.ccpp.ProcCMD;
import com.exception.ccpp.FileManagement.FileManager;

import java.nio.file.Path;

import static com.exception.ccpp.DockerHandler.getDockerCommand;

public class ExecutionConfig {
public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static ProcCMD getCompileCommand(FileManager fm, String container_id) {
        String[] sourceFilenames = fm.getLanguageFiles().stream().map(fm::getRelativePath).map(Path::toString).toArray(String[]::new);
        return getCompileCommand(fm.getLanguage(), sourceFilenames, container_id);
    }

    private static ProcCMD getCompileCommand(String language, String[] sourceFilenames, String container_id) {
        String sources = String.join(" ", sourceFilenames);
        String[] exec_cmd;
        switch (language) {
            case "java":
            {
                exec_cmd = String.format("javac %s", sources).split(" ");
                break;
            }
            case "cpp", "c++", "c":
            {
                exec_cmd = new String[sourceFilenames.length + 3];
                exec_cmd[0] = switch (language) {
                    case "c" -> "gcc";
                    default -> "g++";
                };
                System.arraycopy(sourceFilenames, 0, exec_cmd, 1, sourceFilenames.length);
                exec_cmd[sourceFilenames.length + 1] = "-o";
                exec_cmd[sourceFilenames.length + 2] = "submission";
//                exec_cmd = String.format("%s %s -o submission", compiler, sources).split(" ");
                break;
            }
            default:
                return null;
        };
        ProcCMD command = getDockerCommand(container_id, exec_cmd);

        return command;
    }

    public static ProcCMD getExecuteCommand(FileManager fm) {
        ProcCMD exec_cmd = getDockerCommand(null, null);
        String run_cmd;
        switch (fm.getLanguage()) {
            case "java":
            {
                run_cmd = String.format("java %s", fm.getCurrentFileStringPath().replace(".java","").replaceAll("[\\\\/]",".")); // idk if com.exception.ccpp.Main is in all program
                System.err.println("Base run_cmd: " + run_cmd);
                break;
            }
            case "cpp", "c++", "c":
            {
                run_cmd = "./submission";
                break;
            }
            case "python", "py":
            {
                run_cmd = String.format("python ./%s", fm.getCurrentFileStringPath());
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported language.");
        };

        return exec_cmd.setRunCMD(run_cmd);
    }



    static String NO_JDK_ERROR = "Java Compiler not found.\nEnsure you are running on a JDK.\n";
    static String NO_C_COMPILER_ERROR =
"""
No C/C++ compiler found.
Required: gcc or clang.

Windows:
  gcc (MSYS2):   https://www.msys2.org/
  clang/LLVM:    https://github.com/llvm/llvm-project/releases
  
macOS:
  Command Line Tools: xcode-select --install
""";

}