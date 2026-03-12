package com.exception.ccpp;


import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;

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
        IMAGES[DockerImageType.JAVA.ordinal()] = "ccjudge-java";
        IMAGES[DockerImageType.C_CPP.ordinal()] = "ccjudge-c";
        IMAGES[DockerImageType.PYTHON.ordinal()] = "ccjudge-python";

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


    public static ProcCMD getDockerCommand(String container_id, String[] compile_cmd)
    {
        return new ProcCMD(container_id, compile_cmd);
    }

    public static void stopAllDockers()
    {
        stopDockerWorker(DockerImageType.C_CPP);
        stopDockerWorker(DockerImageType.JAVA);
        stopDockerWorker(DockerImageType.PYTHON);
    }

    public static void stopDockerWorker(DockerImageType image)
    {
        StringBuilder sb = new StringBuilder();
        HashSet<String> instances_id = CONTAINER_INSTANCES[image.ordinal()];
        int size = instances_id.size();
        int i =0;
        for (String id : CONTAINER_INSTANCES[image.ordinal()]) {
            try {
                new ProcessBuilder(String.format("docker rm -f -v %s", id).split(" ")).start().waitFor();

            } catch (Exception e) {
                System.err.println("[DockerHandler.startDockerWorker]: Error starting docker worker!");
            }
        }
        instances_id.clear();

    }

    public static LinkedHashSet<String> getContainerInstances(DockerImageType image)
    {
        return CONTAINER_INSTANCES[image.ordinal()];
    }

    public static String createWorker(String rootdir, DockerImageType image) {
        String id = Long.toString(System.currentTimeMillis(), 36) + Integer.toString((int)(Math.random() * 1_000_000), 36);
        String docker_image = getImage(image);
        String[] cmd = String.format("docker run --network none -di --name %s -v %s:/code %s bash", id, rootdir, docker_image).split(" ");
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
