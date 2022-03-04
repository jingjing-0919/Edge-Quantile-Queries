package Util;

import Config.config;
import Query.BaseStation;
import Query.Cell;
import Query.Query;
import Query.RunWithReturn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ConcurrentQueryUtil {
    public static ArrayList<Integer> execute(Cell cell, int id, ArrayList<BaseStation> arr, HashMap<BaseStation,Double> yita, int dataSize, String csvFile1) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("./src/TestResultLog/ConcurrentQueryTestResult.txt", true));
        bw.write(" \r\n");
        bw.write("\r\n");
        bw.write("GridID: "+ id);
        bw.close();
        double temp = 0;
        ArrayList<Integer> quantile = new ArrayList<>();
        int delayMax = 0;
        for (BaseStation baseStation : arr) {
            if (yita.get(baseStation) != 0.0){
                ArrayList<Integer> arrayList = RunWithReturn.run(cell,baseStation, (int) (dataSize * yita.get(baseStation)), csvFile1, 0.5, (int) temp * dataSize);
                int index = arrayList.size()-1;
                int delay = arrayList.get(index);
                arrayList.remove(index);
                quantile.addAll(arrayList);
                if(delay > delayMax){
                    delayMax = delay;
                }
            }
            temp = temp + yita.get(baseStation);
        }
        double errorRate = 0;
        for (BaseStation baseStation : arr) {
            errorRate = errorRate + yita.get(baseStation) * baseStation.getE();
        }
        cell.delay = delayMax;

        return quantile;
    }

    public static Cell checkBottleneck1(ArrayList<Cell> cells){
        Cell bottleneck = cells.get(0);
        for (Cell cell : cells) {
            if (bottleneck.delay < cell.delay && cell.set.size() != 0) {
                bottleneck = cell;
            }
        }
        return bottleneck;
    }

    public static Cell checkBottleneck(ArrayList<Cell> cells, ArrayList<Query>queries){
        int []cnt = new int[cells.size()];
        for (Query query : queries) {
            for (int j = 0; j < query.covered.size(); j++) {
                double max = 0;
                int index = 0;
                if (max < query.covered.get(j).delay) {
                    max = query.covered.get(j).delay;
                    index = j;
                }
                cnt[index]++;
            }
        }
        int max = 0;
        int index = 0;
        for (int l = 0;l < cnt.length;l++){
            if (max < cnt[l]){
                max = cnt[l];
                index = l;
            }
        }
        return cells.get(index);
    }



    public static void Calculate(double errorBound, Cell cell){//distribute the data under the given errorBound
        cell.yita.clear();
        double [] upper = new double[cell.arr.size()];
        ArrayList<BaseStation> arr = cell.arr;
        int[] set = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            set[i] = 0;
        }
        for (int i = 0; i < arr.size(); i++) {
            upper[i] = computeDataUpperBound(errorBound, arr, i);
        }
        double yita = 1;
        double[] yita_final = new double[arr.size()];
        while (yita > 0) {
            double z = 0;
            for (int i = 0; i < arr.size(); i++) {
                if (set[i] == 0) {
                    z = z + 1 / arr.get(i).getUTC();
                }
            }
            boolean flag = true;
            for (int i = 0; i < arr.size(); i++) {
                if (set[i] == 0) {
                    double yita_i = Math.round(yita * 100 / (arr.get(i).getUTC() * z)) / 100.0;
                    if (arr.get(i).getE() > errorBound && yita_i > upper[i]) {
                        yita_final[i] = upper[i];
                        set[i] = 1;
                        flag = false;
                        yita = yita - upper[i];
                    } else {
                        yita_final[i] = yita_i;
                    }
                }
            }
            if (flag) {
                break;
            }
        }
        for (int i = 0;i < yita_final.length;i++){
            cell.yita.put(arr.get(i),yita_final[i]);
            cell.delay = yita_final[i] * cell.dataVolume * cell.arr.get(i).getUTC();
            cell.error = errorBound;
        }
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

    public static boolean distribute(double delay, Cell cell){
        cell.arr.sort(Comparator.comparingDouble(BaseStation::getE));
        HashMap<BaseStation,Double> yita = new HashMap<>();
        double data = cell.dataVolume;
        int index = 0;
        double temp_e = 0;
        while (data > 0 && index < cell.arr.size()){
            BaseStation cur = cell.arr.get(index);
            double cur_data = delay / cur.getUTC();
            if (cur_data < data){
                data = data - cur_data;
                yita.put(cur,cur_data/ cell.dataVolume);
                index++;
                temp_e = temp_e + cur.getE() * cur_data;
            }
            else {
                yita.put(cur,data/ cell.dataVolume);
                temp_e = temp_e + cur.getE() * data;
                data = 0;
            }
        }
        for (int i = 0; i < cell.arr.size(); i++){
            if (!yita.containsKey(cell.arr.get(i))){
                yita.put(cell.arr.get(i),0.0);
            }
        }

        double error = cell.error * cell.dataVolume;
        double error_increase = temp_e - error;
        boolean flag = true;
        for (int i = 0; i < cell.set.size(); i++){
            Query query = cell.set.get(i);
            if (query.error + error_increase/query.dataSize > query.getErrorBound()){
                flag = false;
                break;
            }
        }
        if (flag && data == 0){
            cell.yita = yita;
            cell.delay = delay;
            cell.error = temp_e/ cell.dataVolume;
            return  true;
        }
        else {
            return false;
        }



    }



    public static void CalculateError(ArrayList<Query>checkList){
        for (Query cur : checkList) {
            int temp_N = 0;
            double temp_e = 0;
            for (int j = 0; j < cur.covered.size(); j++) {
                temp_e = temp_e + 0.1*calculateIGError(cur,cur.covered.get(j));
                temp_N = temp_N + cur.covered.get(j).dataVolume;
            }
            cur.error = temp_e / temp_N;
            cur.dataSize = temp_N;
        }
    }










    public static int check(Query query, Cell cell) {
        int query_XLeft = query.getX_left();
        int query_XRight = query.getX_right();
        int query_YLeft = query.getY_left();
        int query_YRight = query.getY_right();

        boolean x = cell.getX_right() <= query_XLeft || cell.getX_left() >= query_XRight;
        boolean y = cell.getY_right() <= query_YLeft || cell.getY_left() >= query_YRight;

        boolean x1 = cell.getX_left() >= query_XLeft && cell.getX_right() <= query_XRight;
        boolean y1 = cell.getY_left() >= query_YLeft && cell.getY_right() <= query_YRight;

        if (x1 & y1) {
            return 0;
        } else if (!x & !y) {
            return 1;
        } else {
            return -1;
        }
    }

    public static void checkIG(Query query) {
        double y = 0;
        int N = 0;
        for (int j = 0; j < query.covered.size(); j++) {
            y = y + query.covered.get(j).error * query.covered.get(j).dataVolume;
            N = N + query.covered.get(j).dataVolume;
        }
        for (int j = 0; j < query.intersecting.size(); j++) {
            N = N + query.intersecting.get(j).dataVolume;
        }
        for (int i = 0; i < query.intersecting.size(); i++) {
            Cell IG = query.intersecting.get(i);
            calculateIG(query, IG, N);
        }
    }

    public static void calculateIG(Query query, Cell cell, int N) {
        ArrayList<Integer> arrX = new ArrayList<>();
        ArrayList<Integer> arrY = new ArrayList<>();
        arrX.add(query.getX_left());
        arrX.add(query.getX_right());
        arrX.add(cell.getX_left());
        arrX.add(cell.getX_right());
        Collections.sort(arrX);
        arrY.add(query.getY_left());
        arrY.add(query.getY_right());
        arrY.add(cell.getY_right());
        arrY.add(cell.getY_left());
        Collections.sort(arrY);

        int cell_length = config.Cell_length;

        int intersectArea = (arrX.get(2) - arrX.get(1)) * (arrY.get(2) - arrY.get(1));
        int irrelevantArea = cell_length * cell_length - intersectArea;
        intersectArea = (intersectArea * 100 / (cell_length * cell_length))*N/100;
        irrelevantArea = (irrelevantArea*100 / (cell_length * cell_length))*N/100;

        double ErrorIG = irrelevantArea + cell.error * intersectArea ;
        double ErrorEG = intersectArea ;


        if (ErrorEG >= ErrorIG) {
            query.covered.add(cell);
            query.intersecting.remove(cell);
            cell.set.add(query);
        }
    }

    public static double calculateIGError(Query query, Cell cell){
        ArrayList<Integer> arrX = new ArrayList<>();
        ArrayList<Integer> arrY = new ArrayList<>();
        arrX.add(query.getX_left());
        arrX.add(query.getX_right());
        arrX.add(cell.getX_left());
        arrX.add(cell.getX_right());
        Collections.sort(arrX);
        arrY.add(query.getY_left());
        arrY.add(query.getY_right());
        arrY.add(cell.getY_right());
        arrY.add(cell.getY_left());
        Collections.sort(arrY);
        int cell_length = config.Cell_length;

        int intersectArea = (arrX.get(2) - arrX.get(1)) * (arrY.get(2) - arrY.get(1));
        int irrelevantArea = cell_length * cell_length - intersectArea;
        intersectArea = (intersectArea *100/ (cell_length * cell_length))* cell.dataVolume/100;
        irrelevantArea = (irrelevantArea*100 / (cell_length * cell_length))* cell.dataVolume/100;

        return (irrelevantArea + cell.error * intersectArea );
    }


    public static boolean calculate(Query query,BaseStation baseStation){
        boolean x1 = baseStation.getLongitude() > query.getX_left() && baseStation.getLongitude() < query.getX_right();
        boolean y1 = baseStation.getLatitude() > query.getY_left() && baseStation.getLatitude() < query.getY_right();
        return x1 && y1;
    }

    public static int calculateEGError(Query query, Cell cell) {

        ArrayList<Integer> arrX = new ArrayList<>();
        ArrayList<Integer> arrY = new ArrayList<>();
        arrX.add(query.getX_left());
        arrX.add(query.getX_right());
        arrX.add(cell.getX_left());
        arrX.add(cell.getX_right());
        Collections.sort(arrX);
        arrY.add(query.getY_left());
        arrY.add(query.getY_right());
        arrY.add(cell.getY_right());
        arrY.add(cell.getY_left());
        Collections.sort(arrY);
        int cell_length = config.Cell_length;

        int intersectArea = (arrX.get(2) - arrX.get(1)) * (arrY.get(2) - arrY.get(1));
        intersectArea = (intersectArea *100/ (cell_length * cell_length))* cell.dataVolume/100;

        return  intersectArea ;

    }


    public static boolean calculate(Cell cell, BaseStation baseStation) {
        int x = baseStation.getLongitude();
        int y = baseStation.getLatitude();
        int r = baseStation.getRadius();

        double distance1 = calculate(cell.getX_left(), cell.getY_left(), x, y);
        double distance2 = calculate(cell.getX_right(), cell.getY_left(), x, y);
        double distance3 = calculate(cell.getX_right(), cell.getY_right(), x, y);
        double distance4 = calculate(cell.getX_left(), cell.getY_right(), x, y);

        return distance1 < r || distance2 < r || distance3 < r || distance4 < r;

    }

    public static double calculate(int x, int y, int x1, int y1) {
        return Math.sqrt((x - x1)*(x-x1) + (y - y1)*(y-y1));
    }

    public static double calculateMiniError(Cell cell) {
        ArrayList<BaseStation> arr = cell.arr;
        double z = 1;
        for (BaseStation baseStation : arr) {
            if(z > baseStation.getE()){
                z = baseStation.getE();
            }
        }
        return z;
    }
}