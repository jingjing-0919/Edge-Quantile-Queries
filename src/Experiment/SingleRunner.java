package Experiment;

import GreenwaldKhanna.Block;
import GreenwaldKhanna.GKWindow;
import Model.BaseStation;
import Model.Query;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import Config.config;


public class SingleRunner {
    public static double errorBound;

    public static int[] run(ArrayList<BaseStation> arr, Query query) throws IOException {
        errorBound = query.getErrorBound();
        //"DFE", "NDFE", "EDFE", "LDFE", "SDFE", "BTA"

        double[] upper_DFE = new double[arr.size()];
        double[] upper_EDFE = new double[arr.size()];
        double[] upper_LDFE = new double[arr.size()];
        double[] upper_SDFE = new double[arr.size()];
        double[] upper_NDFE = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            upper_DFE[i] = computeDataUpperBound(errorBound, arr, i);
        }
        double[] eta = dataReDistribution(arr, errorBound, upper_DFE);
        double[] eta_EPS = dataReDistributionEpsFirst(arr, errorBound, upper_EDFE);
        double[] eta_UTC = dataReDistributionUTCFirst(arr, errorBound, upper_LDFE);
        double[] eta_Random = dataReDistributionRandom(arr, errorBound, upper_SDFE);
        double[] eta_OEP = dataReDistributionOEP(arr, errorBound, upper_NDFE);
        double[] eta_BTA = dataReDistribution_BTA(arr,errorBound);

        int dataSize = query.dataSize;

        String dataFile = "./Data/synthetic_data_sample_25m.txt";
        int[] delay;
        switch (config.Method) {
            case "DFE":
                delay = execute(arr, eta, upper_DFE, dataSize, dataFile, 1);
                break;
            case "EDFE":
                delay = execute(arr, eta_EPS, upper_EDFE, dataSize, dataFile, 2);
                break;
            case "LDFE":
                delay = execute(arr, eta_UTC, upper_LDFE, dataSize, dataFile, 3);
                break;
            case "SDFE":
                delay = execute(arr, eta_Random, upper_SDFE, dataSize, dataFile, 4);
                break;
            case "NDFE":
                delay = execute(arr, eta_OEP, upper_NDFE, dataSize, dataFile, 5);
                break;
            case "BTA":
                delay = execute(arr, eta_BTA, upper_DFE, dataSize, dataFile, 6);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + config.Method);
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter("TestResultLog/SingleQueryTestResult.txt", true));
        bw.write("\r\n");
        bw.write("-------------------------------------------------------------------------------------------");
        bw.write("\r\n");
        bw.close();
        return delay;

    }

    public static int[]  execute(ArrayList<BaseStation> arr, double[] eta, double[] upper, int dataSize, String csvFile1, int type) throws IOException {
        double temp = 0;
        if (type == 1) {
            arr.sort(Comparator.comparingDouble(BaseStation::getId));
        } else if (type == 2) {
            arr.sort(Comparator.comparingDouble(BaseStation::getE));
        } else if (type == 3) {
            arr.sort(Comparator.comparingDouble(BaseStation::getUTC));
        } else if (type == 4) {
            arr.sort(Comparator.comparingDouble(BaseStation::getLatitude));
        } else if (type == 5) {
            arr.sort(Comparator.comparingDouble(BaseStation::getLongitude));
        }
        int []res = new int[2];
        int delay = 0;
        int mem = 0;
        for (int i = 0; i < arr.size(); i++) {
            if (eta[i]==0){
                continue;
            }
            if (eta[i] == upper[i]) {
                res= run(arr.get(i), (int) (dataSize * eta[i]), csvFile1, 0.5, (int) temp * dataSize, eta[i], 0, type, upper[i]);
            } else {
                res = run(arr.get(i), (int) (dataSize * eta[i]), csvFile1, 0.5, (int) temp * dataSize, eta[i], 1, type, upper[i]);
            }
            temp = temp + eta[i];
            if (delay < res[0]){
                delay = res[0];
            }
            mem = mem+res[1];

        }
        double errorRate = 0;
        for (int i = 0; i < arr.size(); i++) {
            errorRate = errorRate + eta[i] * arr.get(i).getE();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter("TestResultLog/SingleQueryTestResult.txt", true));
        bw.write("\r\n");
        bw.write("ErrorRate: " + errorRate);
        bw.write("\r\n");
        bw.close();
        res[0] = delay;
        res[1] = mem;

        return res;
    }

    public static double[] dataReDistribution_BTA(ArrayList<BaseStation> arr, double errorBound) {
        double[] eta = new double[arr.size()];
        int index = 0;
        double UTC = 1;
        for (int i = 0;i < arr.size();i++){
            if(arr.get(i).getE() < errorBound){
                if (arr.get(i).getUTC() < UTC){
                    index = i;
                }
            }
        }
        eta[index] = 1;
        return eta;
    }

    public static double[] dataReDistribution(ArrayList<BaseStation> arr, double errorBound, double[] upper) {
        int[] set = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            set[i] = 0;
        }
        arr.sort(Comparator.comparingDouble(BaseStation::getId));//使之有序
        for (int i = 0; i < arr.size(); i++) {
            upper[i] = computeDataUpperBound(errorBound, arr, i);
        }
        double eta = 1;
        double[] eta_final = new double[arr.size()];
        while (eta > 0) {
            double z = 0;
            for (int i = 0; i < arr.size(); i++) {
                if (set[i] == 0) {
                    z = z + 1 / arr.get(i).getUTC();
                }
            }
            boolean flag = true;
            for (int i = 0; i < arr.size(); i++) {
                if (set[i] == 0) {
                    double eta_i = Math.round(eta * 100 / (arr.get(i).getUTC() * z)) / 100.0;
                    if (arr.get(i).getE() > errorBound && eta_i > upper[i]) {
                        eta_final[i] = upper[i];
                        set[i] = 1;
                        flag = false;
                        eta = eta - upper[i];
                    } else {
                        eta_final[i] = eta_i;
                    }
                }
            }
            if (flag) {
                break;
            }
        }
        return eta_final;
    }


    public static double[] dataReDistributionUTCFirst(ArrayList<BaseStation> arr, double errorBound, double[] upper) {  //多出来的放到UTC最小的去
        int[] set = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            set[i] = 0;
        }
        double eta = 1;
        double[] eta_final = new double[arr.size()];
        double z = 0;
        arr.sort(Comparator.comparingDouble(BaseStation::getUTC));//使之有序
        for (int i = 0; i < arr.size(); i++) {
            upper[i] = computeDataUpperBound(errorBound, arr, i);
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                z = z + 1 / arr.get(i).getUTC();
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                double eta_i = Math.round(100 / (arr.get(i).getUTC() * z)) / 100.0;
                if (arr.get(i).getE() > errorBound && eta_i > upper[i]) {
                    eta_final[i] = upper[i];
                    set[i] = 1;
                    eta = eta - upper[i];
                } else {
                    eta_final[i] = eta_i;
                    eta = eta - eta_i;
                }
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0 && eta > 0) {
                if (eta <= upper[i] - eta_final[i]) {
                    eta_final[i] = eta_final[i] + eta;
                    eta = 0;
                } else {
                    eta = eta - upper[i] + eta_final[i];
                    eta_final[i] = upper[i];
                }
            }
        }
        return eta_final;
    }


    public static double[] dataReDistributionEpsFirst(ArrayList<BaseStation> arr, double errorBound, double[] upper) {  //多出来的放到Eps最小的去
        int[] set = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            set[i] = 0;
        }
        double eta = 1;
        double[] eta_final = new double[arr.size()];
        double z = 0;
        arr.sort(Comparator.comparingDouble(BaseStation::getE));//使之有序
        for (int i = 0; i < arr.size(); i++) {
            upper[i] = computeDataUpperBound(errorBound, arr, i);
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                z = z + 1 / arr.get(i).getUTC();
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                double eta_i = Math.round(100 / (arr.get(i).getUTC() * z)) / 100.0;
                if (arr.get(i).getE() > errorBound && eta_i > upper[i]) {
                    eta_final[i] = upper[i];
                    set[i] = 1;
                    eta = eta - upper[i];
                } else {
                    eta_final[i] = eta_i;
                    eta = eta - eta_i;
                }
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0 && eta > 0) {
                if (eta <= upper[i] - eta_final[i]) {
                    eta_final[i] = eta_final[i] + eta;
                    eta = 0;
                } else {
                    eta = eta - upper[i] + eta_final[i];
                    eta_final[i] = upper[i];
                }
            }
        }
        return eta_final;
    }


    public static double[] dataReDistributionRandom(ArrayList<BaseStation> arr, double errorBound, double[] upper) {  //多出来的随机放
        int[] set = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            set[i] = 0;
        }
        arr.sort(Comparator.comparingDouble(BaseStation::getLatitude));//使之有序
        for (int i = 0; i < arr.size(); i++) {
            upper[i] = computeDataUpperBound(errorBound, arr, i);
        }


        double eta = 1;
        double[] eta_final = new double[arr.size()];
        double z = 0;
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                z = z + 1 / arr.get(i).getUTC();
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                double eta_i = Math.round(100 / (arr.get(i).getUTC() * z)) / 100.0;
                if (arr.get(i).getE() > errorBound && eta_i > upper[i]) {
                    eta_final[i] = upper[i];
                    set[i] = 1;
                    eta = eta - upper[i];
                } else {
                    eta_final[i] = eta_i;
                    eta = eta - eta_i;
                }
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0 && eta > 0) {
                if (eta <= upper[i] - eta_final[i]) {
                    eta_final[i] = eta_final[i] + eta;
                    eta = 0;
                } else {
                    eta = eta - upper[i] + eta_final[i];
                    eta_final[i] = upper[i];
                }
            }
        }
        return eta_final;
    }

    public static double computeDataUpperBound(double errorBound, ArrayList<BaseStation> arr, int i) {
        double e_min = 1;
        for (int j = 0; j < arr.size(); j++) {
            if (e_min >= arr.get(j).getE() && j != i) {
                e_min = arr.get(j).getE();
            }
        }
        if (arr.get(i).getE() > errorBound) {
            return Math.round((errorBound - e_min) * 100 / (arr.get(i).getE() - e_min)) / 100.0;
        } else {
            return 1;
        }
    }


    public static double[] dataReDistributionOEP(ArrayList<BaseStation> arr, double errorBound, double[] upper) {  //多出来的随机放
        int[] set = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            set[i] = 0;
        }
        arr.sort(Comparator.comparingDouble(BaseStation::getLongitude));//使之有序
        for (int i = 0; i < arr.size(); i++) {
            upper[i] = computeDataUpperBound(errorBound, arr, i);
        }


        double eta = 1;
        double[] eta_final = new double[arr.size()];
        double z = 0;
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                z = z + 1 / arr.get(i).getUTC();
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0) {
                double eta_i = Math.round(100 / (arr.get(i).getUTC() * z)) / 100.0;
                if (arr.get(i).getE() > errorBound && eta_i > upper[i]) {
                    eta_final[i] = upper[i];
                    set[i] = 1;
                    eta = eta - upper[i];
                } else {
                    eta_final[i] = eta_i;
                    eta = eta - eta_i;
                }
            }
        }
        for (int i = 0; i < arr.size(); i++) {
            if (set[i] == 0 && eta > 0) {
                if (eta <= upper[i] - eta_final[i]) {
                    eta_final[i] = eta_final[i] + eta;
                    eta = 0;
                } else {
                    eta = eta - upper[i] + eta_final[i];
                    eta_final[i] = upper[i];
                }
            }
        }
        return eta_final;
    }

    public static int[] run (BaseStation baseStation, int size, String csvFile, double phi, int start_time, double percent, int bound, int type, double upper) throws IOException {
        Runtime r =  Runtime.getRuntime();
        r.gc();
        long start_total = r.totalMemory();
        long start_free = r.freeMemory();
        BufferedWriter bw = new BufferedWriter(new FileWriter("TestResultLog/SingleQueryTestResult.txt",true));
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
            bw.write("DFE:  id: "+ baseStation.getId());
        }
        else if (type == 2){
            bw.write("EDFE:  id: "+ baseStation.getId());
        }
        else if (type == 3){
            bw.write("LDFE：  id: "+ baseStation.getId());
        }
        else if (type == 4){
            bw.write("SDFE:  id: "+ baseStation.getId());
        }
        else if (type == 5){
            bw.write("NDFE:  id: "+baseStation.getId());
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

