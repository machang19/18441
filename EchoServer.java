import java.net.*; 
import java.io.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class EchoServer 
{
    static ExecutorService threadPool = Executors.newFixedThreadPool(12);
    public static void main(String[] args) throws IOException {
         ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(8000);
        }
        catch (IOException e) {
             System.err.println("Could not listen on port: 8000.");
            System.exit(1);
        }
        Socket clientSocket = null;
        System.out.println("Waiting for connection.....");
        int x = 1;
        while (x == 1) {
            x = 2;
            try {
                clientSocket = serverSocket.accept();

            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            if (clientSocket != null)
            {
                Socket finalCS = clientSocket;
                serve(clientSocket);
//                threadPool.submit(() -> {
//                    try {
//                        serve(finalCS);
//                    }
//                    catch (IOException e)
//                    {
//
//                    }
//                });
            }
            clientSocket = null;
        }
     }

     static String getContentType(String filename)
     {
         System.out.println(filename);
         int i = filename.indexOf('.');
         String extension = filename.substring(i +1 );
         System.out.println(i);
         System.out.println(extension);
         extension = extension.toLowerCase();
         if (extension.equals("txt")) { return "text/html"; }
         else if (extension.equals("css")) {return "text/css";}
         else if (extension.equals("htm") || extension.equals("html")) {return "text/html";}
         else if (extension.equals("gif")) {return "image/gif";}
         else if (extension.equals("jpg") || extension.equals("jpeg")) {return "image/jpeg";}
         else if (extension.equals("png")) {return "image/png";}
         else if (extension.equals("js")) {return "application/javascript";}
         else if (extension.equals("mp4") || extension.equals("webm") || extension.equals("ogg") || extension.equals("ogv"))
         {return "video/webm";}
         else { return "application/octet-stream";}
     }

     static List<String> getHeader(BufferedReader in) {
        List<String> header = new ArrayList<>();
        try {
            String param = in.readLine();
            while (param.length() > 0) {
                header.add(param);
                param = in.readLine();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return header;
     }

     public static void serve(Socket clientSocket) throws IOException {
         System.out.println("Connection successful");
         System.out.println("Waiting for input.....");
         boolean keepAlive = true;
         while (keepAlive) {
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream()));

             String requestLine = in.readLine(); // Request-Line ; Section 5.1
             String param = in.readLine();
             List<String> headerParams = getHeader(in);

             keepAlive = headerParams.contains("Connection: keep-alive");
             System.out.println(requestLine);
             System.out.println(headerParams);

             // get file name
             String[] request = requestLine.split(" ");
             String filepath = request[1].substring(1); // remove leading slash
             File testFile = new File(filepath);

             byte[] mybytearray = new byte[(int) testFile.length()];
             FileInputStream fis = new FileInputStream(testFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             bis.read(mybytearray, 0, mybytearray.length);
             OutputStream os = clientSocket.getOutputStream();
             System.out.println("Sending " + testFile + "(" + mybytearray.length + " bytes)");
             SimpleDateFormat dateFormat = new SimpleDateFormat(
                     "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
             dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
             String time = dateFormat.format(Calendar.getInstance().getTime());

             // send back http header
             out.writeBytes("HTTP/1.1 200 OK\r\n");
             out.writeBytes("Date: " + time + "\r\n");
             out.writeBytes("Connection: Keep-Alive\r\n");
             out.writeBytes("Content-Type: " + getContentType(filepath) + "\r\n\r\n");
             os.write(mybytearray, 0, mybytearray.length);
             System.out.println("Done.");
//             clientSocket.close(); // For debugging
         }
     }
} 