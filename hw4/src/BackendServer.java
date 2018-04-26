import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.parseInt;

public class BackendServer {

    private static String filename;
    private static DatagramSocket dsock;
    private Socket sock;
    public static int bandwidth;
    private  ConcurrentMap<String, CopyOnWriteArrayList<InetAddress>> fileLookup = new ConcurrentHashMap<>();
    private ConcurrentMap<InetAddress, Integer> portLookup = new ConcurrentHashMap<>();
    static ConcurrentMap<String, Peer> peers = new ConcurrentHashMap<>();
    private static ConcurrentMap<String, Set<String>> searchDict = new ConcurrentHashMap<>();
    private static String uuid;
    public BackendServer(String uuid) {
        this.filename = "";
        this.sock = null;
        this.uuid = uuid;
    }

    public static void setPeers(ConcurrentMap<String, Peer> peers) {
        BackendServer.peers = peers;
    }



    private static void initialConnectionSetup(DatagramPacket dpack, File file, int port) {
        try {
            // tell client what size the file is
            DatagramSocket initSock = new DatagramSocket();
            InetAddress host = dpack.getAddress();
            String fileSize = "File size:" + file.length();
            byte initarr[] = fileSize.getBytes();
            DatagramPacket initpack = new DatagramPacket(initarr, initarr.length, host, port);
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
        long startTime;
        int bytesSent;

        while(true) {
            DatagramSocket dsock4 = new DatagramSocket(parseInt(args[0]));
            System.out.println("backend port =" + args[0]);
            int port = parseInt(args[0]);
            byte arr1[] = new byte[150];
            DatagramPacket dpack = new DatagramPacket(arr1, arr1.length );

            // receive request file
            System.out.println("waiting");
            dsock4.receive(dpack);
            dsock4.close();
            System.out.println("received dpack");
            byte arr2[] = dpack.getData();
            int packSize = dpack.getLength();
            String request = new String(arr2, 0, packSize);
            System.out.println(request);

            if (request.startsWith("do yo have"))
            {
                String[] rArgs = request.split(" ");
                String filename = rArgs[3];
                boolean shouldSearch = false;
                File f = new File(filename);

                if (!searchDict.containsKey(filename))
                {
                    searchDict.put(filename,new HashSet<>());
                    shouldSearch = true;
                }
                if(f.exists() && !f.isDirectory()) {
                    searchDict.get(filename).add(uuid);
                }
                int ttl = parseInt(rArgs[4]);
                int search_interval = parseInt(rArgs[5]);
                dsock = new DatagramSocket();
                String message = "";
                for (String s: searchDict.get(filename))
                {
                    message += " " + s;
                }
                byte arr[] = message.getBytes();
                InetAddress a = dpack.getAddress();
                DatagramPacket ack = new DatagramPacket(arr, arr.length, a, dpack.getPort());
                dsock.send(ack);
                for (Peer p : peers.values())
                {
                    Set<String> result = findPeers(filename,p,ttl, search_interval);
                    ttl = ttl-1;
                    searchDict.get(filename).addAll(result);
                    try {
                        Thread.sleep(search_interval);
                    }
                    catch (Exception e )
                    {
                        System.out.println(e);
                    }
                }

            }
            if (request.startsWith("Send this file:")) {
                DatagramSocket dsock5 = new DatagramSocket(port);
                String filepath = request.substring(15, request.length());
                file = new File(filepath);
                filesize = (int)file.length();
                initialConnectionSetup(dpack, file, port);

                // receive bandwidth limit
                byte arr3[] = new byte[20];
                dpack = new DatagramPacket(arr3, arr3.length);
                dsock5.receive(dpack);
                dsock5.close();
                byte arr4[] = dpack.getData();
                int bpackSize = dpack.getLength();
                String bmessage = new String(arr4, 0, bpackSize);
                System.out.println(request);
                if (bmessage.startsWith("Bandwidth:")) {
                    String blimit = bmessage.substring(10, bmessage.length());
                    bandwidth = parseInt(blimit);
                    System.out.println(bandwidth);
                }

                // receive Ack
                DatagramSocket checkSock = new DatagramSocket(port);
                int i = 0;
                byte[] filearray = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(filearray, 0, filearray.length);
                bytesSent = 0;
                startTime = 0;
                int delay = 0;
                while (i < filesize) {
                    if (i%100000 == 0)
                        System.out.println("in main function, i=" + i);
                    try {
                        //System.out.println("received packet and sending response");
                        long currTime = System.currentTimeMillis();
                        long elapsed = currTime - startTime;
                        float currRate = (float)bytesSent /(int)elapsed;
                        if (elapsed >= 5000) {
                            System.out.println("Inside bandwidth limiting");
                            System.out.println("Current rate: " + currRate + " bandwidth: " + bandwidth);
                            System.out.println(bandwidth != 0 && currRate > (float)bandwidth);
                            if (bandwidth != 0 && currRate > (float)bandwidth) {
                                // wait
                                delay = (int)(bytesSent/bandwidth - elapsed);
                                System.out.println("Add delay of: " + delay);
                                Thread.sleep(delay);
                            }
                            startTime = currTime;
                            bytesSent = 0;
                        }
                        InetAddress host = dpack.getAddress();
                        String index = i + "startindex";
                        byte iarr[] = index.getBytes();
                        for (int k = 0; k < iarr.length; k++)
                        {
                            sendarr[k] = iarr[k];
                        }
                        for (int j = i; j < Integer.min(i+maxSize-20, filesize); j++) {
                            sendarr[j-i+iarr.length] = filearray[j];
                        }
                        DatagramPacket responsePacket = new DatagramPacket(sendarr, sendarr.length, host, port);
                        checkSock.send(responsePacket);
                        bytesSent += maxSize;

                        if (receiveAck(host, port, checkSock)) {
                            i += maxSize-20;
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
                checkSock.close();
                System.out.println( new Date( ) + "  " + dpack.getAddress( ) + " : " + dpack.getPort( ) + " "+ request);
            }
        }
    }



    public static Set<String> findPeers(String filename, Peer p, int ttl, int interval) {
        Set<String> result = new HashSet<>();
        System.out.println("here");
        if (ttl == 0)
        {
            return result;
        }
        try {
            System.out.println(p.getHostname());
            dsock = new DatagramSocket();
            String message = "do yo have " + filename + " " + (ttl-1) + " " + interval;
            byte arr[] = message.getBytes();
            InetAddress a = InetAddress.getByName(p.getHostname());
            DatagramPacket ack = new DatagramPacket(arr, arr.length, a, p.getBport());
            dsock.send(ack);
            arr = new byte[300];
            DatagramPacket dpack = new DatagramPacket(arr, arr.length, a, p.getBport());
            dsock.setSoTimeout(1000);
            dsock.receive(dpack);
            byte[] data = dpack.getData();
            int length = dpack.getLength();
            String response = new String(data, 0, length);
            for (String s : response.split(","))
            {
                result.add(s);
            }
            return result;

        }
        catch (Exception E)
        {
            System.out.println(E);
        }
        return result;
    }

    public void addPeer(String filename, String host, int port) throws IOException {
        try {
            InetAddress hostAddress = InetAddress.getByName(host);
            if (fileLookup.containsKey(filename))
            {
                fileLookup.get(filename).add(hostAddress);

            }
            else
            {
                CopyOnWriteArrayList<InetAddress> result = new CopyOnWriteArrayList<>();
                result.add(hostAddress);
                fileLookup.put(filename,result);
            }
            portLookup.put(hostAddress,port);
            System.out.println(port);
            System.out.println(host);
            System.out.println(filename);

            System.out.println("connected");
        }
        catch (Exception e){
            System.out.println(e);
            System.out.println("Could not find host " + host + "on port " + port);
        }
    }
    private void sendAck(InetAddress host, int port, DatagramSocket dsock) {
        //System.out.println("Trying to send ack");
        try {
            String message = "ack";
            byte arr[] = message.getBytes();
            DatagramPacket ack = new DatagramPacket(arr, arr.length, host, port);
            dsock.send(ack);
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
    private static boolean receiveAck(InetAddress host, int port, DatagramSocket dsock) throws Exception{
        byte[] arr = new byte[150];
        DatagramPacket dpack = new DatagramPacket(arr, arr.length, host, port);
        try {
            dsock.setSoTimeout(1000);
            dsock.receive(dpack);
            byte[] data = dpack.getData();
            int length = dpack.getLength();
            String ack = new String(data, 0, length);
            if (ack.substring(0,3).equals("ack")) {
                return true;
            }
        }
        catch (Exception e) {
            System.out.println("Receive ack exception");
            System.out.println(e);
            return false;
        }
        return false;
    }
    public byte[] getContent(int start, int end, String filename) {
        try {
            System.out.println("trying to send");
            InetAddress host = fileLookup.get(filename).get(0);
            int port = portLookup.get(host);

            // request file from peer
            String message1 = "Send this file:" + filename;
            System.out.println(message1);
            byte arr[] = message1.getBytes( );
            DatagramPacket dpack = new DatagramPacket(arr, arr.length, host, port);
            dsock = new DatagramSocket();
            dsock.send(dpack);
            System.out.println("sent packet");

            // receive length of file
            arr = new byte[20];
            dpack = new DatagramPacket(arr, arr.length, host, port);
            DatagramSocket dsock2 = new DatagramSocket(port);
            dsock2.receive(dpack);
            dsock2.close();
            System.out.println("received packet");
            byte filearr[] = dpack.getData();
            String s2 = new String(filearr, 10, dpack.getLength()-10);
            System.out.println("length: " + s2);
            int length = parseInt(s2);

            // send bandwidth to peer server
            String bmessage = "Bandwidth:" + bandwidth;
            System.out.println(bmessage);
            byte arr2[] = bmessage.getBytes();
            System.out.println(host + ":" + port);
            dpack = new DatagramPacket(arr2, arr2.length, host, port);
            dsock = new DatagramSocket();
            dsock.send(dpack);
            System.out.println("sent bandwidth packet");

            byte[] result = new byte[length];
            int i = 0;
            dsock2 = new DatagramSocket(port);
            while (i < length-1020) {
                if (i%100000 == 0)
                    System.out.println("Outer loop, i=" + i);
                arr = new byte[1020];
                dpack = new DatagramPacket(arr, arr.length);
                dsock2.receive(dpack);

                s2 = new String(dpack.getData(), 0, dpack.getLength());
                i = parseInt(s2.split("startindex")[0]);
                int offset = s2.indexOf("startindex") + 10;
                byte barr[] = dpack.getData();
                for (int j = i; j < Integer.min(i + dpack.getLength() - offset, length); j++)
                {
                    result[j] = barr[j-i+offset];
                }
                sendAck(host, port, dsock2);

            }
            dsock2.close();
            System.out.println("returning");
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

    public void setRate(int rate) {
        this.bandwidth = rate;
    }

    public boolean checkContent(String filename, String hostname){
        try {
            System.out.println("Checking if content exists");
            InetAddress hostAddress = InetAddress.getByName(hostname);
            CopyOnWriteArrayList<InetAddress> result;
            result = fileLookup.get(filename);
            for (InetAddress iadd : result) {
                if (hostAddress == iadd) {
                    System.out.println("Found content");
                    return true;
                }
            }
        }
        catch(Exception e) {
            System.out.println(e);
        }
        return false;
    }
}