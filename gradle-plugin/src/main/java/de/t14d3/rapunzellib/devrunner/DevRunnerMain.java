package de.t14d3.rapunzellib.devrunner;

public final class DevRunnerMain {

    private DevRunnerMain() {
    }

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } catch (Throwable t) {
            System.err.println("[devrunner] Unhandled exception: " + t.getMessage());
            t.printStackTrace(System.err);
            code = 1;
        }
        System.exit(code);
    }

    public static int run(String[] args) {
        DevRunnerConfig cfg;
        try {
            cfg = DevRunnerConfigParser.parse(args);
        } catch (DevRunnerUsageException e) {
            System.out.println(e.getMessage());
            return e.code();
        }

        try {
            return new DevRunnerOrchestrator(cfg).run();
        } catch (Exception e) {
            System.err.println("[devrunner] Fatal error: " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }
}
