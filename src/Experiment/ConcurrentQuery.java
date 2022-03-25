package Experiment;

import java.io.*;
import java.net.Socket;
import java.util.*;

import Config.config;
import Model.BaseStation;
import Model.Cell;
import Model.Query;
import Util.SingleQueryUtil;
import Util.ConcurrentQueryUtil;
import Socket.Controller;

public class ConcurrentQuery {

    public static void ConcurrentQueryRun() throws IOException {
        Runtime r = Runtime.getRuntime();
        r.gc();
        long start_total = r.totalMemory();
        long start_free = r.freeMemory();

        //initial BaseStations
        ArrayList<BaseStation> baseStations = new ArrayList<>();
        ConcurrentQueryUtil.initBaseStation(baseStations);

        //initial Queries
        ArrayList<Query> queries = new ArrayList<>();
        ConcurrentQueryUtil.initQuery(queries);

        //initial Grids

        ArrayList<Cell> cells = new ArrayList<>();
        ConcurrentQueryUtil.initCell(cells);

        //find all relative BaseStations for  cells
        for (Cell cell : cells) {
            for (BaseStation baseStation : baseStations) {
                if (ConcurrentQueryUtil.calculate(cell, baseStation)) {
                    cell.arr.add(baseStation);
                }
            }
        }

        //find Covered Cells and Intersecting Cells for queries


        for (Cell cell : cells) {
            for (Query query : queries) {
                int c = ConcurrentQueryUtil.check(query, cell);
                if (c == 0) {
                    query.covered.add(cell);
                    cell.set.add(query);
                } else if (c == 1) {
                    query.intersecting.add(cell);
                }
            }
        }
        //Set the default dataVolume to 1000000

        for (Cell cell : cells) {
            cell.dataVolume = config.dataVolume;
        }


        //calculate the minimal error of one grid

        for (Cell cell : cells) {
            cell.minError = ConcurrentQueryUtil.getMiniError(cell);
            cell.error = cell.minError;
        }
        // choose to include or exclude Intersecting Cells
        for (Query query : queries) {
            ConcurrentQueryUtil.checkIG(query);
        }
        // check whether one query can satisfy or not by calculate the error of current state

        ConcurrentQueryUtil.CalculateError(queries);

        for (Query query : queries) {
            if (query.error > query.getErrorBound()) {
                query.success = false;
            }
        }

        //delete failed query

        for (Cell cell : cells) {
            ArrayList<Query> temp = cell.set;
            for (int i = 0; i < cell.set.size(); i++) {
                if (!temp.get(i).success) {
                    cell.set.remove(temp.get(i));
                }
            }
        }


        //Calculate the delay of each grid under the miniErrorBound
        if (!config.Method.equals("CP")) {
            for (Cell cell : cells) {
                ConcurrentQueryUtil.Calculate(cell.minError, cell);
            }
        } else {
            for (Cell cell : cells) {
                ConcurrentQueryUtil.CP_Calculate(cell);
            }
        }
        for (Cell cell : cells) {
            ConcurrentQueryUtil.Calculate(cell.minError, cell);
        }
        //calculate the query error and dataSize
        ConcurrentQueryUtil.CalculateError(queries);

        if (config.Method.equals("QW")) {
            int sum = 0;
            long sumMem = 0;
            for (Query query : queries) {
                query.dataSize = 0;
                for (int i = 0; i < query.covered.size(); i++) {
                    query.dataSize += query.covered.get(i).dataVolume;
                }
                int[] delay = SingleRunner.run(query.arr, query);
                if (sum < delay[0]) {
                    sum = delay[0];
                }
                sumMem = sumMem + delay[1];
            }
            BufferedWriter bw = new BufferedWriter(new FileWriter("./TestResultLog/ConcurrentQueryTestResult.txt", true));
            bw.write("\r\n");
            bw.write("TotalDelay = " + sum);
            bw.write("TotalMemory = " + sumMem);
            bw.write("\r\n");
            bw.close();
        } else {
            // start to relaxing
            long start = System.nanoTime();
            if (!config.Method.equals("CB/R") && !config.Method.equals("CP")) {
                boolean flag = true;
                int count1 = 0;
                int cur_delay = 0;
                double relax_rate = config.RelaxRate;
                while (flag && count1 < 10 + 100 * config.dataVolume / 1000000) {//relax every bottleneck's ErrorBound, let the delay balance
                    ConcurrentQueryUtil.CalculateError(queries);
                    Cell bottleneck;
                    if (config.RelaxMethod.equals("minMax")) {
                        bottleneck = ConcurrentQueryUtil.checkBottleneck_minMax(cells);
                    } else {
                        bottleneck = ConcurrentQueryUtil.checkBottleneck_minAvg(cells, queries);
                    }
                    int id = bottleneck.getId();
                    double error_increase = bottleneck.error * relax_rate * bottleneck.dataVolume;
                    for (int i = 0; i < bottleneck.set.size(); i++) {
                        Query query = bottleneck.set.get(i);
                        if (query.getErrorBound() < error_increase / query.dataSize + query.error) {
                            double cur_relax_rate = (query.getErrorBound() - query.error) * query.dataSize / bottleneck.dataVolume;
                            if (cur_relax_rate < relax_rate) {
                                relax_rate = cur_relax_rate;
                            }
                            flag = false;
                        }
                    }
                    count1++;
                    ConcurrentQueryUtil.Calculate(bottleneck.error * (1 + relax_rate), bottleneck);
                    cells.get(id).delay = bottleneck.delay;
                    cells.get(id).error = bottleneck.error;
                    //DecimalFormat df1 = new DecimalFormat("#000.0");
                    //DecimalFormat df2 = new DecimalFormat("#0.0000");
                    cells.get(id).eta = bottleneck.eta;
                    if ((int) bottleneck.delay == cur_delay) {
                        flag = false;
                    }
                    cur_delay = (int) bottleneck.delay;
                    //System.out.println("bottleneck_Id : "+bottleneck.getId() +" error: "+ df2.format(bottleneck.error) + " delay: "+ df1.format(bottleneck.delay));
                }
            }
            long end = System.nanoTime();
            //System.out.println("start:"+start);
            //System.out.println("end:"+end);
            BufferedWriter bw1 = new BufferedWriter(new FileWriter("./TestResultLog/ConcurrentQueryTestResult.txt", true));
            bw1.write("Time cost of dataRedistribution: " + (end - start) / 1000000 + " ms\n");
            bw1.close();
            long sumMem = 0;

            if (!config.useSocket) {
                //run GK algorithm for cells
                for (Cell cell : cells) {
                    if (cell.set.size() != 0) {
                        cell.quantile = ConcurrentQueryUtil.execute(cell, cell.getId(), cell.arr, cell.eta, cell.dataVolume, config.DataFile);
                        long end_total = r.totalMemory();
                        long end_free = r.freeMemory();
                        long mem = ((end_total - end_free) - (start_total - start_free)) / 1024 / 1024;
                        sumMem += mem;
                        //System.out.println(mem);
                    }
                }
                int sum = 0;
                BufferedWriter bw = new BufferedWriter(new FileWriter("./TestResultLog/ConcurrentQueryTestResult.txt", true));
                for (Query query : queries) {
                    ArrayList<Cell> grids1 = query.covered;
                    int delay = 0;
                    for (Cell cell : grids1) {
                        if (delay < cell.delay) {
                            delay = (int) cell.delay;
                        }
                    }
                    sum = sum + delay;
//------------------------------------------log_start-------------------------------------------------------------------
                    bw.write(" \r\n");
                    bw.write("\r\n");
                    if (query.error > query.getErrorBound()) {
                        bw.write("QueryID: " + query.id + "   delay: " + delay + "   error: " + query.getErrorBound() + "   errorBound: " + query.getErrorBound());
                    } else {
                        bw.write("QueryID: " + query.id + "   delay: " + delay + "   error: " + query.error + "   errorBound: " + query.getErrorBound());
                    }
//------------------------------------------log_end-------------------------------------------------------------------
                }
//------------------------------------------log_start-------------------------------------------------------------------
                bw.write(" \r\n");
                bw.write("\r\n");
                bw.write("QueryAvgDelay: " + sum / queries.size());
                bw.write("Sum Mem: " + sumMem);
                bw.close();
//------------------------------------------log_end-------------------------------------------------------------------
            } else {
                Controller.sendMessage(cells);
            }
        }
    }
}
