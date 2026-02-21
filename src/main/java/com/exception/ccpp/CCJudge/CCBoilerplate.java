package com.exception.ccpp.CCJudge;

import java.io.Serializable;
public class CCBoilerplate implements Serializable {
    private static final long serialVersionUID = 202602L;
    public String code;
    public String filename;
    public boolean is_hidden;
    public boolean is_readonly;

    public CCBoilerplate(String code, String filename, boolean is_hidden, boolean is_readonly) {
        this.code = code;
        this.filename = filename;
        this.is_hidden = is_hidden;
        this.is_readonly = is_readonly;
    }
}
