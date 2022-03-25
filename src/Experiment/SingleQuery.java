package Experiment;

import java.io.*;
import java.util.*;

import Model.BaseStation;
import Model.Cell;
import Model.Query;
import Util.SingleQueryUtil;
import Config.config;

public class SingleQuery {

    public static void SingleQueryRun() throws IOException {

        //initial BaseStations
        ArrayList<BaseStation> baseStations = new ArrayList<>();
        SingleQueryUtil.initBaseStation(baseStations);

        //initial Queries
        ArrayList<Query> queries = new ArrayList<>();
        SingleQueryUtil.initQuery(queries);

        //initial Grids
        ArrayList<Cell> cells = new ArrayList<>();
        SingleQueryUtil.initCell(cells);

        //find all relative BaseStations for  grids
        for (Query query : queries) {
            for (BaseStation baseStation : baseStations) {
                if (SingleQueryUtil.calculate(query, baseStation)) {
                    query.arr.add(baseStation);
                }
            }
        }

        //find Covered Cells and Intersecting Cells for queries
        for (Cell cell : cells) {
            for (Query query : queries) {
                int c = SingleQueryUtil.check(query, cell);
                if (c == 0) {
                    query.covered.add(cell);
                    cell.set.add(query);
                } else if (c == 1) {
                    query.intersecting.add(cell);
                }
            }
        }
        //Set the default dataVolume


        for (Cell cell : cells) {
            cell.dataVolume = config.dataVolume;
        }

        for (Cell cell : cells) {
            cell.minError = SingleQueryUtil.getMiniError(cell);
            cell.error = cell.minError;
        }
        // choose to include or exclude Intersecting Cells
        for (Query query : queries) {
            SingleQueryUtil.checkIG(query);
        }
        // check whether one query can satisfy or not by calculate the error of current state

        SingleQueryUtil.CalculateError(queries);
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

        BufferedWriter bw = new BufferedWriter(new FileWriter("./TestResultLog/SingleQueryTestResult.txt", true));
        bw.write("\r\n");
        bw.write("TotalDelay = " + sum);
        bw.write("TotalMemory = " + sumMem);
        bw.write("\r\n");
        bw.close();
    }
}
