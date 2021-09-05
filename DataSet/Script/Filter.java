/**
 * 依据前面的统计，得到最后的子数据集，这里按照自己想要的格式重新设计了一下,步行速度设计为 60～100 m / min
 */

import java.io.*;
import java.util.Random;

public class Filter {
    static String path = "TestDataSet.txt";
    static int hour = 10;
    static String targetPath = "SubDataSet.txt";
    public static void main(String[] args) {
        filerDataSet(path);
    }

    public static void filerDataSet(String path){
        try {
            int count = 0;
            Random random = new Random();
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(path))));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(targetPath))));
            String line;
            while((line = in.readLine()) != null){
                StringBuilder stringBuilder = new StringBuilder();
                String[] sections = line.split(",");
                if(Integer.parseInt(sections[6].substring(0,2)) == 10) {
                    stringBuilder.append(sections[0] + "," + sections[1]);
                    stringBuilder.append(",");
                    stringBuilder.append(sections[6]);
                    stringBuilder.append(",");
                    stringBuilder.append(random.nextDouble() * 40 + 60);
                    out.write(stringBuilder + "\n");
                    count++;
                }
            }
            in.close();
            out.close();
            System.out.println("写入的条数：" + count);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
