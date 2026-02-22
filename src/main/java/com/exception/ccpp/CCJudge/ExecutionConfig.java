package com.exception.ccpp.CCJudge;


import com.exception.ccpp.FileManagement.FileManager;

import java.nio.file.Path;

import static com.exception.ccpp.DockerHandler.DockerImageType;
import static com.exception.ccpp.DockerHandler.getDockerCommand;

public class ExecutionConfig {
public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static String[] getCompileCommand(FileManager fm, String container_id) {
        String[] sourceFilenames = fm.getLanguageFiles().stream().map(fm::getRelativePath).map(Path::toString).toArray(String[]::new);
        return getCompileCommand(fm.getLanguage(), sourceFilenames, container_id);
    }

    private static String[] getCompileCommand(String language, String[] sourceFilenames, String container_id) {

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
                String compiler = switch (language) {
                    case "c" -> "gcc";
                    default -> "g++";
                };
                exec_cmd = String.format("%s %s -o submission", compiler, sources).split(" ");
                break;
            }
            default:
                return null;
        };
        String[] command = getDockerCommand(container_id, true, exec_cmd);

        return command;
    }


    public static void setExecCmdContainerID(String[] cmd, String id) {
        cmd[3] = id;
    }

    public static String[] getExecuteCommand(FileManager fm) {
        String rootdir = fm.getRootdir().toAbsolutePath().toString();
        String[] exec_cmd;
        switch (fm.getLanguage()) {
            case "java":
            {
                exec_cmd = String.format("java %s", fm.getCurrentFileStringPath().replace(".java","").replaceAll("[\\\\/]",".")).split(" "); // idk if com.exception.ccpp.Main is in all program
                break;
            }
            case "cpp", "c++", "c":
            {
                exec_cmd = "./submission".split(" ");
                break;
            }
            case "python", "py":
            {
                exec_cmd = String.format("python %s", String.format("./%s",fm.getCurrentFileStringPath())).split(" ");
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported language.");
        };
        return getDockerCommand(rootdir, false, exec_cmd);
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