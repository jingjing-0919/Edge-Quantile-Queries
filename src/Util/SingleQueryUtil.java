package Util;

import Model.BaseStation;
import Model.Query;
import Model.Cell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import Config.config;


public class SingleQueryUtil {
    public static int x_length = 5000;
    public static int y_length = 5000;

    public static void initBaseStation(ArrayList<BaseStation> baseStations) {
        String csvFile = config.BaseStationFile;
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] country = line.split(cvsSplitBy);
                int id = Integer.parseInt(country[0]);
                double e = Double.parseDouble(country[1]);
                double UTC = Double.parseDouble(country[2]);
                int delay = Integer.parseInt(country[3]);
                int longitude = Integer.parseInt(country[4]);
                int latitude = Integer.parseInt(country[5]);
                int radius = Integer.parseInt(country[6]);
                BaseStation baseStation = new BaseStation(e, UTC, id, delay, longitude, latitude, radius);
                if (config.useSocket) {
                    baseStation.setIpAddress(country[7]);
                }
                baseStations.add(baseStation);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void initQuery(ArrayList<Query> queries) {
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        String csvFile = config.QueryFile;
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) !=

                    null) {
                // use comma as separator
                String[] country = line.split(cvsSplitBy);
                int id = Integer.parseInt(country[0]);
                int T = Integer.parseInt(country[1]);
                int delta_T = Integer.parseInt(country[2]);
                int x_left = Integer.parseInt(country[3]);
                int x_right = Integer.parseInt(country[4]);
                int y_left = Integer.parseInt(country[5]);
                int y_right = Integer.parseInt(country[6]);
                Query query = new Query(T, delta_T, x_left, x_right, y_left, y_right, 0.01, id);
                queries.add(query);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {

                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void initCell(ArrayList<Cell> cells) {
        int Cell_length = config.Cell_length;
        int numberOfGrid_x = x_length / Cell_length;
        int numberOfGrid_y = y_length / Cell_length;
        int count = 0;
        for (int i = 0; i < numberOfGrid_y; i++) {
            for (int j = 0; j < numberOfGrid_x; j++) {
                Cell cell = new Cell(j * Cell_length, (j + 1) * Cell_length, i * Cell_length, (i + 1) * Cell_length, count);
                cells.add(cell);
                count++;
            }
        }
    }

    public static void CalculateError(ArrayList<Query> checkList) {
        for (Query cur : checkList) {
            int temp_N = 0;
            double temp_e = 0;
            for (int j = 0; j < cur.covered.size(); j++) {
                temp_e = temp_e + cur.covered.get(j).error * cur.covered.get(j).dataVolume;
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
            calculateIG(query, IG, y, i, N);
        }
    }

    public static double[] getDoubles(ArrayList<BaseStation> arr, double errorBound, double[] upper, Comparator<BaseStation> baseStationComparator) {
        int[] set = new int[arr.size()];
        double eta = 1;
        double[] eta_final = new double[arr.size()];
        double z = 0;
        arr.sort(baseStationComparator);
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
        relax_eta(arr,eta_final);
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

    public static void calculateIG(Query query, Cell cell, double y, int index, int N) {
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
        int Cell_length = config.Cell_length;


        int intersectArea = (arrX.get(2) - arrX.get(1)) * (arrY.get(2) - arrY.get(1));
        int irrelevantArea = Cell_length * Cell_length - intersectArea;
        intersectArea = (intersectArea * 100 / (Cell_length * Cell_length)) * N / 100;
        irrelevantArea = (irrelevantArea * 100 / (Cell_length * Cell_length)) * N / 100;

        double ErrorIG = (irrelevantArea + cell.error * intersectArea);
        double ErrorEG = (intersectArea);


        if (ErrorEG >= ErrorIG) {
            query.covered.add(cell);
            query.intersecting.remove(cell);
            cell.set.add(query);
        }
    }

    public static void relax_eta(ArrayList<BaseStation> arr,double[]eta){
        int index = 0;
        double value = 1;
        for (int i = 0;i < eta.length;i++){
            if (arr.get(i).getE() < value){
                value = arr.get(i).getE();
                index = i;
            }
        }
        for (int i = 0;i < eta.length;i++){
            if (i != index && eta[i]>config.RelaxRate/10){
                eta[i] -= config.RelaxRate/10;
                eta[index] += config.RelaxRate/10;
            }
        }
    }



    public static boolean calculate(Query query, BaseStation baseStation) {
        boolean x1 = baseStation.getLongitude() > query.getX_left() && baseStation.getLongitude() < query.getX_right();
        boolean y1 = baseStation.getLatitude() > query.getY_left() && baseStation.getLatitude() < query.getY_right();
        return x1 && y1;
    }


    public static double getMiniError(Cell cell) {
        ArrayList<BaseStation> arr = cell.arr;
        double z = 1;
        for (BaseStation baseStation : arr) {
            if (z > baseStation.getE()) {
                z = baseStation.getE();
            }
        }
        return z;
    }
}
