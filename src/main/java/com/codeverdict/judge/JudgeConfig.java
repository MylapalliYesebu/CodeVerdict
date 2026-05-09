package com.codeverdict.judge;

import java.io.File;

public final class JudgeConfig {
    public static final int COMPILE_TIMEOUT_SECONDS = 10;
    public static final int EXECUTION_TIMEOUT_SECONDS = 5;
    public static final int MAX_OUTPUT_LENGTH = 10000;
    public static final String WORK_DIR = System.getProperty("java.io.tmpdir") + File.separator + "codeverdict" + File.separator + "submissions" + File.separator;

    private JudgeConfig() {}
}
