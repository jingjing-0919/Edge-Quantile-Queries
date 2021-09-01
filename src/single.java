import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class single {
    public static double phi = 0.5;//分位数
    public static int delta_t = 200;//ms
    public static int T = 1000;//ms
    public static int dataSize = 20000000;
    public static double errorBound = 0.004;
    public static String csvFile1 = "test20000000.txt";
    public static void main(String[] args) throws IOException {
        ArrayList<BaseStation> arr = new ArrayList<>();
        String csvFile = "baseStations.txt";
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
                BaseStation baseStation = new BaseStation(e,UTC,id,delay,longitude,latitude,radius);
                arr.add(baseStation);
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
        double [] upper = new double[arr.size()];

        for (int i = 0;i <arr.size();i++){
            upper[i] = computeDataUpperBound( errorBound, arr,i);
        }
        double [] yita  = new double[arr.size()];
        yita = dataReDistribution(arr,errorBound,upper);

        double temp = 0;
        for (int i = 0;i < arr.size();i++){
            System.out.println(yita[i] +" , "+ upper[i]);
        }

        for (int i =0;i < arr.size();i++){
            if (yita[i] == upper[i]){
                Run.run(arr.get(i),(int)(dataSize*yita[i]),T,delta_t,csvFile1,0.5,(int)temp*dataSize,yita[i],0);
            }
            else {
                Run.run(arr.get(i),(int)(dataSize*yita[i]),T,delta_t,csvFile1,0.5,(int)temp*dataSize,yita[i],1);
            }
            temp = temp +yita[i];
        }

    }

    public static double[] dataReDistribution(ArrayList<BaseStation> arr , double errorBound ,double [] upper){
        int [] set = new int[arr.size()];
        for (int i = 0;i < arr.size();i++){
            set[i] = 0;
        }
        double yita = 1;

        double [] yita_final = new double[arr.size()];
        while (yita > 0){
            double z = 0;
            for (int i = 0;i < arr.size();i++){
                if (set[i] == 0){
                    z = z + 1 / arr.get(i).getUTC() ;
                }
            }
            boolean flag = true;
            for (int i = 0; i < arr.size();i++){
                if (set[i] == 0){
                    double yita_i = (double) Math.round(yita * 100/ (arr.get(i).getUTC() * z))/100;
                    if (arr.get(i).getE() > errorBound && yita_i > upper[i]){
                        yita_final[i] = upper[i];
                        set[i] = 1;
                        flag = false;
                        yita = yita - upper[i];
                    }
                    else {
                        yita_final[i] = yita_i;
                    }
                }
            }
            if (flag){
                break;
            }
        }
        return yita_final;
    }


    public static double[] dataReDistributionUTCFirst(ArrayList<BaseStation> arr , double errorBound ,double [] upper){  //多出来的放到UTC最小的去
        int [] set = new int[arr.size()];
        for (int i = 0;i < arr.size();i++){
            set[i] = 0;
        }
        double yita = 1;
        double [] yita_final = new double[arr.size()];
        double z = 0;
        arr.sort(Comparator.comparingDouble(BaseStation::getUTC));//使之有序
        for (int i = 0;i <arr.size();i++){
            upper[i] = computeDataUpperBound( errorBound, arr,i);
        }
        for (int i = 0;i < arr.size();i++){
            if (set[i] == 0){
                z = z + 1 / arr.get(i).getUTC() ;
            }
        }
        for (int i = 0; i < arr.size();i++){
            if (set[i] == 0){
                double yita_i = (double) Math.round( 100/ (arr.get(i).getUTC() * z))/100;
                if (arr.get(i).getE() > errorBound && yita_i > upper[i]){
                    yita_final[i] = upper[i];
                    set[i] = 1;
                    yita = yita - upper[i];
                }
                else {
                    yita_final[i] = yita_i;
                    yita = yita - yita_i;
                }
            }
        }
        for (int i = 0;i < arr.size();i++){
            if (set[i] == 0 && yita > 0){
                if (yita <= upper[i] - yita_final[i]){
                    yita_final[i] = yita_final[i] + yita;
                    yita = 0;
                }
                else {
                    yita = yita - upper[i] + yita_final[i];
                    yita_final[i] = upper[i];
                }
            }
        }
        return yita_final;
    }


    public static double[] dataReDistributionEpsFirst(ArrayList<BaseStation> arr , double errorBound ,double [] upper){  //多出来的放到Eps最小的去
        int [] set = new int[arr.size()];
        for (int i = 0;i < arr.size();i++){
            set[i] = 0;
        }
        double yita = 1;
        double [] yita_final = new double[arr.size()];
        double z = 0;
        arr.sort(Comparator.comparingDouble(BaseStation::getE));//使之有序
        for (int i = 0;i <arr.size();i++){
            upper[i] = computeDataUpperBound( errorBound, arr,i);
        }
        for (int i = 0;i < arr.size();i++){
            if (set[i] == 0){
                z = z + 1 / arr.get(i).getUTC() ;
            }
        }
        for (int i = 0; i < arr.size();i++){
            if (set[i] == 0){
                double yita_i = (double) Math.round( 100/ (arr.get(i).getUTC() * z))/100;
                if (arr.get(i).getE() > errorBound && yita_i > upper[i]){
                    yita_final[i] = upper[i];
                    set[i] = 1;
                    yita = yita - upper[i];
                }
                else {
                    yita_final[i] = yita_i;
                    yita = yita - yita_i;
                }
            }
        }
        for (int i = 0;i < arr.size();i++){
            if (set[i] == 0 && yita > 0){
                if (yita <= upper[i] - yita_final[i]){
                    yita_final[i] = yita_final[i] + yita;
                    yita = 0;
                }
                else {
                    yita = yita - upper[i] + yita_final[i];
                    yita_final[i] = upper[i];
                }
            }
        }
        return yita_final;
    }


    public static double[] dataReDistributionRandom(ArrayList<BaseStation> arr , double errorBound ,double [] upper){  //多出来的随机放
        int [] set = new int[arr.size()];
        for (int i = 0;i < arr.size();i++){
            set[i] = 0;
        }


        double yita = 1;
        double [] yita_final = new double[arr.size()];
        double z = 0;
        for (int i = 0;i < arr.size();i++){
            if (set[i] == 0){
                z = z + 1 / arr.get(i).getUTC() ;
            }
        }
        for (int i = 0; i < arr.size();i++){
            if (set[i] == 0){
                double yita_i = (double) Math.round( 100/ (arr.get(i).getUTC() * z))/100;
                if (arr.get(i).getE() > errorBound && yita_i > upper[i]){
                    yita_final[i] = upper[i];
                    set[i] = 1;
                    yita = yita - upper[i];
                }
                else {
                    yita_final[i] = yita_i;
                    yita = yita - yita_i;
                }
            }
        }
        for (int i = 0;i < arr.size();i++){
            if (set[i] == 0 && yita > 0){
                if (yita <= upper[i] - yita_final[i]){
                    yita_final[i] = yita_final[i] + yita;
                    yita = 0;
                }
                else {
                    yita = yita - upper[i] + yita_final[i];
                    yita_final[i] = upper[i];
                }
            }
        }
        return yita_final;
    }

    public static double computeDataUpperBound(double errorBound,ArrayList<BaseStation> arr, int i){
        double e_min = 1;
        for (int j = 0;j < arr.size();j++){
            if (e_min >= arr.get(j).getE() && j != i){
                e_min = arr.get(j).getE();
            }
        }
        if (arr.get(i).getE() > errorBound){
            return (double)Math.round ((errorBound - e_min)*100/(arr.get(i).getE() - e_min))/100;
        }
        else {
            return 1;
        }
    }
}
