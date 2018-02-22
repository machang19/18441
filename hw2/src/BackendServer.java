import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;

import static java.lang.Integer.parseInt;

public class BackendServer {

    private static String filename;
    private static String hostname;
    private static int port;
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
    public   void addPeer(String filename, String hostname, int port)
    {
        this.filename = filename;
        String[] args = hostname.split(":");
        this.hostname = args[0];
        this.port = port;
    }
}
