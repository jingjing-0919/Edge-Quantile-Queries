import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class simulator {//数据生产
    public static class data {
        ArrayList<Integer> longitude  ;
        ArrayList<Integer> latitude ;
        ArrayList<Long> time ;
        ArrayList<Integer> value ;

        data(){
            this.latitude = new ArrayList<>();
            this.longitude = new ArrayList<>();
            this.time = new ArrayList<>();
            this.value = new ArrayList<>();
        }

    }
    public static int size = 20000000;

    public static void main(String[] args) throws IOException, InterruptedException {
        data newData = new data();
        Random random = new Random();
        long time = System.currentTimeMillis();
        for (int i = 0;i < size;i++){
//            newData.longitude.add(random.nextInt(100));
//            newData.latitude.add(random.nextInt(100));
            newData.time.add(System.currentTimeMillis()-time);
            newData.value.add(random.nextInt(1000000));
        }
        System.out.println(newData.time.get(size-1) - newData.time.get(0));
        BufferedWriter bw = new BufferedWriter(new FileWriter("test20000000.txt"));
        for (int i = 0;i < size;i++){
            String str = newData.value.get(i) + "," +0+","+0+","+ newData.time.get(i)+"\n";
            bw.write(str);
        }
        bw.close();
    }
}
