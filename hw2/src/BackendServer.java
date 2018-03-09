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
    private static void initialConnectionSetup(DatagramPacket dpack, File file) {
        try {
            // tell client what size the file is
            DatagramSocket initSock = new DatagramSocket();
            String strAddr = dpack.getAddress().toString();
            strAddr = strAddr.substring(1); // strip leading slash from address
//            InetAddress host = InetAddress.getByName(strAddr);
            InetAddress host = InetAddress.getByName("128.237.205.32");
            String fileSize = "File size:" + file.length();
            byte initarr[] = fileSize.getBytes();
            DatagramPacket initpack = new DatagramPacket(initarr, initarr.length, host, 8345);
            initSock.send(initpack);
            initSock.close();
            System.out.println("Sent initial packet");
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    public static void main( String args[]) throws Exception {
        int filesize = 0;
        int maxSize = 1020; // maximum packet size is 40 Bytes (for now)
        byte sendarr[] = new byte[maxSize];
        File file = new File("");

        while(true) {
            DatagramSocket dsock4 = new DatagramSocket(parseInt(args[0]));
            System.out.println("backend port =" + args[0]);
            byte arr1[] = new byte[150];
            DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

            System.out.println("waiting");
            dsock4.receive(dpack);
            dsock4.close();
            System.out.println("received dpack");
            byte arr2[] = dpack.getData();
            int packSize = dpack.getLength();
            String request = new String(arr2, 0, packSize);
            System.out.println(request);

            if (request.startsWith("Send this file:")) {
                String filepath = request.substring(15, request.length());
                file = new File(filepath);
                filesize = (int)file.length();
                initialConnectionSetup(dpack, file);
                System.out.println("Address: " + dpack.getAddress());
                System.out.println("Port: " + dpack.getPort());
                boolean ack = receiveAck(dpack.getAddress(), 8345);
                if (ack) {
                    int i = 0;
                    while (i < filesize) {
                        System.out.println("in main function, i=" + i);
                        try {
                            byte[] filearray = new byte[(int) file.length()];
                            FileInputStream fis = new FileInputStream(file);
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            bis.read(filearray, 0, filearray.length);

                            //System.out.println("received packet and sending response");
                            DatagramSocket checkSock = new DatagramSocket();
                            String strAddr = dpack.getAddress().toString();
                            strAddr = strAddr.substring(1); // strip leading slash from address
                            //InetAddress host = InetAddress.getByName(strAddr);
                            //InetAddress host = dpack.getAddress();
                            InetAddress host = InetAddress.getByName("128.237.205.32");
                            String index = i + "startindex";
                            byte iarr[] = index.getBytes();
                            //System.out.println("Host: " + strAddr);
                            for (int k = 0; k < iarr.length; k++)
                            {
                                sendarr[k] = iarr[k];
                            }
                            for (int j = i; j < i+maxSize-20; j++) {
                                sendarr[j-i+iarr.length] = filearray[j];
                            }
                            DatagramPacket responsePacket = new DatagramPacket(sendarr, sendarr.length, host, 8345);
                            checkSock.send(responsePacket);
                            checkSock.close();
                            System.out.println("Sent response packet!");
                            if (receiveAck(host, 8345)) {
                                i += maxSize-20;
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }
                System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ request);
            }
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
    private void sendAck(InetAddress host, int port) {
        System.out.println("Trying to send ack");
        try {
            String message = "ack";
            byte arr[] = message.getBytes();
            DatagramPacket ack = new DatagramPacket(arr, arr.length, host, port);
	        DatagramSocket dsock2 = new DatagramSocket();
            dsock2.send(ack);
            dsock2.close();
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    private static boolean receiveAck(InetAddress host, int port) throws Exception{
        byte[] arr = new byte[150];
	    DatagramSocket dsock2 = new DatagramSocket();
        DatagramPacket dpack = new DatagramPacket(arr, arr.length, host, port);
        try {
            dsock2 = new DatagramSocket(port);
            System.out.println("created new dsock");
    	    dsock2.setSoTimeout(1000);
            dsock2.receive(dpack);
            dsock2.close();
            byte[] data = dpack.getData();
            int length = dpack.getLength();
            String ack = new String(data, 0, length);
            System.out.println(ack);
            if (ack.substring(0,3).equals("ack")) {
                return true;
            }
        }
        catch (Exception e) {
            System.out.println("Receive ack exception");
            System.out.println(e);
            dsock2.close();
            return false;
        }
        return false;
    }
    public byte[] getContent(int start, int end) {
        try {
            System.out.println("trying to send");
            String message1 = "Send this file:" + "small.ogv";
            System.out.println(message1);
            byte arr[] = message1.getBytes( );

            InetAddress host = InetAddress.getByName("128.2.13.137");
	        DatagramPacket dpack = new DatagramPacket(arr, arr.length, host, 8345);

            System.out.println("here2");
            dsock = new DatagramSocket();
            System.out.println("here1");
            dsock.send(dpack);
            System.out.println("sent packet");
            arr = new byte[20];
            dpack = new DatagramPacket(arr, arr.length, host, 8345);

            // receive length of file
	        DatagramSocket dsock2 = new DatagramSocket(8345);
            dsock2.receive(dpack);
            dsock2.close();
            System.out.println("received packet");
            byte filearr[] = dpack.getData();
            String s2 = new String(filearr, 10, dpack.getLength()-10);
            System.out.println("length: " + s2);
            int length = parseInt(s2);

            byte[] result = new byte[length];
            int i = 0;
            while (i < length) {
                System.out.println("Outer loop, i=" + i);
                arr = new byte[1020];
                dpack = new DatagramPacket(arr, arr.length);
                sendAck(host, 8345);
                System.out.println("Ack was sent");
                dsock2 = new DatagramSocket(8345);
                dsock2.receive(dpack);
                dsock2.close();
                System.out.println("Packet received");
                s2 = new String(dpack.getData(), 0, dpack.getLength());
                i = parseInt(s2.split("startindex")[0]);
                int offset = s2.indexOf("startindex") + 10;
                //System.out.println("Packet: " + s2);
                byte barr[] = dpack.getData();
                System.out.println("Before copy loop");
                for (int j = i; j < i + dpack.getLength() - offset; j++)
                {
                    result[j] = barr[j-i+offset];
                }
                System.out.println("After copy loop");

            }
            sendAck(host, 8345);
            return result;
        }
        catch (Exception e)
        {
            System.out.println(e);
            byte[] result = {};
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
