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


    /* the baseline methods:
       ["DFE", "NDFE", "EDFE", "LDFE", "SDFE", "BTA"] for singleQuery
       ["QW", "CB", "CB-I", "CB-E", "CB/R", "CP"] for ConcurrentQuery
    */
    public static String Method = "CB";

    /* the number of base stations:
       "./src/Data/BaseStations[16|24|32].txt"
    */
    public static String BaseStationFile = "./Data/BaseStations16.txt";

    /* the number of base stations:
       "./src/Data/Query[30|40|50|60].txt"
     */
    public static String QueryFile = "./Data/Query50.txt";

    /* the error bound strictness \alpha
       [0.8, 0.9, 1.0, 1.1, 1.2]
    */
    public static double Alpha = 1.0;

    /* the cell side length \mathit{ll}
       [250, 500, 1000]
     */
    public static int Cell_length = 1000;

    /* dataVolume for one Cell; UN = \# of cells * dataVolume.
       when Cell_length = 1000, there are 25 cells in total, for UN = 25M, dataVolume = 1000000;
       when Cell_length = 500, there are 100 cells in total, for UN = 25M, dataVolume = 250000;
       when Cell_length = 250, there are 400 cells in total, for UN = 25M, dataVolume = 62500;
     */
    public static int dataVolume = 500000;

    /* relaxing factor
     */
    public static double RelaxRate = 0.2;

    /* reserach project objective: minMax or minAvg
     */
    public static String RelaxMethod = "minMax";

    /* real mobility data
     */
    public static String DataFile = "./Data/geolife1.txt";

    public static boolean useSocket = false;

}
