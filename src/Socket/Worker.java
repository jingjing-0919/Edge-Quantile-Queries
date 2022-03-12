package Socket;

import Experiment.ConcurrentRunner;
import Model.BaseStation;
import Util.SingleQueryUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Worker {
    public static ArrayList<BaseStation> baseStations;
    public static void main(String[] args) throws IOException {
        baseStations = new ArrayList<>();
        SingleQueryUtil.initBaseStation(baseStations);
        int portNumber = 8888;
        PrintWriter out = null;
        BufferedReader in = null;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server is OK, is waiting for connect...");
            int id = 0;
            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("Hava a connect");
                GkRunner go = new GkRunner(clientSocket, id++);
                Thread t1 = new Thread(go);
                t1.start();

            }
        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (clientSocket != null) {
                clientSocket.
                        close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }

        }
    }

    static class GkRunner implements Runnable {
        PrintWriter out = null;
        BufferedReader in = null;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int id;

        public GkRunner(Socket clientSocket, int idd) throws IOException {
            id = idd;
            this.clientSocket = clientSocket;
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        @Override
        public void run() {
            try {

                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine;
                // Wait for input
                if ((inputLine = in.readLine()) != null) {
                    String [] split = inputLine.split(",");
                    ConcurrentRunner.run(baseStations.get(Integer.parseInt(split[0])),Integer.parseInt(split[1]),
                            split[2],Double.parseDouble(split[3]),Integer.parseInt(split[4]));

                }
                clientSocket.close();

            } catch (IOException e) {
                System.out.println(
                        "Exception caught when trying to listen on port " + id + " or listening for a connection");
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println(e.getMessage());
            } finally {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (clientSocket != null) {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }



}
