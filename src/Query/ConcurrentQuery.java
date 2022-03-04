package Query;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import Config.config;
import Util.SingleQueryUtil;
import Util.ConcurrentQueryUtil;

public class ConcurrentQuery {

    public static void ConcurrentQueryRun() throws IOException {
        Runtime r =  Runtime.getRuntime();
        r.gc();
        long start_total = r.totalMemory();
        long start_free = r.freeMemory();

        //initial BaseStations
        ArrayList<BaseStation> baseStations = new ArrayList<>();
        SingleQueryUtil.initBaseStation(baseStations);

        //initial Queries
        ArrayList<Query> queries = new ArrayList<>();
        SingleQueryUtil.initQuery(queries);

        //initial Grids

        ArrayList<Cell> cells = new ArrayList<>();
        SingleQueryUtil.initCell(cells);

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
                int c = ConcurrentQueryUtil.check(query, cell) ;
                if (c == 0) {
                    query.covered.add(cell);
                    cell.set.add(query);
                } else if (c == 1) {
                    query.intersecting.add(cell);
                }
            }
        }
        //Set the default dataVolume to 1000000

        for (Cell cell: cells){
            cell.dataVolume = 1000000;
        }


        //calculate the minimal error of one grid

        for (Cell cell : cells) {
            cell.minError = ConcurrentQueryUtil.calculateMiniError(cell);
            cell.error = cell.minError;
        }
        // choose to include or exclude Intersecting Cells
        for (Query query:queries){
            ConcurrentQueryUtil.checkIG(query);
        }
        // check whether one query can satisfy or not by calculate the error of current state

        ConcurrentQueryUtil.CalculateError(queries);

        for(Query query: queries){
            if (query.error > query.getErrorBound()){
                query.success = false;
            }
        }

        //delete failed query

        for (Cell cell : cells){
            ArrayList<Query> temp = cell.set;
            for (int i = 0; i < cell.set.size(); i++){
                if (!temp.get(i).success){
                    cell.set.remove(temp.get(i));
                }
            }
        }
        long start = System.currentTimeMillis();

        //Calculate the delay of each grid under the miniErrorBound
        for (Cell cell : cells){
            ConcurrentQueryUtil.Calculate(cell.minError, cell);
        }
        //calculate the query error and dataSize
        ConcurrentQueryUtil.CalculateError(queries);

        // start to relaxing
        boolean flag = true;
        int count1 = 0;
        int cur_delay = 0;

        while (flag && count1 < 50 * config.dataVolume / 1000000){//relax every bottleneck's ErrorBound, let the delay balance
            ConcurrentQueryUtil.CalculateError(queries);
            Cell bottleneck = ConcurrentQueryUtil.checkBottleneck1(cells);
            int id = bottleneck.getId();
            double relax_rate = 0.2;
            double error_increase = bottleneck.error * relax_rate * bottleneck.dataVolume;
            for (int i =0;i < bottleneck.set.size();i++){
                Query query = bottleneck.set.get(i);
                if (query.getErrorBound() < error_increase/query.dataSize + query.error ){
                    double  cur_relax_rate = (query.getErrorBound() - query.error) * query.dataSize / bottleneck.dataVolume;
                    if (cur_relax_rate < relax_rate){
                        relax_rate = cur_relax_rate;
                    }
                    flag = false;
                }
            }
            count1++;
            ConcurrentQueryUtil.Calculate(bottleneck.error * (1+relax_rate),bottleneck);
            cells.get(id).delay = bottleneck.delay;
            cells.get(id).error = bottleneck.error;
            DecimalFormat df1 = new DecimalFormat("#000.0");
            DecimalFormat df2 = new DecimalFormat("#0.0000");
            cells.get(id).yita = bottleneck.yita;
            if ((int)bottleneck.delay == cur_delay){
                flag = false;
            }
            cur_delay = (int)bottleneck.delay;
            System.out.println("bottleneck_Id : "+bottleneck.getId() +" error: "+ df2.format(bottleneck.error) + " delay: "+ df1.format(bottleneck.delay));


        }
        long end = System.currentTimeMillis();
        BufferedWriter bw1 = new BufferedWriter(new FileWriter("./src/TestResultLog/ConcurrentQueryTestResult.txt", true));
        bw1.write("Time cost of dataRedistribution: " + (end-start) + " ms\n");
        bw1.close();
        long sumMem = 0;

        //run GK algorithm for cells
        for (Cell cell : cells) {
            if (cell.set.size() != 0){
                cell.quantile = ConcurrentQueryUtil.execute(cell, cell.getId(), cell.arr, cell.yita, cell.dataVolume, config.DataFile);
                long end_total = r.totalMemory();
                long end_free = r.freeMemory();
                long mem = ((end_total- end_free) - (start_total - start_free))/1024/1024;
                sumMem += mem;
                System.out.println(mem);
            }
        }
        int sum = 0;
        BufferedWriter bw = new BufferedWriter(new FileWriter("./src/TestResultLog/ConcurrentQueryTestResult.txt", true));
        for (Query query:queries){
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
            if(query.error > query.getErrorBound()){
                bw.write("QueryID: "+ query.id+"   delay: " + delay+ "   error: "+ query.getErrorBound()+"   errorBound: "+query.getErrorBound());
            }else {
                bw.write("QueryID: "+ query.id+"   delay: " + delay+ "   error: "+ query.error+"   errorBound: "+query.getErrorBound());
            }
//------------------------------------------log_end-------------------------------------------------------------------
        }
//------------------------------------------log_start-------------------------------------------------------------------
        bw.write(" \r\n");
        bw.write("\r\n");
        bw.write("QueryAvgDelay: "+ sum/queries.size());
        bw.write("Sum Mem: "+sumMem);
        bw.close();
//------------------------------------------log_end-------------------------------------------------------------------
    }
}
