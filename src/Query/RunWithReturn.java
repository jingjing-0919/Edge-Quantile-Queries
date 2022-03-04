package Query;

import java.io.*;
import java.util.ArrayList;

import Greenwald_khanna.Block;
import Greenwald_khanna.GKWindow;

public class RunWithReturn {
    public static ArrayList<Integer> run (Cell cell,BaseStation baseStation,int size,String csvFile,double phi,int start_time) throws IOException {

        double e = baseStation.getE();
        int n = 0;//current number of summary
        size = size - size % 100;
        int n_delay = 0;
        int size_delay = size * baseStation.getDelayPer100() / 100;
        int blocks = 1;
        ArrayList<Block> blist = new ArrayList<Block>(blocks);
        ArrayList<Block> blist_delay = new ArrayList<>(blocks);
        int[] arr_data = new int[size+2];
        long[] arr_time = new long [size+2];
        int temp = 0;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        int count = 0;
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                // use comma as separator
                if (start_time <= count && count < start_time + size ){
                    String[] country = line.split(cvsSplitBy);
                    arr_data[temp] = Integer.parseInt(country[0]);
                    arr_time[temp] = Long.parseLong(country[3]);
                    temp++;
                }
                count++;
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }

        long start = System.currentTimeMillis();
        if (baseStation.getDelayPer100() == 0){
            while (n < size){
                GKWindow.greenwald_khanna_window(n, arr_data[n], size, e, blist);
                n++;
            }
        }
        else {
            while (n < size) {
                for (int i =0;i <100;i++){
                    GKWindow.greenwald_khanna_window(n, arr_data[n], size, e, blist);
                    n++;
                }
                for (int i = 0; i < baseStation.getDelayPer100();i++){
                    GKWindow.greenwald_khanna_window(n_delay,arr_data[n_delay],size_delay,e,blist_delay);
                }
            }
        }
        long end = System.currentTimeMillis();
        ArrayList<Integer> quantile = GKWindow.quantile(phi, n, e, blist);

//------------------------------------------log_start-------------------------------------------------------------------
        BufferedWriter bw = new BufferedWriter(new FileWriter("src/TestResultLog/ConcurrentQueryTestResult.txt",true));
        bw.write("\n");
        bw.write("Query.BaseStation id: "+ baseStation.getId()+"\n");
        bw.write("dataSize: "+size+ "\n");
        bw.write("GK  cost: "+ (end - start) + "ms"+ " error: "+ e + "\n");
        bw.close();
//------------------------------------------log_end---------------------------------------------------------------------

        quantile.add((int) (end-start));
        return quantile;
    }
}
