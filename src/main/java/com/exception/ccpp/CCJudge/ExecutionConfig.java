package com.exception.ccpp.CCJudge;


import com.exception.ccpp.Common.Helpers;
import com.exception.ccpp.FileManagement.FileManager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



public class ExecutionConfig {
public static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static int docker_cmd_length;
    static {
        docker_cmd_length = getDockerCommand("", DockerImage.C_CPP, false).length;
    }

    public static String[] getDockerCommand(String rootdir, DockerImage image, boolean is_compiling)
    {
        String type = switch (image) {
            case C_CPP -> "judge-slim-c";
            case PYTHON -> "judge-slim-python";
            default -> "judge-java";
        };
        if (is_compiling) return new String[]{"docker", "run", "--rm", "-v", String.format("%s:/code", rootdir), type};
        return new String[]{"docker", "run", "--rm", "-it", "-v", String.format("%s:/code", rootdir), type};
    }

    public static String[] getCompileCommand(FileManager fm) {
        String[] sourceFilenames = fm.getLanguageFiles().stream().map(fm::getRelativePath).map(Path::toString).toArray(String[]::new);
        return getCompileCommand(fm.getLanguage(), fm.getRootdir().toAbsolutePath().toString(), sourceFilenames);
    }

    private static String[] getCompileCommand(String language, String rootdir, String[] sourceFilenames) {
        String[] command;
        switch (language) {
            case "java":
            {
                String[] docker = getDockerCommand(rootdir, DockerImage.JAVA, true);
                command = new String[1 + docker.length + sourceFilenames.length];

                command[docker.length] = "javac";

                System.arraycopy(docker, 0, command, 0, docker.length);
                System.arraycopy(sourceFilenames, 0, command, docker.length + 1, sourceFilenames.length);
                return command;
            }
            case "cpp", "c++", "c":
            {
                String[] docker = getDockerCommand(rootdir, DockerImage.C_CPP, true);
                command = new String[3 + docker.length + sourceFilenames.length];

                command[docker.length] = switch (language) {
                    case "c" -> "gcc";
                    default -> "g++";
                };
                command[docker.length + 1 + sourceFilenames.length] = "-o";
                command[docker.length + 2 + sourceFilenames.length] = "submission";

                System.arraycopy(docker, 0, command, 0, docker.length);
                System.arraycopy(sourceFilenames, 0, command, docker.length + 1, sourceFilenames.length);
                return command;
            }
            default:
                return null;
        }
    }

    public static String[] getExecuteCommand(FileManager fm) {
        String rootdir = fm.getRootdir().toAbsolutePath().toString();
        String[] cmd;
        String[] docker_cmd;
        switch (fm.getLanguage()) {
            case "java":
            {
                cmd= new String[docker_cmd_length + 2];
                docker_cmd = getDockerCommand(rootdir, DockerImage.JAVA, false);
                cmd[docker_cmd_length] = "java";
                cmd[docker_cmd_length + 1] = String.format("./%s",fm.getCurrentFileStringPath().replace(".java","").replaceAll("[\\\\/]",".")); // idk if com.exception.ccpp.Main is in all program
                break;
            }
            case "cpp", "c++", "c":
            {
                cmd = new String[docker_cmd_length + 3];
                docker_cmd = getDockerCommand(rootdir, DockerImage.C_CPP, false);
                int i = 0;
                cmd[docker_cmd_length + i] = "stdbuf";
                cmd[docker_cmd_length + (++i)] = "-o0";
                cmd[docker_cmd_length + (++i)] = "./submission";
                break;
            }
            case "python", "py":
            {
                cmd= new String[docker_cmd_length + 2];
                docker_cmd = getDockerCommand(rootdir, DockerImage.PYTHON, false);
                cmd[docker_cmd_length] = "python";
                cmd[docker_cmd_length + 1] = String.format("./%s",fm.getCurrentFileStringPath());
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported language.");
        };
        System.arraycopy(docker_cmd, 0, cmd, 0, docker_cmd_length);
        return cmd;
    }

    public enum DockerImage {
        JAVA,
        C_CPP,
        PYTHON
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