plugins {
    alias(libs.plugins.database.spool.module.conventions)
}

dependencies {
    // In-memory JDBC driver for unit tests (spool's own tests use H2 too).
    testImplementation("com.h2database:h2:2.2.224")
}
