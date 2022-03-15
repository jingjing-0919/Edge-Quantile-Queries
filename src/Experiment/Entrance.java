package Experiment;

import Config.config;

import java.io.IOException;

public class Entrance {
    public static void main(String[] args) throws IOException {
        if (config.Method.equals("DFE") || config.Method.equals("EDFE") || config.Method.equals("LDFE") ||
                config.Method.equals("NDFE") || config.Method.equals("SDFE") || config.Method.equals("BTA")) {
            SingleQuery.SingleQueryRun();
        } else {
            ConcurrentQuery.ConcurrentQueryRun();
        }
    }
}

