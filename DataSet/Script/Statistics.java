/**
 * 这个文本用来统计每一天该地区出现的数据条数，来看那一天的密度最大，作为后面过滤的依据
 * 结果是这样的：
 * 总天数：1839
 * 密度最大小时是 第10小时（index = 10), 504878
 * 密度第二大的小时是 第1小时（index = 1), 472994
 * 密度第三大的小时是 第11小时 (index = 11), 430800
 */
import java.io.*;


public class Statistics {
    static String path = "TestDataSet.txt";
    static int[] hours = new int[24];
    public static void main(String[] args) {
        ReadFile(path);
        getMaxThreeHours();
    }

    public static void ReadFile(String path){
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            String line;
            while((line = in.readLine()) != null){
                String[] sections = line.split(",");
                int hour = Integer.parseInt(sections[6].substring(0,2));
                hours[hour]++;
            }
            in.close();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public static void getMaxThreeHours(){
        for (int i = 0; i < 24 ; i++) {
            System.out.printf("第%d小时, 数据数 %d\n",i, hours[i]);
        }
    }
}
