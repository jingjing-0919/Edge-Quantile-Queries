package Experiment;

import java.io.*;
import java.util.ArrayList;
import GreenwaldKhanna.Block;
import GreenwaldKhanna.GKWindow;
import Model.BaseStation;

public class Run {
    public static int[] run (BaseStation baseStation, int size, String csvFile, double phi, int start_time, double percent, int bound, int type, double upper) throws IOException {
        Runtime r =  Runtime.getRuntime();
        r.gc();
        long start_total = r.totalMemory();
        long start_free = r.freeMemory();
        BufferedWriter bw = new BufferedWriter(new FileWriter("FinalSingleTestResult.txt",true));
        double e = baseStation.getE();


        int n = 0;//current number of summary
        size = size - size % 100;
        int n_delay = 0;
        int size_delay = size * baseStation.getDelayPer100() / 100;
        int blocks = (int) Math.floor(2/e);

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
        //ArrayList<Integer> quantile = GKWindow.quantile(phi, n, e, blist);
        long end = System.currentTimeMillis();
        long end_total = r.totalMemory();
        long end_free = r.freeMemory();
        long mem = ((end_total- end_free) - (start_total - start_free))/1024/1024;

        //System.out.println(mem) ;

//------------------------------------------log_start-------------------------------------------------------------------
        bw.write("\r\n");
        bw.write(csvFile+"  ");
        if (type == 1){
            bw.write("EBR:  id: "+ baseStation.getId());
        }
        else if (type == 2){
            bw.write("EPS:  id: "+ baseStation.getId());
        }
        else if (type == 3){
            bw.write("UTC：  id: "+ baseStation.getId());
        }
        else if (type == 4){
            bw.write("RAN:  id: "+ baseStation.getId());
        }
        else if (type == 5){
            bw.write("OEP:  id: "+baseStation.getId());
        }
        else if (type == 6){
            bw.write("BTA:  id: "+baseStation.getId());
        }
        if (bound == 0){
            bw.write(" ,dataSize : "+size + " , percent："+percent+" ,upperbound：  "+ upper+ " , e = "+e+" ,delay "+ (end - start)+ " ms" + ",reach dataUpperBound");
        }
        else {
            bw.write(" ,dataSize : "+size + " , percent："+percent+" ,upperbound：  "+ upper+ " , e = "+e+" ,delay "+ (end - start)+ " ms");
        }
        bw.write("  \n");
        bw.close();
//------------------------------------------log_end---------------------------------------------------------------------
        int []res = new int[2];
        res[0] = (int) (end-start);
        res[1] = (int) mem;
        return res;
    }
}
