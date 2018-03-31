import java.net.*;
import java.io.*;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Integer.parseInt;

public class VodServer {
    static ExecutorService threadPool = Executors.newFixedThreadPool(12);
    static BackendServer bServer = new BackendServer();
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;

        if (args.length < 2) {
            System.err.println("please specify a port number");
            System.exit(1);
        }
        try {
            serverSocket = new ServerSocket(parseInt(args[0]));
            threadPool.submit(() -> {
                        try {
                            String[] b_args = {args[1]};
                            bServer.main(b_args);
                        }
                        catch (Exception e)
                        {

                        }
                    }

            );
        }
        catch (IOException e) {
            System.err.println("Could not listen on port: " + args[0]);
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
        System.out.println(Arrays.asList(peerInfo));
        byte[] filearr = {};
        if (peerInfo.length > 1 && peerInfo[1].equals("peer")) {
            Map<String,String> uri_params = parse_uri(uri);
            System.out.println(uri_params);
            filepath = uri_params.get("path");
            if (peerInfo[2].substring(0,3).equals("add")) {
                System.out.println("we are adding");
                bServer.addPeer(filepath, uri_params.get("host"), parseInt(uri_params.get("port")) );
                return;
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
    private static Map<String,String> parse_conf(String filename) throws Exception {
        Map<String,String> result = new HashMap<>();
        Boolean uuidFound = false;
        FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);
        String line = null;

        while ( (line = br.readLine()) != null) {
            int equalsInd = line.indexOf("=");
            if (line.startsWith("uuid")) {
                String uuid = line.substring(equalsInd+1, line.length());
                result.put("uuid", uuid);
                uuidFound = true;
            }
            else if(line.startsWith("name")) {
                String name = line.substring(equalsInd+1, line.length());
                result.put("name", name);
            }
            else if(line.startsWith("frontend_port")) {
                String frontend_port = line.substring(equalsInd+1, line.length());
                result.put("frontend_port", frontend_port);
            }
            else if(line.startsWith("backend_port")) {
                String backend_port = line.substring(equalsInd+1, line.length());
                result.put("backend_port", backend_port);
            }
            else if(line.startsWith("content_dir")) {
                String content_dir = line.substring(equalsInd+1, line.length());
                result.put("content_dir", content_dir);
            }
            else if(line.startsWith("peer_count")) {
                String peer_count = line.substring(equalsInd+1,line.length());
                result.put("peer_count", peer_count);
            }
            else if(line.startsWith("peer_")) {
                String peer_name = line.substring(equalsInd-2);
                String peer_info = line.substring(equalsInd+1, line.length());
                result.put(peer_name, peer_info);
            }
            else {
                System.out.println("Don't recognize line: " + line);
            }
        }
        if (uuidFound == false) {
            // TODO: generate uuid
        }
        return result;
    }
}