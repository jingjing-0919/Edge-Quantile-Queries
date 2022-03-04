package Query;
import Config.config;

import java.io.IOException;

public class Execute {
    public static void main(String[] args) throws IOException {
        switch (config.Method) {
            case "DFE":
                SingleQuery.SingleQueryRun();
                break;
            case "EDFE":
                SingleQuery.SingleQueryRun();
                break;
            case "LDFE":
                SingleQuery.SingleQueryRun();
                break;
            case "NDFE":
                SingleQuery.SingleQueryRun();
                break;
            case "SDFE":
                SingleQuery.SingleQueryRun();
                break;
            case "BTA":
                SingleQuery.SingleQueryRun();
                break;
            case "QW":
                ConcurrentQuery.ConcurrentQueryRun();
                break;
            case "CB":
                ConcurrentQuery.ConcurrentQueryRun();
                break;
            case "CB-I":
                ConcurrentQuery.ConcurrentQueryRun();
                break;
            case "CB-E":
                ConcurrentQuery.ConcurrentQueryRun();
                break;
            case "CB-R":
                ConcurrentQuery.ConcurrentQueryRun();
                break;
            case "CP":
                ConcurrentQuery.ConcurrentQueryRun();
                break;
        }
    }
}
