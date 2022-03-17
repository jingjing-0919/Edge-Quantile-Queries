package Config;

public class config {
    /*
       default setting:
          Method = "CB"
          DataFile = "./Resources/Data/SyntheticData/synthetic_data_sample_2m.txt"
          BaseStationFile = "./Resources/BaseStation/BaseStations16.txt"
          QueryFile = "./Resources/Query/Query50.txt"
          Alpha = 1.0
          Cell_length = 1000
          dataVolume = 1000000
          relaxRate = 0.2
          RelaxMethod = "minMax"
          useSocket = false
     */


    public static String Method = "LDFE";
    /* choose from all the baseline we have:
       ["DFE", "NDFE", "EDFE", "LDFE", "SDFE", "BTA"] for singleQuery
       ["QW", "CB", "CB-I", "CB-E", "CB-R", "CP"] for ConcurrentQuery
    */


    public static String DataFile = "./Resources/Data/SyntheticData/synthetic_data_sample_25m.txt";
    /*
        "./Resources/Data/SyntheticData/synthetic_data_sample_2m.txt" for ConcurrentQuery
        "./Resources/Data/SyntheticData/synthetic_data_sample_25m.txt" for SingleQuery
     */

    public static String BaseStationFile = "./Resources/BaseStation/BaseStations16.txt";
    /* choose from "./Resources/BaseStation/BaseStations16.txt" or "./Resources/BaseStation/BaseStations24.txt" or
        "./Resources/BaseStation/BaseStations32.txt" to decide the number of baseStations
        choose "./Resources/BaseStation/BaseStations_Socket.txt" if ues Socket model.
     */

    public static String QueryFile = "./Resources/Query/Query50.txt";
    /* choose from  ["./Resources/Query/Query30.txt" , "./Resources/Query/Query40.txt" , "./Resources/Query/Query50.txt" ,
       "./Resources/Query/Query60.txt" , "./Resources/Query/Query70.txt" ] to decide the number of queries
     */
    public static double Alpha = 1.0;
    // choose from [0.8, 0.9, 1.0, 1.1, 1.2]

    public static int Cell_length = 1000;
    // choose from [250, 500, 1000]

    public static int dataVolume = 1000000;
    // dataVolume for one Cell. UN = #cells * dataVolume.

    /* when Cell_length = 1000, there are 25 cells in total, for UN = 25M, dataVolume = 1000000;
       when Cell_length = 500, there are 100 cells in total, for UN = 25M, dataVolume = 250000;
       when Cell_length = 250, there are 400 cells in total, for UN = 25M, dataVolume = 62500;
     */

    public static double RelaxRate = 0.2;

    public static String RelaxMethod = "minMax";
    /*
        choose from ["minMax", "minAvg"]
     */
    public static boolean useSocket = false;

}
