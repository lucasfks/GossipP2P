package gossipP2P;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.*;


public class Peer {

    private static String ipAddress;
    private static int port;
    private static String folderPath;

    private static DatagramSocket socket;

    private static File[] files;

    private static String[] knownPeersIp = {"", ""};
    private static int[] knownPeersPort = {0, 0};

    private static ArrayList<Mensagem> processedRequests = new ArrayList<>(0);

    private static ArrayList<Mensagem> receivedResponses = new ArrayList<>(0);

    public static class PeerThread extends Thread {
        private String function;
        private Mensagem message;
        public PeerThread(String peerFunction) {
            this.function = peerFunction;
        }
        public PeerThread(String peerFunction, Mensagem incomingMessage) {
            this.function = peerFunction;
            this.message = incomingMessage;
        }

        public void run() {
            try {
                if (this.function.equals("searchFiles")) {
                    searchFiles();
                } else if (this.function.equals("processSearch")) {
                    processSearch(this.message);
                } else if (this.function.equals("processResponse")) {
                    processResponse(this.message);
                } else {
                    System.out.println("Invalid function for peer");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void startThread(String peerFunction) {
        PeerThread td1 = new PeerThread(peerFunction);
        td1.start();
    }

    public static void startThread(String peerFunction, Mensagem incomingMessage) {
        PeerThread td1 = new PeerThread(peerFunction, incomingMessage);
        td1.start();
    }

    public static class UpdateFiles extends TimerTask {
        public void run() {
            files = listFiles(folderPath);
            String filesString = "";
            for (File f : files) filesString += " " + f.getName();
            System.out.println("Sou peer " + ipAddress + ":" + port + " com arquivos" + filesString);
        }
    }


    public static File[] listFiles(String path) {
        File fObj = new File(path);
        File[] files = new File[0];
        if (fObj.exists() && fObj.isDirectory()) {
            // array for the files of the directory pointed by fObj
            files = fObj.listFiles();
        }
        return files;
    }

    // Converts Mensagem object to byte[]
    public static byte[] message2Bytes(Mensagem message) throws Exception {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(boas)) {
            oos.writeObject(message);
            return boas.toByteArray();
        }
    }

    public static Mensagem bytes2Message(byte[] data) throws Exception {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Mensagem deserializedUser = (Mensagem) ois.readObject();
            return deserializedUser;
        }
    }

    public static byte[] readFileInBytes(File file) throws Exception {

        // Creating an object of FileInputStream to
        // read from a file
        FileInputStream fl = new FileInputStream(file);

        // Now creating byte array of same length as file
        byte[] arr = new byte[(int)file.length()];

        // Reading file content to byte array
        // using standard read() method
        fl.read(arr);

        // lastly closing an instance of file input stream
        // to avoid memory leakage
        fl.close();

        // Returning above byte array
        return arr;
    }

    public static void writeFileAsBytes(String fullPath, byte[] bytes) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fullPath)) {
            fos.write(bytes);
            //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        }
    }

    public static int getRandomIndex(int length) {

        int rnd = new Random().nextInt(length);

        return rnd;
    }

    public static void showFileNames(String path) {
        File fObj = new File(path);
        File[] files = new File[0];
        if (fObj.exists() && fObj.isDirectory()) {
            // array for the files of the directory pointed by fObj
            files = fObj.listFiles();
        }
        String filesString = "";
        for (File f : files) filesString += " " + f.getName();
        System.out.println("arquivos da pasta:" + filesString);
    }


    // function to search file in receivedResponses
    private static boolean fileInResponses(String filename, ArrayList<Mensagem> receivedResponses) {
        for (Mensagem m : receivedResponses) {
            if (m.filename.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    public static void searchFiles() throws Exception {
        String fileToSearch;
        String command;

        boolean fileFound = false;

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Type command SEARCH if you want to search for a file.");
            command = scanner.next();
            if (!command.equals("SEARCH")) {
                System.out.println("Invalid command. Acceptable command: SEARCH");
            } else {
                System.out.println("What file do you want to search for?");
                fileToSearch = scanner.next();

                // creates Mensagem object and save it as a byte array to be sent to the other peer
                Mensagem searchMessage = new Mensagem("SEARCH", ipAddress, port, fileToSearch);

                // sending packet
                byte[] sendData = message2Bytes(searchMessage);

                int peer = getRandomIndex(knownPeersIp.length);

                InetAddress peerIp = InetAddress.getByName(knownPeersIp[peer]);

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, peerIp, knownPeersPort[peer]);
                socket.send(sendPacket);


                // Receiving RESPONSE:

                // start timer (in milliseconds)
                long startTime = System.currentTimeMillis(); //fetch starting time
                while (false||(System.currentTimeMillis()-startTime)<10000) {
                    // look for the file searched in receivedResponses
                    fileFound = fileInResponses(fileToSearch, receivedResponses);
                    if (fileFound) break;
                }
                if (!fileFound) {
                    System.out.println("ninguém no sistema possui o arquivo " + fileToSearch);
                }


            }
        }
    }

    public static void processSearch(Mensagem incomingMessage) throws Exception {

        for (Mensagem request : processedRequests) {
            if (incomingMessage.equals(request)) {
                System.out.println("requisição já processada para " + incomingMessage.filename);
                return;
            }
        }
        // if the request has not yet been processed
        processedRequests.add(incomingMessage);


        byte[] sendBuf = new byte[1024];

        // Searches for file in the peer's directory
        for (File file : files) {
            // If it has the file, send a RESPONSE to the original peer that requested it
            if (file.getName().equals(incomingMessage.filename)) {

                Mensagem response = new Mensagem("RESPONSE", ipAddress,
                        port, incomingMessage.filename, readFileInBytes(file));

                sendBuf = message2Bytes(response);

                // gets IP Address and Port od the peer who originally did the SEARCH request
                InetAddress IPAddress = InetAddress.getByName(incomingMessage.originIpAddress);
                int originPort = incomingMessage.originPort;

                DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, originPort);

                System.out.println("tenho " + incomingMessage.filename + " respondendo para " +
                        incomingMessage.originIpAddress + ":" + incomingMessage.originPort);
                socket.send(sendPacket);
                return;

            }
        }
        // If peer does not have the file, redirect the SEARCH request to another peer
        byte[] sendData = message2Bytes(incomingMessage);

        int peer = getRandomIndex(knownPeersIp.length);

        InetAddress peerIp = InetAddress.getByName(knownPeersIp[peer]);

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, peerIp, knownPeersPort[peer]);

        System.out.println("não tenho " + incomingMessage.filename + ", encaminhando para " +
                knownPeersIp[peer] + ":" + knownPeersPort[peer]);
        socket.send(sendPacket);

    }

    public static void processResponse(Mensagem incomingMessage) throws Exception {

        try {
            // in case it is Windows
            writeFileAsBytes(folderPath + "\\" + incomingMessage.filename, incomingMessage.fileContent);
        } catch (Exception e) {
            // in case it is MacOS or Linux
            writeFileAsBytes(folderPath + "/" + incomingMessage.filename, incomingMessage.fileContent);
        }

        // adds the response message to receivedResponses ArrayList
        receivedResponses.add(incomingMessage);

        System.out.println("peer com arquivo procurado: " +
                incomingMessage.originIpAddress + ":" + incomingMessage.originPort +
                " " + incomingMessage.filename);
    }

    // gateway receives SEARCH and RESPONSE messages
    public static void gateway() throws Exception {
        // creates socket
        try {
            socket = new DatagramSocket(port, InetAddress.getByName(ipAddress));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Updates "files" array
        Timer timer = new Timer();
        TimerTask task = new UpdateFiles();
        timer.schedule(task, 0, 30000); // updates "files" array every 30 seconds


        startThread("searchFiles");

        while (true) {
            // Declares receiving buffer
            byte[] recBuffer = new byte[1024];

            // Creates DatagramPacket to be received
            DatagramPacket recPkt = new DatagramPacket(recBuffer, recBuffer.length);

            // receives DatagramPacket (blocking method)
            socket.receive(recPkt);   // BLOCKING

            Mensagem incomingMessage = bytes2Message(recPkt.getData());

            if (incomingMessage.messageType.equals("SEARCH")) {
                startThread("processSearch", incomingMessage);
            } else if (incomingMessage.messageType.equals("RESPONSE")) {
                startThread("processResponse", incomingMessage);
            } else {
                System.out.println("Message type must be SEARCH or RESPONSE.");
            }

        }
    }


    public static void initialize() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Insert IP address:");
        ipAddress = scanner.next();

        System.out.println("Insert port:");
        port = scanner.nextInt();

        System.out.println("Insert folder path:");
        folderPath = scanner.next();

        System.out.println("Insert first peer's IP address:");
        knownPeersIp[0] = scanner.next();
        System.out.println("Insert first peer's port:");
        knownPeersPort[0] = scanner.nextInt();

        System.out.println("Insert second peer's IP address:");
        knownPeersIp[1] = scanner.next();
        System.out.println("Insert second peer's port:");
        knownPeersPort[1] = scanner.nextInt();

        showFileNames(folderPath);
    }



    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        String function = "";

        while (true) {
            System.out.println("Choose which function to execute (INICIALIZA or SEARCH):");
            function = scanner.next();
            if (function.equals("INICIALIZA")) {
                initialize();
                break;
            } else {
                System.out.println("You must first set up your peer with INICIALIZA before executing a SEARCH.");
            }
        }

        gateway();
    }
}
