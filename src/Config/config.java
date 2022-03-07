package Config;

public class config {

    public static String Method = "CB";
    /* choose from all the baseline we have:
       ["DFE", "NDFE", "EDFE", "LDFE", "SDFE", "BTA"] for singleQuery
       ["QW", "CB", "CB-I", "CB-E", "CB-R", "CP"] for ConcurrentQuery
    */

    public static String DataFile = "./Data/geolife1.txt";

    public static String BaseStationFile = "./Data/baseStations16.txt";
    /* choose from "./src/Data/baseStations16.txt" or "./src/Data/baseStations24.txt" or "./src/Data/baseStations32.txt"
       to decide the number of baseStations
     */

    public static String QueryFile = "./Data/Query50.txt";
    /* choose from  ["./src/Data/Query50.txt" , "./src/Data/Query50.txt" , "./src/Data/Query50.txt" ,
       "./src/Data/Query50.txt" , "./src/Data/Query50.txt" ] to decide the number of queries
     */
    public static double Alpha = 1.0;
    // choose from [0.8, 0.9, 1.0, 1.1, 1.2]

    public static int Cell_length = 1000;
    // choose from [250, 500, 1000]

    public static int dataVolume = 500000;
    // dataVolume for one Cell. UN = #cells * dataVolume.

    /* when Cell_length = 1000, there are 25 cells in total, for UN = 25M, dataVolume = 1000000;
       when Cell_length = 500, there are 100 cells in total, for UN = 25M, dataVolume = 250000;
       when Cell_length = 250, there are 400 cells in total, for UN = 25M, dataVolume = 62500;
     */

    public static int x_length = 5000;
    public static int y_length = 5000;

    public static double relaxRate = 0.2;

}
