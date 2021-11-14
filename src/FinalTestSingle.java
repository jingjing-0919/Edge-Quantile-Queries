import java.io.*;
import java.util.*;

public class FinalTestSingle {//todo:1.  50 - 100w/s 数据 1km^km  2. delta_t = 1s  3.  delay 需要有累计  4. 按error relax


    public static int Grid_length = 1000;//默认正方形
    public static int x_length = 5000;//应该为网格边长整数倍
    public static int y_length = 5000;//应该为网格边长整数倍
    public static int [][] result;


    public static void main(String[] args) throws IOException {

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


        //初始化Query
        ArrayList<Query> queries = new ArrayList<>();
        csvFile = "data/Query60_txt";
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
                double errorBound = Double.parseDouble(country[7]) ;
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
        //初始化所有Grids
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

        //对所有相关的Grid找到相应的BaseStations
        for (Query query:queries) {
            for (BaseStation baseStation : baseStations) {
                if (calculate(query, baseStation)) {
                    query.arr.add(baseStation);
                }
            }
        }

        //对每个query 维护 IG 和 EG

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
        //对每个Grid进行采样，得到数据量


        for (Grid grid:grids){
            grid.dataVolume = 400000;
        }

        for (Grid grid : grids) {
            grid.minError = calculateMiniError(grid);
            grid.error = grid.minError;
        }
        // 对IG进行EXCLUDE和INCLUDE判断
        for (Query query:queries){
            checkIG(query);//todo： 重写一下calculateError
        }
        // 对每个Query去检测它的ErrorBound是否在每个grid的error设为最小时被满足

        CalculateError(queries);
        int sum = 0;
        long sumMem = 0;



        for (Query query:queries){
            query.dataSize = 0;
            for (int i = 0;i < query.covered.size();i++){
                query.dataSize += query.covered.get(i).dataVolume;
            }
            int []delay = SingleRun.run(query.arr,query);
            if (sum < delay[0]){
                sum = delay[0];
            }
            sumMem = sumMem + delay[1];
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter("FinalSingleTestResult.txt", true));
        bw.write("\r\n");
        bw.write("TotalDelay = "+sum);
        bw.write("TotalMemory = "+sumMem);
        bw.write("\r\n");
        bw.close();


    }




    public static void CalculateError(ArrayList<Query>checkList){//在每次进行distribute之后，计算当前Grid涉及的queries的错误率,检查是否超过errorBound
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
            calculateIG(query, IG, y, i, N);
        }
    }

    public static void calculateIG(Query query, Grid grid, double y, int index, int N) {
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
        intersectArea = (intersectArea *100/ (Grid_length * Grid_length))*N/100;
        irrelevantArea = (irrelevantArea*100 / (Grid_length * Grid_length))*N/100;

        double ErrorIG = (irrelevantArea + grid.error * intersectArea );
        double ErrorEG = (intersectArea ) ;


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
        intersectArea = (intersectArea *10000/ (Grid_length * Grid_length))* grid.dataVolume/10000;
        irrelevantArea = (irrelevantArea*10000 / (Grid_length * Grid_length))*grid.dataVolume/10000;

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
        intersectArea = (intersectArea *10000/ (Grid_length * Grid_length))* grid.dataVolume/10000;

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
