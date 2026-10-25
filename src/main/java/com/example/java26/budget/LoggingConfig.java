package com.example.java26.budget;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LoggingConfig {
    public static final String PACKAGE_LOGGER_NAME = "com.example.java26";

    private static final Logger PACKAGE_LOGGER = Logger.getLogger(PACKAGE_LOGGER_NAME);

    private LoggingConfig() {
    }

    public static void init(Path logFile) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$-7s %3$s: %5$s%6$s%n");
        PACKAGE_LOGGER.setUseParentHandlers(false);
        PACKAGE_LOGGER.setLevel(Level.FINE);
        try {
            FileHandler handler = new FileHandler(logFile.toString(), true);
            handler.setEncoding("UTF-8");
            handler.setLevel(Level.FINE);
            handler.setFormatter(new SimpleFormatter());
            PACKAGE_LOGGER.addHandler(handler);
        } catch (IOException | SecurityException e) {
            PACKAGE_LOGGER.setUseParentHandlers(true);
            PACKAGE_LOGGER.log(Level.WARNING,
                    "Kunde inte öppna loggfilen " + logFile + " - loggar till konsoler i stället", e);
        }
    }
}
