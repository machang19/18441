import java.util.Date;
import java.net.*;
import java.io.*;

import static java.lang.Integer.parseInt;

public class BackendServer {

    private static String filename;
    private static DatagramSocket dsock;
    private Socket sock;

    public BackendServer() {
        this.filename = "";
        this.sock = null;
    }
    public static void main( String args[]) throws Exception {
        // starter code
        dsock = new DatagramSocket(parseInt(args[0]));
        System.out.println("backend port =" + args[0]);
        byte arr1[] = new byte[150];
        DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

        while(true) {
            System.out.println("waiting");
            dsock.receive(dpack);
            System.out.println("received dpack");
            byte arr2[] = dpack.getData();
            int packSize = dpack.getLength();
            String s2 = new String(arr2, 0, packSize);
            System.out.println(s2);
            if (s2.startsWith("Send this file:"))
            {
                System.out.println("inside file send statement");
                try {
                    String filepath = s2.substring(15,s2.length());
                    File file = new File(filepath);
                    byte[] filearray = new byte[(int) file.length()];
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    bis.read(filearray, 0, filearray.length);
                    System.out.println("received packet and sending response");
                    DatagramSocket checkSock = new DatagramSocket();
                    System.out.println("ADDRESS: " + dpack.getAddress());
                    InetAddress iaddr = InetAddress.getByName("128.237.137.96");
                    checkSock.connect(iaddr, 8345);
                    DatagramPacket responsePacket = new DatagramPacket(filearray, filearray.length );
                    checkSock.send(responsePacket);
                    System.out.println("Sent response packet!");
                }
                catch (Exception e) {
                    System.out.println(e);
                }
            }
            System.out.println("after if statement");
            System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ s2);

        }
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

        byte[] result = {};
        try {
            System.out.println("trying to send");
            String message1 = "Send this file:" + this.filename;
            byte arr[] = message1.getBytes( );
            DatagramPacket dpack = new DatagramPacket(arr, arr.length);
            byte[] ipAddr = new byte[]{(byte)128, (byte)237, 125, (byte)153};
            //InetAddress host = InetAddress.getByAddress("128.237.125.153");
            InetAddress host = InetAddress.getByAddress(ipAddr);
            System.out.println("here2");
            this.dsock = new DatagramSocket(8345,host);
            System.out.println("here1");
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
