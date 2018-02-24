import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;

import static java.lang.Integer.parseInt;

public class BackendServer {

    private static String filename;
    private static String hostname;
    private static int port;
    private DatagramSocket dsock;
    public BackendServer() {
        this.filename = "";
        this.hostname = "";
        this.port = -1;
    }
    public static void main( String args[]) throws Exception
    {
        DatagramSocket dsock = new DatagramSocket(7077);
        byte arr1[] = new byte[150];
        DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

        while(true)
        {
            dsock.receive(dpack);
            byte arr2[] = dpack.getData();
            int packSize = dpack.getLength();
            String s2 = new String(arr2, 0, packSize);

            System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ s2);
            dsock.send(dpack);
        }
    }
    public void addPeer(String filename, String hostname, int port)
    {
        this.filename = filename;
        String[] args = hostname.split(":");
        this.hostname = args[0];
        this.port = port;
    }
    public void getContent(int start, int end){

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
