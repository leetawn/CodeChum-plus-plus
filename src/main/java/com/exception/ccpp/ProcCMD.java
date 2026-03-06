package com.exception.ccpp;

public class ProcCMD implements Cloneable{
    private String[] cmd;
    boolean is_exec_cmd;

    private static final int
            INDEX_CONTAINER_ID = 2,
            INDEX_RUN_CMD = 4,
            INDEX_INPUTS = 5,
            INDEX_OUTPUT_FILENAME = 6;
    private static final int
            COMPILE_CMD_SIZE = 3,
            EXEC_CMD_SIZE = 7;


    ProcCMD(String container_id, String[] compile_cmd) {
        this.is_exec_cmd = compile_cmd == null;

        if (!is_exec_cmd) {
            cmd = new String[compile_cmd.length + COMPILE_CMD_SIZE];
            System.arraycopy(compile_cmd, 0, cmd, COMPILE_CMD_SIZE, compile_cmd.length);
        } else {
            cmd = new String[EXEC_CMD_SIZE];
            cmd[3] = "trunner";
            // cmd[4] = <run_cmd>
            // cmd[5] = <inputs>
            cmd[INDEX_OUTPUT_FILENAME] =  "exec_output.txt";
        }
        cmd[0] = "docker";
        cmd[1] = "exec";
        cmd[2] = container_id;
    }

    public String[] getCmd() {
        return cmd;
    }

    public ProcCMD setOutputFilename(String filename)
    {
        if (is_exec_cmd)
            cmd[INDEX_OUTPUT_FILENAME] = filename;
        return this;
    }

    public ProcCMD setRunCMD(String run_cmd)
    {
        if (is_exec_cmd)
            cmd[INDEX_RUN_CMD] = run_cmd;
        return this;
    }

    public ProcCMD setInputs(String inputs)
    {
        if (is_exec_cmd)
            cmd[INDEX_INPUTS] = inputs;
        return this;
    }

    public ProcCMD setContainerID(String container_id) {
        if (is_exec_cmd)
            cmd[INDEX_CONTAINER_ID] = container_id;
        return this;
    }

    @Override
    public ProcCMD clone() {
        try {
            ProcCMD clone = (ProcCMD) super.clone();
            is_exec_cmd = clone.is_exec_cmd;
            cmd = clone.cmd.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
