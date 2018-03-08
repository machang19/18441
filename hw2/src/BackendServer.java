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
            InetAddress iaddr = InetAddress.getByName(strAddr);
            initSock.connect(iaddr, 8345);
            String fileSize = "File size:" + file.length();
            byte initarr[] = fileSize.getBytes();
            DatagramPacket initpack = new DatagramPacket(initarr, initarr.length);
            initSock.send(initpack);
            System.out.println("Sent initial packet");
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    public static void main( String args[]) throws Exception {
        int filesize = 0;
        int maxSize = 40; // maximum packet size is 40 Bytes (for now)
        byte sendarr[] = new byte[maxSize];
        File file = new File("");

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
            String request = new String(arr2, 0, packSize);
            System.out.println(request);

            if (request.startsWith("Send this file:")) {
                String filepath = request.substring(15, request.length());
                file = new File(filepath);
                filesize = (int)file.length();
                initialConnectionSetup(dpack, file);
            }
            boolean ack = receiveAck(dpack.getAddress(), dpack.getPort());
            if (ack) {
                int i = 0;
                while (i < filesize) {
                    try {
                        byte[] filearray = new byte[(int) file.length()];
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        bis.read(filearray, 0, filearray.length);

                        System.out.println("received packet and sending response");
                        DatagramSocket checkSock = new DatagramSocket();
                        String strAddr = dpack.getAddress().toString();
                        strAddr = strAddr.substring(1); // strip leading slash from address
                        InetAddress host = InetAddress.getByName(strAddr);

                        for (int j = i; j < i+40; j++) {
                            sendarr[j-i] = filearray[j];
                        }
                        DatagramPacket responsePacket = new DatagramPacket(sendarr, maxSize, host, 8345);
                        checkSock.send(responsePacket);
                        System.out.println("Sent response packet!");
			if (receiveAck(host, 8345)) {
		            i += maxSize;
		        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
            System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ request);

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
        try {
            String message = "ack";
            byte arr[] = message.getBytes();
            DatagramPacket ack = new DatagramPacket(arr, arr.length, host, port);
	    DatagramSocket dsock = new DatagramSocket(port);
            dsock.send(ack);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    private static boolean receiveAck(InetAddress host, int port) {
        byte[] arr = new byte[150];
        DatagramPacket dpack = new DatagramPacket(arr, arr.length, host, port);
        try {    
	    DatagramSocket dsock = new DatagramSocket(port);
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
    public byte[] getContent(int start, int end) {
        try {
            System.out.println("trying to send");
            String message1 = "Send this file:" + "small.ogv";
            System.out.println(message1);
            byte arr[] = message1.getBytes( );
            
            InetAddress host = InetAddress.getByName("128.2.13.145");
	    DatagramPacket dpack = new DatagramPacket(arr, arr.length, host, 8345);

            System.out.println("here2");
            this.dsock = new DatagramSocket();
            System.out.println("here1");
            dsock.send(dpack);
            System.out.println("sent packet");
            arr = new byte[20];
            dpack = new DatagramPacket(arr, arr.length);

            // receive length of file
	    this.dsock = new DatagramSocket(8345);
            dsock.receive(dpack);
            System.out.println("received packet");
            byte filearr[] = dpack.getData();
            String s2 = new String(filearr, 10, dpack.getLength());
            System.out.println(s2);
            int length = parseInt(s2);
            sendAck(host, 8345);

            byte[] result = new byte[length];
            int i = 0;
            while (result.length < length) {
                arr = new byte[40];
                dpack = new DatagramPacket(arr, arr.length);
                sendAck(host, 8345);
                dsock.receive(dpack);
                s2 = new String(filearr, 0, dpack.getLength());
                System.out.println(s2);
                byte barr[] = dpack.getData();
                for (int j = i; j < i + dpack.getLength(); j++)
                {
                    result[j] = barr[j-i];
                }
                i += 40;
                //TODO: add ack from sender too
//                if (ack) {
//                    i += 40;
//                }
//                else {
//                    // TODO: add timeout capability
//                }
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
