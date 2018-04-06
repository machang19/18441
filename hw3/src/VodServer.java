import javafx.util.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.*;
import java.io.*;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;

public class VodServer {
    static ExecutorService threadPool = Executors.newFixedThreadPool(12);
    static BackendServer bServer = new BackendServer();
    static String uuid;
    static int node;
    static int frontend_port;
    static int backend_port;
    static String content_dir;
    static int peer_count;
    static ConcurrentMap<String, Peer> peers = new ConcurrentHashMap<>();
    static ConcurrentMap<Integer, Peer> nodeToPeer = new ConcurrentHashMap<>();
    static ConcurrentMap<Integer,List<Pair<Integer,Integer>>> networkMap = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        String filename = "node.conf";
        ArrayList<Pair<Integer,Integer>> temp = new ArrayList<>();
        temp.add(new Pair<>(2,10));
        temp.add(new Pair<>(3,20));
        networkMap.put(1,temp);
        temp = new ArrayList<>();
        temp.add(new Pair<>(1,10));
        temp.add(new Pair<>(3,20));
        networkMap.put(2,temp);
        temp = new ArrayList<>();
        temp.add(new Pair<>(1,20));
        temp.add(new Pair<>(2,20));
        temp.add(new Pair<>(4,30));
        networkMap.put(3,temp);
        temp = new ArrayList<>();
        temp.add(new Pair<>(3,30));
        networkMap.put(4,temp);
        if (args.length == 1) {
            System.out.println("Config file provided");
            filename = args[0];

        }
        try {
            System.out.println("Trying to parse conf file");
            int i = parse_conf(filename);
        }
        catch (Exception e) {
            System.out.println("Cannot read file: " + filename);
        }
        try {
            serverSocket = new ServerSocket(frontend_port);
            System.out.println("after frontend");
            threadPool.submit(() -> {
                        try {
                            String[] b_args = {Integer.toString(backend_port)};
                            bServer.main(b_args);
                        }
                        catch (Exception e)
                        {

                        }
                    }

            );
            threadPool.submit(() -> {
                Long time = System.nanoTime();
                int numFails = 0;
                while (true) {
                    for (Peer p : peers.values()) {
                        try {
                            if (((System.nanoTime() - time) / 1_000_000_000.0) >= 10.0) {
                                time = System.nanoTime();
                                updateNetworkMap(p);
                                numFails = 0;
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                            numFails += 1;
                            if (numFails >= 3) {
                                System.out.println("3 Fails: neighbor " + p.getUuid() + " unreachable");
                                peers.remove(p.getUuid());
                            }
                        }
                    }
                }
            });
        }
        catch (IOException e) {
            System.err.println("Could not listen on port: " + frontend_port);
            System.exit(1);
        }
        Socket clientSocket = null;
        System.out.println("Waiting for connection.....");
        while (true) {
            try {

                clientSocket = serverSocket.accept();

            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
                break;
            }
            if (clientSocket != null) {
                Socket finalCS = clientSocket;
                threadPool.submit(() -> {
                    try {
                        serve(finalCS);
                    }
                    catch (IOException e) {

                    }
                });
            }
            clientSocket = null;
        }
        serverSocket.close();
    }

    private static void updateNetworkMap(Peer p) throws Exception{
        System.out.println("here");
        Socket socket = new Socket(p.getHostname(), p.getFport());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String time = dateFormat.format(Calendar.getInstance().getTime());
        System.out.println("updating network map");
        OutputStream os = socket.getOutputStream();
        out.writeBytes("GET /peer/neighbors HTTP/1.1\r\n");
        out.writeBytes("Date: " + time + "\r\n");
        out.writeBytes("Connection: Keep-Alive\r\n");
        out.writeBytes("Content-Type: application/json\r\n\r\n");
        System.out.println("Done.");
        String temp;
        StringBuffer response = new StringBuffer();
        while ((temp = in.readLine())!= null ) {
            response.append(temp);
            System.out.println(temp);
        }

        //print in String
        System.out.println("here");
        System.out.println(response.toString());
        System.out.println("here");
        //Read JSON response and print
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response.toString());
        System.out.println("here");
        System.out.println(json.toJSONString());
        System.out.println("here");
        out.close();
        socket.close();
        in.close();
    }

    static String getContentType(String filename) {
        System.out.println(filename);
        int i = filename.indexOf('.');
        String extension = filename.substring(i +1 );
        System.out.println(i);
        System.out.println(extension);
        extension = extension.toLowerCase();
        if (extension.equals("txt"))      { return "text/html"; }
        else if (extension.equals("css")) { return "text/css"; }
        else if (extension.equals("htm") || extension.equals("html")) { return "text/html"; }
        else if (extension.equals("gif")) { return "image/gif"; }
        else if (extension.equals("jpg") || extension.equals("jpeg")) { return "image/jpeg"; }
        else if (extension.equals("png")) { return "image/png"; }
        else if (extension.equals("js"))  { return "application/javascript"; }
        else if (extension.equals("mp4") || extension.equals("webm") || extension.equals("ogg") || extension.equals("ogv"))
        { return "video/webm";}
        else { return "application/octet-stream"; }
    }

    public static void serve(Socket clientSocket) throws IOException {
        System.out.println("Connection successful. Waiting for input.....");
        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));

        String requestLine = in.readLine();
        String param = in.readLine();
        List<String> headerParams = new ArrayList<>();
        String range = "";
        while (param.length() > 0) {
            String[] temp = param.split(":");
            if (range.equals("") && param.split(":")[0].equals("Range")) {
                range = (param.split(":")[1]).trim();
            }
            headerParams.add(param);
            param = in.readLine();
        }
        System.out.println(headerParams);
        System.out.println(range);
        String[] request = requestLine.split(" ");
        System.out.println(requestLine);
        String uri = request[1];
        String[] peerInfo = uri.split("/");
        String filepath;
        System.out.println("yo yo " + Arrays.asList(peerInfo));
        System.out.println("hi" + peerInfo[2]);
        byte[] filearr = {};
        if (peerInfo.length > 1 && peerInfo[1].equals("peer")) {
            Map<String,String> uri_params = parse_uri(uri);
            System.out.println(uri_params);
            filepath = uri_params.get("path");
            if (peerInfo[2].length() >= 11 && peerInfo[2].substring(0,11).equals("addneighbor")) {
                System.out.println("here");
                parse_addneighbor_uri(uri);
                System.out.println("here2");
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String time = dateFormat.format(Calendar.getInstance().getTime());
                System.out.println("returning uuid");
                OutputStream os = clientSocket.getOutputStream();
                out.writeBytes("HTTP/1.1 200 OK\r\n");
                out.writeBytes("Date: " + time + "\r\n");
                out.writeBytes("Connection: Keep-Alive\r\n");
                out.writeBytes("Content-Type: application/json\r\n\r\n");
                JSONArray arr = new JSONArray();
                for (Peer p : peers.values())
                {
                    JSONObject peer = new JSONObject();
                    peer.put("uuid",p.getUuid());
                    peer.put("host", p.getHostname());
                    peer.put("name", "node" + p.getNode());
                    peer.put("frontend", p.getFport());
                    peer.put("backend", p.getBport());
                    peer.put("metric", p.getDistance());
                    arr.add(peer);
                }
                byte [] mybytearray = arr.toJSONString().getBytes();
                os.write(mybytearray, 0, mybytearray.length);
                System.out.println("Done.");
                out.close();
                in.close();
                clientSocket.close();
                return;
            }
            else if (peerInfo[2].substring(0,3).equals("add")) {
                System.out.println("we are adding");
                bServer.addPeer(filepath, uri_params.get("host"), parseInt(uri_params.get("port")) );
                return;
            }else if (peerInfo[2].equals("kill")) {
                System.exit(0);// 0, 0 are dummy args dont do anything yet
            }
            else if (peerInfo[2].equals("view")) {
                filepath = peerInfo[3];
                System.out.println("viewing");
                filearr = bServer.getContent(0,0, filepath);
                try {
                    view(filepath, filearr, out, clientSocket);
                }
                catch (Exception e) {
                    System.out.println(e);
                }
                System.out.println(filearr);// 0, 0 are dummy args dont do anything yet
            }
            else if (peerInfo[2].equals("uuid")) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String time = dateFormat.format(Calendar.getInstance().getTime());
                System.out.println("returning uuid");
                OutputStream os = clientSocket.getOutputStream();
                out.writeBytes("HTTP/1.1 200 OK\r\n");
                out.writeBytes("Date: " + time + "\r\n");
                out.writeBytes("Connection: Keep-Alive\r\n");
                out.writeBytes("Content-Type: application/json\r\n\r\n");
                JSONObject uuidJSON = new JSONObject();

                uuidJSON.put("uuid",uuid);
                byte [] mybytearray = uuidJSON.toJSONString().getBytes();
                os.write(mybytearray, 0, mybytearray.length);
                System.out.println("Done.");
                out.close();
                in.close();
                clientSocket.close();
                return;
            }
            else if (peerInfo[2].equals("map")) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String time = dateFormat.format(Calendar.getInstance().getTime());
                System.out.println("returning uuid");
                OutputStream os = clientSocket.getOutputStream();
                out.writeBytes("HTTP/1.1 200 OK\r\n");
                out.writeBytes("Date: " + time + "\r\n");
                out.writeBytes("Connection: Keep-Alive\r\n");
                out.writeBytes("Content-Type: application/json\r\n\r\n");
                JSONObject mapJSON = new JSONObject();
                for (Integer n: networkMap.keySet())
                {
                    JSONObject temp = new JSONObject();
                    for (Pair<Integer,Integer> neighbor: networkMap.get(n))
                    {
                        temp.put("node" + neighbor.getKey(), neighbor.getValue());
                    }
                    mapJSON.put("node" + n, temp);
                }

                byte [] mybytearray = mapJSON.toJSONString().getBytes();
                os.write(mybytearray, 0, mybytearray.length);
                System.out.println("Done.");
                out.close();
                in.close();
                clientSocket.close();
                return;
            }
            else if (peerInfo[2].equals("rank")) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String time = dateFormat.format(Calendar.getInstance().getTime());
                System.out.println("returning uuid");
                OutputStream os = clientSocket.getOutputStream();
                out.writeBytes("HTTP/1.1 200 OK\r\n");
                out.writeBytes("Date: " + time + "\r\n");
                out.writeBytes("Connection: Keep-Alive\r\n");
                out.writeBytes("Content-Type: application/json\r\n\r\n");
                JSONArray arr = new JSONArray();
                Map<Integer,Integer> sPaths = find_shortest_paths(node);
                ArrayList<Pair<Integer,Integer>> unsortedPaths = new ArrayList<>();
                for (Integer n: sPaths.keySet())
                {
                    if (n != node ) {
                        int d = sPaths.get(n);

                        int i = 0;
                        for (Pair<Integer, Integer> j : unsortedPaths) {
                            if (j.getValue() > d) {
                                break;
                            }
                            i++;
                        }
                        unsortedPaths.add(i, new Pair<>(n, d));
                    }
                }
                for (Pair<Integer,Integer> j : unsortedPaths)
                {
                    JSONObject temp = new JSONObject();
                    temp.put("node" + j.getKey(), j.getValue());
                    arr.add(temp);
                }

                byte [] mybytearray = arr.toJSONString().getBytes();
                os.write(mybytearray, 0, mybytearray.length);
                System.out.println("Done.");
                out.close();
                in.close();
                clientSocket.close();
                return;
            }
            else if (peerInfo[2].equals("neighbors")) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String time = dateFormat.format(Calendar.getInstance().getTime());
                System.out.println("returning uuid");
                JSONArray arr = new JSONArray();
                for (Peer p : peers.values())
                {
                    JSONObject peer = new JSONObject();
                    peer.put("uuid",p.getUuid());
                    peer.put("host", p.getHostname());
                    peer.put("name", "node" + p.getNode());
                    peer.put("frontend", p.getFport());
                    peer.put("backend", p.getBport());
                    peer.put("metric", p.getDistance());
                    arr.add(peer);
                }
                byte [] mybytearray = arr.toJSONString().getBytes();
                OutputStream os = clientSocket.getOutputStream();
                out.writeBytes("HTTP/1.1 200 OK\r\n");
                out.writeBytes("Date: " + time + "\r\n");
                out.writeBytes("Connection: Keep-Alive\r\n");
                out.writeBytes("Content-Length: " + mybytearray.length + "\r\n");
                out.writeBytes("Content-Type: application/json\r\n\r\n");

                //os.write(mybytearray, 0, mybytearray.length);
                out.write(mybytearray, 0, mybytearray.length);
                System.out.println("Done.");
                out.close();
                in.close();
                clientSocket.close();
                return;
            }

            else if (peerInfo[2].substring(0,6).equals("config")) {
                int rateInd = peerInfo[2].indexOf("=");
                String rateStr = peerInfo[2].substring(rateInd+1);
                int rate = parseInt(rateStr);
                System.out.println("set bit rate to " + rate);
                bServer.setRate(rate);
                return;
            }
        }
        else {
            filepath = uri;
        }
        filepath = filepath.replaceAll("/", "");
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String time = dateFormat.format(Calendar.getInstance().getTime());
        try {
            File testFile = new File(filepath);
            BufferedInputStream bis = null;
            OutputStream os = null;
            byte[] mybytearray = new byte[(int) testFile.length()];
            if (filearr.length != 0)
            {
                mybytearray = filearr;
            }
            FileInputStream fis = new FileInputStream(testFile);
            bis = new BufferedInputStream(fis);
            if (range.equals("")) {
                bis.read(mybytearray, 0, mybytearray.length);
                os = clientSocket.getOutputStream();
                System.out.println("Sending " + testFile + "(" + mybytearray.length + " bytes)");
                out.writeBytes("HTTP/1.1 200 OK\r\n");
                out.writeBytes("Date: " + time + "\r\n");
                out.writeBytes("Connection: Keep-Alive\r\n");
                out.writeBytes("Content-Type: " + getContentType(filepath) + "\r\n\r\n");

                System.out.println(getContentType(filepath));
                os.write(mybytearray, 0, mybytearray.length);
                System.out.println("Done.");
            }
            else {
                String[] temp = range.split("=");
                String[] temp2 = temp[1].split("-");
                int start;
                int length;
                if (temp2.length == 1) {
                    if (temp[1].indexOf("-") == 0) {
                        length = parseInt(temp2[0]);
                        start = Integer.max(0, mybytearray.length - length);
                    }
                    else {
                        start = parseInt(temp2[0]);
                        length = mybytearray.length - start;
                    }
                }
                else {
                    start = parseInt(temp2[0]);
                    int end = parseInt(temp2[1]);
                    length = Integer.min(end - start + 1, mybytearray.length - start + 1);
                }
                System.out.println(start);
                System.out.println(length);
                bis.read(mybytearray, start, length);
                os = clientSocket.getOutputStream();
                System.out.println("Sending " + testFile + "(" + length + " bytes)");
                if (start == 0 && length == mybytearray.length) {
                    out.writeBytes("HTTP/1.1 200 OK\r\n");
                }
                else {
                    out.writeBytes("HTTP/1.1 206 Partial Content\r\n");
                }
                out.writeBytes("Date: " + time + "\r\n");
                out.writeBytes("Connection: Keep-Alive\r\n");
                out.writeBytes("Content-Type: " + getContentType(filepath) + "\r\n\r\n");
                System.out.println(getContentType(filepath));
                os.write(mybytearray, start, length);
                System.out.println("Done.");
            }
        }
        catch (FileNotFoundException e) {
            out.writeBytes("HTTP/1.1 404 Not Found\r\n");
            out.writeBytes("Date: " + time + "\r\n");
            out.writeBytes("Connection: Keep-Alive\r\n");
        }

        out.close();
        in.close();
        clientSocket.close();
    }

    private static void view(String filepath, byte[] filearr, DataOutputStream out, Socket clientSocket) throws Exception {
        OutputStream os = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String time = dateFormat.format(Calendar.getInstance().getTime());
        os = clientSocket.getOutputStream();
        out.writeBytes("HTTP/1.1 200 OK\r\n");
        out.writeBytes("Date: " + time + "\r\n");
        out.writeBytes("Connection: Keep-Alive\r\n");
        out.writeBytes("Content-Type: " + getContentType(filepath) + "\r\n\r\n");
        System.out.println(getContentType(filepath));
        os.write(filearr, 0, filearr.length);
        System.out.println("Done.");
    }

    private static Map<String,String> parse_uri(String uri) {
        Map<String,String> result = new HashMap<>();
        int starti = uri.indexOf("path=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            String path = uri.substring(starti + 5,endi);
            result.put("path", path);
        }
        starti = uri.indexOf("host=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            String host = uri.substring(starti + 5,endi);
            result.put("host", host);
        }
        starti = uri.indexOf("port=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            String port = uri.substring(starti + 5,endi);
            result.put("port", port);
        }
        starti = uri.indexOf("rate=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            String rate = uri.substring(starti + 5,endi);
            result.put("rate", rate);
        }

        return result;
    }


    private static void parse_addneighbor_uri(String uri) {
        Peer result = new Peer();
        int starti = uri.indexOf("uuid=");
        System.out.println(1);
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            result.setUuid(uri.substring(starti + 5,endi));
        }
        System.out.println(2);
        starti = uri.indexOf("host=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            result.setHostname(uri.substring(starti + 5,endi));
        }
        System.out.println(3);
        starti = uri.indexOf("frontend=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            result.setFport(parseInt(uri.substring(starti + 9,endi)));
        }
        System.out.println(4);
        starti = uri.indexOf("backend=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            result.setBport(parseInt(uri.substring(starti + 8,endi)));
        }
        System.out.println(5);
        starti = uri.indexOf("metric=");
        if (starti != -1) {
            int endi = uri.indexOf('&', starti);
            if(endi == -1) endi = uri.length();
            result.setDistance(parseInt(uri.substring(starti + 7,endi)));
        }

        System.out.println(6);
        int max = -1;
        for (Peer p : peers.values())
        {
            max = Integer.max(max, p.getNode());
        }
        result.setNode(max + 1);
        peers.put(result.getUuid(), result);
    }

    private static int parse_conf(String filename) throws Exception {
        FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);
        String line = null;

        // Populate with default values
        frontend_port = 18345;
        backend_port = 18346;
        content_dir = "content/";
        peer_count = 0;

        while ( (line = br.readLine()) != null) {
            // System.out.print("Line: " + line + ".....");
            int equalsInd = line.indexOf("=");
            if (line.startsWith("uuid")) {
                String c_uuid = line.substring(equalsInd+1, line.length()).trim();
                uuid = c_uuid;
            }
            else if(line.startsWith("name")) {
                String hostname = line.substring(equalsInd+1, line.length()).trim();
                node = parseInt(hostname.substring(4,hostname.length()));;
            }
            else if(line.startsWith("frontend_port")) {
                String fport = line.substring(equalsInd+1, line.length()).trim();
                frontend_port = Integer.parseInt(fport);
            }
            else if(line.startsWith("backend_port")) {
                String bport = line.substring(equalsInd+1, line.length()).trim();
                backend_port = parseInt(bport);
            }
            else if(line.startsWith("content_dir")) {
                String dir = line.substring(equalsInd+1, line.length()).trim();
                content_dir = dir;
            }
            else if(line.startsWith("peer_count")) {
                String numPeers = line.substring(equalsInd+1,line.length()).trim();
                peer_count  = parseInt(numPeers);
            }
            else if(line.startsWith("peer_")) {
                String peer_name = line.substring(0, equalsInd-1).trim();
                String peer_info = line.substring(equalsInd+1, line.length()).trim();
                Peer peer = new Peer();
                peer.update_params(peer_name,peer_info);
                peers.put(peer.getUuid(), peer);
            }
            else {
                System.out.println("Don't recognize line: " + line);
            }
        }
        if (uuid == null) {
            uuid = java.util.UUID.randomUUID().toString();
            String newline = "uuid = " + uuid;
            try {
                FileWriter fw = new FileWriter(filename);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(newline + "\n");
                bw.close();
            } catch (IOException e) {
                System.out.println("Could not write new uuid to file " + filename);
            }
        }
        System.out.println("returning from parse_conf");
        return 0;
    }

    private static Map<Integer,Integer> find_shortest_paths(Integer start)
    {
        Map<Integer,Integer> queue = new HashMap<>();
        Map<Integer,Integer> result = new HashMap<>();
        queue.put(start,0);
        while (queue.size() > 0)
        {
            int minV = -1;
            int minP = -1;
            for (Integer node : queue.keySet())
            {
                int v = queue.get(node);
                if (minP == -1 || v < minV)
                {
                    minP = node;
                    minV = v;
                }
            }
            queue.remove(minP);
            if (result.containsKey(minP)) continue;
            result.put(minP,minV);
            for (Pair<Integer,Integer> p : networkMap.get(minP))
            {
                int node = p.getKey();
                int distance = p.getValue();
                if (queue.containsKey(node))
                {
                    queue.put(node,Integer.min(queue.get(node),distance));
                }
                else
                {
                    queue.put(node,distance + minV);
                }
            }

        }
        return result;
    }
}