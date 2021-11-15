import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class MultipleQuery2 {

    public static String Query_txt;
    public static String BaseStations_txt;
    public static int Grid_length = 1000;//square default
    public static int x_length = 5000;
    public static int y_length = 5000;
    public static int [][] result;
    public static boolean fail = false;

    public static void main(String[] args) throws IOException {
        Runtime r =  Runtime.getRuntime();
        r.gc();
        long start_total = r.totalMemory();
        long start_free = r.freeMemory();

        //初始化BaseStations
        ArrayList<BaseStation> baseStations = new ArrayList<>();
        String csvFile = "data/baseStations16.txt";
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


        //initial Query
        ArrayList<Query> queries = new ArrayList<>();
        csvFile = "data/Query50_txt";
        br = null;
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
                Query query = new Query(T, delta_T, x_left, x_right, y_left, y_right, errorBound,id);
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
        //initial Grids
        int numberOfGrid_x = x_length / Grid_length;
        int numberOfGrid_y = y_length / Grid_length;
        ArrayList<Grid> grids = new ArrayList<>();
        result = new int[numberOfGrid_x*numberOfGrid_y][numberOfGrid_y*numberOfGrid_y];
        int count = 0;
        for (int i = 0; i < numberOfGrid_y; i++) {
            for (int j = 0; j < numberOfGrid_x; j++) {
                Grid grid = new Grid(j * Grid_length, (j + 1) * Grid_length, i * Grid_length, (i + 1) * Grid_length, count);
                grids.add(grid);
                count++;
            }
        }

        //find all relative BaseStations for  grids
        for (Grid grid : grids) {
            for (BaseStation baseStation : baseStations) {
                if (calculate(grid, baseStation)) {
                    grid.arr.add(baseStation);
                }
            }
        }

        //find Covered Cells and Intersecting Cells for queries

        for (Grid grid : grids) {
            for (Query query : queries) {
                int c = check(query, grid) ;
                if (c == 0) {
                    query.covered.add(grid);
                    grid.set.add(query);
                } else if (c == 1) {
                    query.intersecting.add(grid);
                }
            }
        }
        //Set the default dataVolume to 1000000

        for (Grid grid:grids){
            grid.dataVolume = 1000000;
        }
        

        //calculate the minimal error of one grid

        for (Grid grid : grids) {
            grid.minError = calculateMiniError(grid);
            grid.error = grid.minError;
        }
        // choose to include or exclude Intersecting Cells
        for (Query query:queries){
            checkIG(query);
        }
        // check whether one query can satisfy or not by calculate the error of current state

        CalculateError(queries);

        for(Query query: queries){
          if (query.error > query.getErrorBound()){
              query.success = false;
          }
        }

        //delete failed query

        for (Grid grid:grids){
            ArrayList<Query> temp = grid.set;
            for (int i = 0;i < grid.set.size();i++){
                if (!temp.get(i).success){
                    grid.set.remove(temp.get(i));
                }
            }
        }
        long start = System.currentTimeMillis();

        //Calculate the delay of each grid under the miniErrorBound
        for (Grid grid:grids){
             Calculate(grid.minError,grid);
        }
        //calculate the query error and dataSize
        CalculateError(queries);

        // start to relaxing
        boolean flag = true;
        int count1 = 0;

        while (flag && count1 < 50){//relax every bottleneck's ErrorBound, let the delay balance
            CalculateError(queries);
            Grid bottleneck = checkBottleneck1(grids);
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

            Calculate(bottleneck.error * (1+relax_rate),bottleneck);


            grids.get(id).delay = bottleneck.delay;
            grids.get(id).error = bottleneck.error;
            DecimalFormat df1 = new DecimalFormat("#000.0");
            DecimalFormat df2 = new DecimalFormat("#0.0000");
            grids.get(id).yita = bottleneck.yita;
            System.out.println("bottleneck_Id : "+bottleneck.getId() +" error: "+ df2.format(bottleneck.error) + " delay: "+ df1.format(bottleneck.delay));


        }
        long end = System.currentTimeMillis();
        BufferedWriter bw1 = new BufferedWriter(new FileWriter("MultipleQueryTestResult1.txt", true));
        bw1.write("Time cost of dataRedistribution: " + (end-start) + " ms\n");
        bw1.close();
        long sumMem = 0;

        //对Grids运行GK
        for (Grid grid : grids) {
            if (grid.set.size() != 0){
                grid.quantile = execute(grid,grid.getId(),grid.arr, grid.yita, grid.dataVolume, "test2000000.txt");
                long end_total = r.totalMemory();
                long end_free = r.freeMemory();
                long mem = ((end_total- end_free) - (start_total - start_free))/1024/1024;
                sumMem += mem;
                System.out.println(mem);
            }
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter("MultipleQueryTestResult1.txt", true));
        int sum = 0;
        for (Query query:queries){
            ArrayList<Grid> grids1 = query.covered;
            int delay = 0;
            for (Grid grid : grids1) {
                if (delay < grid.delay) {
                    delay = (int) grid.delay;
                }
            }
            sum = sum + delay;
            bw.write(" \r\n");
            bw.write("\r\n");
            if(query.error > query.getErrorBound()){
                bw.write("QueryID: "+ query.id+"   delay: " + delay+ "   error: "+ query.getErrorBound()+"   errorBound: "+query.getErrorBound());
            }else {
                bw.write("QueryID: "+ query.id+"   delay: " + delay+ "   error: "+ query.error+"   errorBound: "+query.getErrorBound());
            }

        }
        bw.write(" \r\n");
        bw.write("\r\n");
        bw.write("QueryAvgDelay: "+ sum/queries.size());
        bw.write("Sum Mem: "+sumMem);

        bw.close();







    }

    public static ArrayList<Integer> execute(Grid grid,int id,ArrayList<BaseStation> arr, HashMap<BaseStation,Double> yita,  int dataSize, String csvFile1) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter("MultipleQueryTestResult1.txt", true));
        bw.write(" \r\n");
        bw.write("\r\n");
        bw.write("GridID: "+ id);
        bw.close();
        double temp = 0;
        ArrayList<Integer> quantile = new ArrayList<>();
        int delayMax = 0;
        for (BaseStation baseStation : arr) {
            if (yita.get(baseStation) != 0.0){
                ArrayList<Integer> arrayList = RunWithReturn.run(grid,baseStation, (int) (dataSize * yita.get(baseStation)), csvFile1, 0.5, (int) temp * dataSize);
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
        grid.delay = delayMax;

        return quantile;
    }

    public static Grid checkBottleneck1(ArrayList<Grid> grids){
        Grid bottleneck = grids.get(0);
        for (Grid grid : grids) {
            if (bottleneck.delay < grid.delay && grid.set.size() != 0) {
                bottleneck = grid;
            }
        }
        return bottleneck;
    }




    public static Grid checkBottleneck(ArrayList<Grid> grids,ArrayList<Query>queries){
        int []cnt = new int[grids.size()];
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
        return grids.get(index);
    }



    public static void Calculate(double errorBound,Grid grid){//distribute the data under the given errorBound
        grid.yita.clear();
        double [] upper = new double[grid.arr.size()];
        ArrayList<BaseStation> arr = grid.arr;
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
            grid.yita.put(arr.get(i),yita_final[i]);
            grid.delay = yita_final[i] * grid.dataVolume * grid.arr.get(i).getUTC();
            grid.error = errorBound;
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

    public static boolean distribute(double delay,Grid grid){
        grid.arr.sort(Comparator.comparingDouble(BaseStation::getE));
        HashMap<BaseStation,Double> yita = new HashMap<>();
        double data = grid.dataVolume;
        int index = 0;
        double temp_e = 0;
        while (data > 0 && index < grid.arr.size()){
            BaseStation cur = grid.arr.get(index);
            double cur_data = delay / cur.getUTC();
            if (cur_data < data){
                data = data - cur_data;
                yita.put(cur,cur_data/grid.dataVolume);
                index++;
                temp_e = temp_e + cur.getE() * cur_data;
            }
            else {
                yita.put(cur,data/grid.dataVolume);
                temp_e = temp_e + cur.getE() * data;
                data = 0;
            }
        }
        for (int i = 0;i < grid.arr.size();i++){
            if (!yita.containsKey(grid.arr.get(i))){
                yita.put(grid.arr.get(i),0.0);
            }
        }

        double error = grid.error * grid.dataVolume;
        double error_increase = temp_e - error;
        boolean flag = true;
        for (int i = 0; i < grid.set.size();i++){
            Query query = grid.set.get(i);
            if (query.error + error_increase/query.dataSize > query.getErrorBound()){
                flag = false;
                break;
            }
        }
        if (flag && data == 0){
            grid.yita = yita;
            grid.delay = delay;
            grid.error = temp_e/grid.dataVolume;
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










    public static int check(Query query, Grid grid) {
        int query_XLeft = query.getX_left();
        int query_XRight = query.getX_right();
        int query_YLeft = query.getY_left();
        int query_YRight = query.getY_right();

        boolean x = grid.getX_right() <= query_XLeft || grid.getX_left() >= query_XRight;
        boolean y = grid.getY_right() <= query_YLeft || grid.getY_left() >= query_YRight;

        boolean x1 = grid.getX_left() >= query_XLeft && grid.getX_right() <= query_XRight;
        boolean y1 = grid.getY_left() >= query_YLeft && grid.getY_right() <= query_YRight;

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
            Grid IG = query.intersecting.get(i);
            calculateIG(query, IG, N);
        }
    }

    public static void calculateIG(Query query, Grid grid, int N) {
        ArrayList<Integer> arrX = new ArrayList<>();
        ArrayList<Integer> arrY = new ArrayList<>();
        arrX.add(query.getX_left());
        arrX.add(query.getX_right());
        arrX.add(grid.getX_left());
        arrX.add(grid.getX_right());
        Collections.sort(arrX);
        arrY.add(query.getY_left());
        arrY.add(query.getY_right());
        arrY.add(grid.getY_right());
        arrY.add(grid.getY_left());
        Collections.sort(arrY);

        int intersectArea = (arrX.get(2) - arrX.get(1)) * (arrY.get(2) - arrY.get(1));
        int irrelevantArea = Grid_length * Grid_length - intersectArea;
        intersectArea = (intersectArea * 100 / (Grid_length * Grid_length))*N/100;
        irrelevantArea = (irrelevantArea*100 / (Grid_length * Grid_length))*N/100;

        double ErrorIG = irrelevantArea + grid.error * intersectArea ;
        double ErrorEG = intersectArea ;


        if (ErrorEG >= ErrorIG) {
            query.covered.add(grid);
            query.intersecting.remove(grid);
            grid.set.add(query);
        }
    }

    public static double calculateIGError(Query query, Grid grid){
        ArrayList<Integer> arrX = new ArrayList<>();
        ArrayList<Integer> arrY = new ArrayList<>();
        arrX.add(query.getX_left());
        arrX.add(query.getX_right());
        arrX.add(grid.getX_left());
        arrX.add(grid.getX_right());
        Collections.sort(arrX);
        arrY.add(query.getY_left());
        arrY.add(query.getY_right());
        arrY.add(grid.getY_right());
        arrY.add(grid.getY_left());
        Collections.sort(arrY);

        int intersectArea = (arrX.get(2) - arrX.get(1)) * (arrY.get(2) - arrY.get(1));
        int irrelevantArea = Grid_length * Grid_length - intersectArea;
        intersectArea = (intersectArea *100/ (Grid_length * Grid_length))* grid.dataVolume/100;
        irrelevantArea = (irrelevantArea*100 / (Grid_length * Grid_length))*grid.dataVolume/100;

        return (irrelevantArea + grid.error * intersectArea );
    }


    public static boolean calculate(Query query,BaseStation baseStation){
        boolean x1 = baseStation.getLongitude() > query.getX_left() && baseStation.getLongitude() < query.getX_right();
        boolean y1 = baseStation.getLatitude() > query.getY_left() && baseStation.getLatitude() < query.getY_right();
        return x1 && y1;
    }

    public static int calculateEGError(Query query, Grid grid) {

        ArrayList<Integer> arrX = new ArrayList<>();
        ArrayList<Integer> arrY = new ArrayList<>();
        arrX.add(query.getX_left());
        arrX.add(query.getX_right());
        arrX.add(grid.getX_left());
        arrX.add(grid.getX_right());
        Collections.sort(arrX);
        arrY.add(query.getY_left());
        arrY.add(query.getY_right());
        arrY.add(grid.getY_right());
        arrY.add(grid.getY_left());
        Collections.sort(arrY);

        int intersectArea = (arrX.get(2) - arrX.get(1)) * (arrY.get(2) - arrY.get(1));
        intersectArea = (intersectArea *100/ (Grid_length * Grid_length))* grid.dataVolume/100;

        return  intersectArea ;

    }


    public static boolean calculate(Grid grid, BaseStation baseStation) {
        int x = baseStation.getLongitude();
        int y = baseStation.getLatitude();
        int r = baseStation.getRadius();

        double distance1 = calculate(grid.getX_left(), grid.getY_left(), x, y);
        double distance2 = calculate(grid.getX_right(), grid.getY_left(), x, y);
        double distance3 = calculate(grid.getX_right(), grid.getY_right(), x, y);
        double distance4 = calculate(grid.getX_left(), grid.getY_right(), x, y);

        return distance1 < r || distance2 < r || distance3 < r || distance4 < r;

    }

    public static double calculate(int x, int y, int x1, int y1) {
        return Math.sqrt((x - x1)*(x-x1) + (y - y1)*(y-y1));
    }

    public static double calculateMiniError(Grid grid) {
        ArrayList<BaseStation> arr = grid.arr;
        double z = 1;
        for (BaseStation baseStation : arr) {
            if(z > baseStation.getE()){
                z = baseStation.getE();
            }
        }
        return z;
    }


}
