package de.t14d3.rapunzellib.inventory;

import java.util.concurrent.atomic.AtomicInteger;

final class TestInventoryFeatureInstallers {
    private static final AtomicInteger PAPER_INSTALL_CALLS = new AtomicInteger();
    private static final AtomicInteger VELOCITY_INSTALL_CALLS = new AtomicInteger();

    private TestInventoryFeatureInstallers() {
    }

    static void reset() {
        PAPER_INSTALL_CALLS.set(0);
        VELOCITY_INSTALL_CALLS.set(0);
    }

    static int paperInstallCalls() {
        return PAPER_INSTALL_CALLS.get();
    }

    static int velocityInstallCalls() {
        return VELOCITY_INSTALL_CALLS.get();
    }

    static void recordPaperInstall() {
        PAPER_INSTALL_CALLS.incrementAndGet();
    }

    static void recordVelocityInstall() {
        VELOCITY_INSTALL_CALLS.incrementAndGet();
    }
}
