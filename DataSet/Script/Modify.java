/**
 * 这个部分将原来的整个数据集逐条存进一个文件中，基本过滤一下我的数据，基于这两个条件：
 * 文件格式上的一致（头尾有一些注解去掉），每条数据的经度（116.3 ~ 116.35) 和 纬度（39.8 ~ 40.1)这个范围内
 */
import java.io.*;


public class Modify {
    static final double lon_down = 116.3;
    static final double lon_up = 116.35;
    static final double lat_down = 39.97;
    static final double lat_up = 40.02;

    /**
     * 上面地区就是长，宽大约各5km 的区域
     * 这种情况下数据的总条数为：6128043条，原本在北京五环地区有17732698条数据，占34.6%, 面积占总面积2.3%。可以看成热点区域
     */
    public static void main(String[] args) {
        readfile("/Users/john/Desktop/Geolife Trajectories 1.3/Data");
    }

    public static boolean readfile(String filepath){
        try{
            BufferedReader in;
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("TestDataSet.txt"))));
            File file = new File(filepath);
            if(file.isDirectory()){
                String[] filelist = file.list();
                long count = 0;
                long test = 0;
                for(int i = 0; i < filelist.length; i++){
                    File readfile = new File(filepath + "/" + filelist[i] + "/Trajectory");
                    if(readfile.isDirectory()){
                        String[] dataList = readfile.list();
                        for (int j = 0; j < dataList.length; j++) {
                            File readData = new File(readfile.getAbsolutePath() + "/" + dataList[j]);
                            if(readData.isFile()){
                                test++;
                                in = new BufferedReader(new InputStreamReader(new FileInputStream(readData)));
                                String line;
                                for (int k = 0; k < 6; k++) {
                                    in.readLine();
                                }
                                while((line = in.readLine()) != null){
                                    String[] position = line.split(",");// position[0] 表示纬度，position[1] 表示经度
                                    double lat = Double.parseDouble(position[0]);
                                    double lon = Double.parseDouble(position[1]);
                                    if(lat_down<= lat && lat <= lat_up && lon_down <= lon && lon <= lon_up) {
                                        out.write(line + '\n');
                                        count++;
                                    }
                                }
                                in.close();
                            }
                        }
                    }
                }
                out.close();
                System.out.println("文件数"+ test);
                System.out.println("数据条数: "+ count);
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
        return true;
    }


}
