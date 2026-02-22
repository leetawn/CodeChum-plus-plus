package com.exception.ccpp;


import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;

public class DockerHandler {
    public final static String[] IMAGES;
    public final static String[] CONTAINER_IDS;
//    ConcurrentHashMap<String, boolean>
    public final static LinkedHashSet<String>[] CONTAINER_INSTANCES = new LinkedHashSet[]{
            new LinkedHashSet<String>(),
            new LinkedHashSet<String>(),
            new LinkedHashSet<String>()
    };
    static {
        IMAGES = new String[DockerImageType.DOCKER_IMAGES_MAX.ordinal()];
        IMAGES[DockerImageType.JAVA.ordinal()] = "judge-java";
        IMAGES[DockerImageType.C_CPP.ordinal()] = "judge-slim-c";
        IMAGES[DockerImageType.PYTHON.ordinal()] = "judge-slim-python";

        CONTAINER_IDS = new String[DockerImageType.DOCKER_IMAGES_MAX.ordinal()];
        CONTAINER_IDS[DockerImageType.JAVA.ordinal()] = "ccj-worker-java";
        CONTAINER_IDS[DockerImageType.C_CPP.ordinal()] = "ccj-worker-c";
        CONTAINER_IDS[DockerImageType.PYTHON.ordinal()] = "ccj-worker-py";
    }

    public static String getContainerID(DockerImageType type) {
        return CONTAINER_IDS[type.ordinal()];
    }
    public static String getImage(DockerImageType type) {
        return IMAGES[type.ordinal()];
    }


    public static String[] getDockerCommand(String container_id, boolean is_compiling, String[] exec_cmd)
    {
        int size = (is_compiling) ? 3 : 4;
        String[] cmd = new String[exec_cmd.length + size];
        cmd[0] = "docker";
        cmd[1] = "exec";
        if (is_compiling) cmd[2] = container_id;
        else {
            cmd[2] = "-it";
            cmd[3] = container_id;
        }
        System.arraycopy(exec_cmd, 0, cmd, size, exec_cmd.length);
        return cmd;
    }

    public static void stopDockerWorker(DockerImageType image)
    {
        StringBuilder sb = new StringBuilder("\"");
        HashSet<String> instances_id = CONTAINER_INSTANCES[image.ordinal()];
        int size = instances_id.size();
        int i =0;
        for (String id : CONTAINER_INSTANCES[image.ordinal()]) {
            sb.append("docker rm -f -v ");
            sb.append(id);
            sb.append("2>/dev/null || true");
            if (i++ < size-1)
            {
                sb.append(" && ");
            }
        }
        sb.append("\"");

        String[] cmd =  {"bash", "-c", sb.toString()};
        try {
            new ProcessBuilder(cmd).start().waitFor();
            instances_id.clear();
        } catch (Exception e) {
            System.err.println("[DockerHandler.startDockerWorker]: Error starting docker worker!");
        }
    }

    public static String createWorker(String rootdir, DockerImageType image) {
        String id = Long.toString(System.currentTimeMillis(), 36) + Integer.toString((int)(Math.random() * 1_000_000), 36);
        String docker_image = getImage(image);
        String[] cmd = String.format("docker run --network none -dit --name %s -v %s:/code %s bash", id, rootdir, docker_image).split(" ");
        try {
            new ProcessBuilder(cmd).redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).directory(new File(rootdir)).start().waitFor();
            CONTAINER_INSTANCES[image.ordinal()].add(id);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("[DockerHandler.startDockerWorker]: Error starting docker worker!");
        }
        return id;
    }

    public enum DockerImageType {
        JAVA,
        C_CPP,
        PYTHON,
        DOCKER_IMAGES_MAX
    }

}
