package de.t14d3.rapunzellib.serverrunner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ServerRunnerMainTest {
    @Test
    void helpReturnsZeroExitCode() throws Exception {
        assertEquals(0, ServerRunnerMain.run(new String[]{"--help"}));
        assertEquals(0, ServerRunnerMain.run(new String[]{"-h"}));
    }

    @Test
    void unknownArgsReturnUsageErrorCode() throws Exception {
        assertEquals(2, ServerRunnerMain.run(new String[]{"foo"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--unknown"}));
    }

    @Test
    void missingValueArgsReturnUsageErrorCode() throws Exception {
        assertEquals(2, ServerRunnerMain.run(new String[]{"--paper-count"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--jvm-arg"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--paper-extra-plugin"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--velocity-extra-plugin"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--replace", "a.txt", "x"}));
    }
}

