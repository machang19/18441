import java.util.Date;
import java.net.*;
import java.io.*;

import static java.lang.Integer.parseInt;

public class BackendServer {

    private static String filename;
    private static DatagramSocket dsock1;
    private static DatagramSocket dsock;
    private Socket sock;

    public BackendServer() {
        this.filename = "";
        this.sock = null;
    }
    private static boolean initialConnectionSetup(DatagramPacket dpack, File file) {
        System.out.print("Initializing connection...");
        try {
            // tell client what size the file is
            while (true) {
                DatagramSocket initSock = new DatagramSocket();
                String strAddr = dpack.getAddress().toString();
                strAddr = strAddr.substring(1); // strip leading slash from address
                InetAddress iaddr = InetAddress.getByName("128.237.160.226");
                System.out.println(strAddr);
                initSock.connect(iaddr, 8345);
                System.out.println("Connected!");
                String fileSize = "File size:" + file.length();
                byte initarr[] = fileSize.getBytes();
                DatagramPacket initpack = new DatagramPacket(initarr, initarr.length);
                initSock.send(initpack);
                System.out.println("Sent initial packet");
                if (true)
                {
                    return true;
                }
            }
        }
        catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }
    public static void main( String args[]) throws Exception {
        // starter code
        dsock1 = new DatagramSocket(parseInt(args[0]));
        System.out.println("backend port =" + args[0]);
        byte arr1[] = new byte[150];
        DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

        while(true) {
            System.out.println("waiting");
            dsock1.receive(dpack);
            System.out.println("received dpack");
            byte arr2[] = dpack.getData();
            int packSize = dpack.getLength();
            String s2 = new String(arr2, 0, packSize);
            System.out.println(s2);
            if (s2.startsWith("Send this file:")) {
                System.out.println("inside file send statement");
                String filepath = s2.substring(15, s2.length());
                File file = new File(filepath);
                int filesize = (int)file.length();
                initialConnectionSetup(dpack, file);
                System.out.println("Finished initial connection setup");
//                try {
//                    byte[] filearray = new byte[(int) file.length()];
//                    FileInputStream fis = new FileInputStream(file);
//                    BufferedInputStream bis = new BufferedInputStream(fis);
//                    bis.read(filearray, 0, filearray.length);
//                    System.out.println("received packet and sending response");
//                    DatagramSocket checkSock = new DatagramSocket();
//                    System.out.println("ADDRESS: " + dpack.getAddress());
////                    InetAddress iaddr = InetAddress.getByName("128.237.137.96");
//                    InetAddress iaddr = dpack.getAddress();
//                    System.out.println(iaddr.toString());
//                    checkSock.connect(iaddr, 8345);
//                    DatagramPacket responsePacket = new DatagramPacket(filearray, filearray.length );
//                    checkSock.send(responsePacket);
//                    System.out.println("Sent response packet!");
////                }
//                catch (Exception e) {
//                    System.out.println(e);
//                }
            }
            System.out.println("after if statement");
            System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ s2);

        }
    }
    private void sendAck(DatagramSocket dsock) {
        try {
            String message = "ack";
            byte arr[] = message.getBytes();
            DatagramPacket ack = new DatagramPacket(arr, arr.length);
            dsock.send(ack);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    private static boolean receiveAck(DatagramSocket dsock) {
        byte[] arr = new byte[150];
        DatagramPacket dpack = new DatagramPacket(arr, arr.length);
        try {
            dsock.setSoTimeout(1000);
            dsock.receive(dpack);
            byte[] data = dpack.getData();
            int length = dpack.getLength();
            String ack = new String(data, 0, length);
            System.out.println(ack);
            if (ack == "ack") {
                return true;
            }
        }
        catch (Exception e) {
            System.out.println(e);
            return false;
        }
        return false;
    }
    public void addPeer(String filename, String host, int port) throws IOException {
        this.filename = filename;
        try {
            System.out.println(port);
            System.out.println(host);
            System.out.println(filename);
            InetAddress hostAddress = InetAddress.getByName(host);
            this.dsock = new DatagramSocket(port,hostAddress);
            System.out.println("connected");
        }
        catch (Exception e){
            System.out.println(e);
            System.out.println("Could not find host " + host + "on port " + port);
        }
    }
    public byte[] getContent(int start, int end) {
        System.out.println("inside getContent()");
        byte[] result = {};
        try {
            System.out.println("trying to send");
            String message1 = "Send this file:" + "small.ogv";
            byte arr[] = message1.getBytes( );
            DatagramPacket dpack = new DatagramPacket(arr, arr.length);
            InetAddress host = InetAddress.getByName("128.2.13.138");
            this.dsock = new DatagramSocket();
            this.dsock.connect(host,8345);
            System.out.println("connected to DatagramSocket");
            dsock.send(dpack);
            System.out.println("sent packet");
            dsock.receive(dpack);
            System.out.println("received packet");
            byte filearr[] = dpack.getData();
            return filearr;
        }
        catch (Exception e)
        {
            System.out.println(e);
            return result;
        }
    }



    public void sendHeader(int size) {
        // 16 bits for source port, 16 bits for destination port
        // 32 bits for sequence number
        // 32 bits for acknowledgement number
        // 16 bits for CRC/checksum, 16 bits for padding
    }
    public void send(int start, int end) {

    }
}
