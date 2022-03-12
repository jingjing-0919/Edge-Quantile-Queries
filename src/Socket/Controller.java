package Socket;

import Config.config;
import Model.BaseStation;
import Model.Cell;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Controller {
    public static void sendMessage(ArrayList<Cell> cells){
        for (Cell cell : cells) {
            for (int j = 0; j < cell.arr.size(); j++) {
                BaseStation baseStation = cell.arr.get(j);
                if (cell.eta.get(baseStation) != 0.0) {
                    try {
                        Socket s = new Socket(baseStation.getIpAddress(), 8888);
                        OutputStream os = s.getOutputStream();
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
                        //send message to worker
                        bw.write(j+","+ (int) (config.dataVolume * cell.eta.get(baseStation))+","+config.DataFile+","+ 0.5+","+ 0);
                        bw.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

