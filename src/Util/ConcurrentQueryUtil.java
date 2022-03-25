package Util;

import Config.config;
import Model.BaseStation;
import Model.Cell;
import Model.Query;
import Experiment.ConcurrentRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static Util.SingleQueryUtil.x_length;
import static Util.SingleQueryUtil.y_length;

public class ConcurrentQueryUtil {
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
                double errorBound = Double.parseDouble(country[7]);
                Query query = new Query(T, delta_T, x_left, x_right, y_left, y_right, errorBound, id);
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
    public static ArrayList<Integer> execute(Cell cell, int id, ArrayList<BaseStation> arr, HashMap<BaseStation, Double> eta, int dataSize, String csvFile1) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("./TestResultLog/ConcurrentQueryTestResult.txt", true));
        bw.write(" \r\n");
        bw.write("\r\n");
        bw.write("GridID: " + id);
        bw.close();
        double temp = 0;
        ArrayList<Integer> quantile = new ArrayList<>();
        int delayMax = 0;
        for (BaseStation baseStation : arr) {
            if (eta.get(baseStation) != 0.0) {
                ArrayList<Integer> arrayList = ConcurrentRunner.run(baseStation, (int) (dataSize * eta.get(baseStation)), csvFile1, 0.5, (int) temp * dataSize);
                int index = arrayList.size() - 1;
                int delay = arrayList.get(index);
                arrayList.remove(index);
                quantile.addAll(arrayList);
                if (delay > delayMax) {
                    delayMax = delay;
                }
            }
            temp = temp + eta.get(baseStation);
        }
        double errorRate = 0;
        for (BaseStation baseStation : arr) {
            errorRate = errorRate + eta.get(baseStation) * baseStation.getE();
        }
        cell.delay = delayMax;

        return quantile;
    }

    public static Cell checkBottleneck_minMax(ArrayList<Cell> cells) {
        Cell bottleneck = cells.get(0);
        for (Cell cell : cells) {
            if (bottleneck.delay < cell.delay && cell.set.size() != 0) {
                bottleneck = cell;
            }
        }
        return bottleneck;
    }

    public static Cell checkBottleneck_minAvg(ArrayList<Cell> cells, ArrayList<Query> queries) {
        int[] cnt = new int[cells.size()];
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
        for (int l = 0; l < cnt.length; l++) {
            if (max < cnt[l]) {
                max = cnt[l];
                index = l;
            }
        }
        return cells.get(index);
    }


    public static void Calculate(double errorBound, Cell cell) {//distribute the data under the given errorBound
        cell.eta.clear();
        double[] upper = new double[cell.arr.size()];
        ArrayList<BaseStation> arr = cell.arr;
        int[] set = new int[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            set[i] = 0;
        }
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
        for (int i = 0; i < eta_final.length; i++) {
            cell.eta.put(arr.get(i), eta_final[i]);
            cell.delay = eta_final[i] * cell.dataVolume * cell.arr.get(i).getUTC();
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


    public static void CalculateError(ArrayList<Query> checkList) {
        for (Query cur : checkList) {
            int temp_N = 0;
            double temp_e = 0;
            if (config.Method.equals("CB-E")) {
                for (int j = 0; j < cur.intersecting.size(); j++) {
                    temp_e = temp_e + 0.1 * calculateEGError(cur, cur.intersecting.get(j));
                    temp_N = temp_N + cur.intersecting.get(j).dataVolume;
                }
            }
            for (int j = 0; j < cur.covered.size(); j++) {
                temp_e = temp_e + 0.1 * calculateIGError(cur, cur.covered.get(j));
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
        if (config.Method.equals("CB") || config.Method.equals("CB/R") || config.Method.equals("CP")) {
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
        } else if (config.Method.equals("CB-I")) {
            query.covered.addAll(query.intersecting);
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
        intersectArea = (intersectArea * 100 / (cell_length * cell_length)) * N / 100;
        irrelevantArea = (irrelevantArea * 100 / (cell_length * cell_length)) * N / 100;

        double ErrorIG = irrelevantArea + cell.error * intersectArea;
        double ErrorEG = intersectArea;


        if (ErrorEG >= ErrorIG) {
            query.covered.add(cell);
            query.intersecting.remove(cell);
            cell.set.add(query);
        }
    }

    public static double calculateIGError(Query query, Cell cell) {
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
        intersectArea = (intersectArea * 100 / (cell_length * cell_length)) * cell.dataVolume / 100;
        irrelevantArea = (irrelevantArea * 100 / (cell_length * cell_length)) * cell.dataVolume / 100;

        return (irrelevantArea + cell.error * intersectArea);
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
        intersectArea = (intersectArea * 100 / (cell_length * cell_length)) * cell.dataVolume / 100;

        return intersectArea;

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
        return Math.sqrt((x - x1) * (x - x1) + (y - y1) * (y - y1));
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

    public static void CP_Calculate(Cell cell) {
        cell.eta.clear();
        double min_error = 1;
        int index = 0;
        for (int i = 0; i < cell.arr.size(); i++) {
            if (min_error < cell.arr.get(i).getE()) {
                min_error = cell.arr.get(i).getE();
                index = i;
            }
        }
        cell.eta.put(cell.arr.get(index), 1.0);
        for (int i = 0; i < cell.arr.size(); i++) {
            if (index != i) {
                cell.eta.put(cell.arr.get(i), 0.0);
            }
        }

    }
}
