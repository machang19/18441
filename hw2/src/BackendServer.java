import java.util.Date;
import java.net.*;
import java.io.*;

import static java.lang.Integer.parseInt;

public class BackendServer {

    private static String filename;
    private DatagramSocket dsock;
    private Socket sock;

    public BackendServer() {
        this.filename = "";
        this.sock = null;
    }
    public static void main( String args[]) throws Exception {
        // starter code
        DatagramSocket dsock = new DatagramSocket(parseInt(args[0]));
        byte arr1[] = new byte[150];
        DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

        while(true) {
            dsock.receive(dpack);
            System.out.println("recieved dpack");
            byte arr2[] = dpack.getData();
            int packSize = dpack.getLength();
            String s2 = new String(arr2, 0, packSize);
            System.out.println(s2);
            if (s2.startsWith("Send this file:"))
            {
                String filepath = s2.substring(15,s2.length()-1);
                File file = new File(filepath);
                byte[] filearray = new byte[(int) file.length()];
                DatagramPacket responsePacket = new DatagramPacket(filearray, filearray.length );
                dsock.send(responsePacket);
            }
            System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ s2);

        }
    }
    public void addPeer(String filename, String host, int port) throws IOException {
        this.filename = filename;
        try {
            InetAddress hostAddress = InetAddress.getByName(host);
            this.dsock = new DatagramSocket(port,hostAddress);
            System.out.println("connected");
        }
        catch (UnknownHostException e){
            System.out.println("Could not find host " + host + "on port " + port);
        }
    }
    public byte[] getContent(int start, int end) {

        byte[] result = {};
        try {
            String message1 = "Send this file:" + this.filename;
            byte arr[] = message1.getBytes( );
            DatagramPacket dpack = new DatagramPacket(arr, arr.length);
            dsock.send(dpack);
            dsock.receive(dpack);
            byte filearr[] = dpack.getData();
            return filearr;
        }
        catch (IOException e)
        {
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
