package de.t14d3.rapunzellib.commands;

import java.util.concurrent.atomic.AtomicInteger;

final class TestCommandFeatureInstallers {
    private static final AtomicInteger PAPER_INSTALL_CALLS = new AtomicInteger();

    private TestCommandFeatureInstallers() {
    }

    static void reset() {
        PAPER_INSTALL_CALLS.set(0);
    }

    static void recordPaperInstall() {
        PAPER_INSTALL_CALLS.incrementAndGet();
    }

    static int paperInstallCalls() {
        return PAPER_INSTALL_CALLS.get();
    }
}
