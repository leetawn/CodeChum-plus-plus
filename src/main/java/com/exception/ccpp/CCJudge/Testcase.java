package com.exception.ccpp.CCJudge;

import java.io.Serializable;

public class Testcase implements Serializable {
    private static final long serialVersionUID = 202512L;
    private final String expected_output;
    private final String[] inputs;
    // TODO: private boolean hidden;

    public Testcase(String[] inputs, String expected_output) {
        this.inputs = inputs;
        this.expected_output = expected_output;
    }

    public String getExpectedOutput() {
        return expected_output;
    }
    public String[] getInputs() {
        return inputs;
    }
}
