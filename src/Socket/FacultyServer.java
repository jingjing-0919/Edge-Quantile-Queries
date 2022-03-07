package Socket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class FacultyServer {
    final static String INVALID = "Invalid command";
    final static String INVALID_NAME = "Invalid name";

    final static String FILE_NAME = "FacultyList.csv";
    final static List<Faculty> facultyList = new ArrayList<>();

    public static class Faculty {
        String name;
        String p;
        String dep;

        public String getName() {
            return name;
        }

        public String getP() {
            return p;
        }

        public String getDep() {
            return dep;
        }

        Faculty(String name, String p, String dep) {
            this.name = name;
            this.p = p;
            this.dep = dep;
        }

        @Override
        public String toString() {
            return String.format("%s, %s, %s", name, p, dep);
        }
    }

    public static void readFile() {
        File file = new File(FILE_NAME);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String s = reader.readLine();
            while (s != null) {
                String[] strs = s.replace("\n", "").split(",");
                facultyList.add(new Faculty(strs[0], strs[1], strs[2]));
                s = reader.readLine();
            }
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("IO Exception occurred when reading the file:" + FILE_NAME);
        }

    }

    public static String handleCommand(String s) {
        s = s.replace("\n", "");
        s = s.trim();

        int idx = s.indexOf(' ');
        if (idx == -1) {
            return INVALID;
        }

        String command = s.substring(0, idx);
        String content = s.substring(idx + 1);

        String result = "";
        switch (command.toUpperCase()) {
            case "NAME":
                result = handleNameCommand(content);
                break;
            case "FIRSTLETTER":
                result = handleFirstLetterCommand(content);
                break;
            case "DEP":
                result = handleDepCommand(content);
                break;
            default:
                result = INVALID;
        }
        return result;
    }

    public static String handleNameCommand(String s) {

        String content = facultyList.stream()
                .filter(e -> e.getName().equals(s))
                .map((e) -> e.toString())
                .collect(Collectors.joining("\n"));
        return content;
    }

    public static String handleFirstLetterCommand(String s) {
        String content = facultyList.stream()
                .filter(e -> e.getName().toLowerCase().charAt(0) == s.toLowerCase().charAt(0))
                .map((e) -> e.toString())
                .collect(Collectors.joining("\n"));
        return content;
    }

    public static String handleDepCommand(String s) {
        String content = facultyList.stream()
                .filter(e -> e.getDep().toLowerCase().contains(s.toLowerCase()))
                .map((e) -> e.toString())
                .collect(Collectors.joining("\n"));
        return content;
    }

    public static void main(String[] args) throws IOException {

        int portNumber = 8888;
        PrintWriter out = null;
        BufferedReader in = null;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        readFile();
//        while (true) {
//            try {
//                Scanner scaner = new Scanner(System.in);
//                String s = scaner.nextLine();
//                String result = handleCommand(s);
//                System.out.println(result);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server is OK, is waiting for connect...");
            int id = 0;
            while (true) {
                clientSocket = serverSocket.accept();
                System.out.println("Hava a connect");
                DoIt go = new DoIt(clientSocket, id++);
                Thread t1 = new Thread(go);
                t1.start();
//                out = new PrintWriter(clientSocket.getOutputStream(), true);
//                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//                String inputLine, outputLine;
//                // Wait for input
//                if ((inputLine = in.readLine()) != null) {
//                    out.println("Server got you!");
//                    String command = inputLine;
//                    String response = handleCommand(command);
//
//                    out.println(response);
//                }
//                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println(
                    "Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }

        }

    }

    static class DoIt implements Runnable {
        PrintWriter out = null;
        BufferedReader in = null;
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        int id;

        public DoIt(Socket clientSocket, int idd) throws IOException {
            id = idd;
            this.clientSocket = clientSocket;
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String inputLine, outputLine;
                // Wait for input
                if ((inputLine = in.readLine()) != null) {
                    System.out.println("Thread-" + id + ":Received " + inputLine);
                    //out.println("Server got you!"+id);
                    String command = inputLine;
                    String response = handleCommand(command);
                    out.println("server from Thread-" + id + " :" + response);
                    System.out.println("Thread-" + id + ":Command processed");
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

