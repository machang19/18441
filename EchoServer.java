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
        while (true) {
            try {
                clientSocket = serverSocket.accept();

            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }
            if (clientSocket != null)
            {
                Socket finalCS = clientSocket;
                threadPool.submit(() -> {
                    try {
                        serve(finalCS);
                    }
                    catch (IOException e)
                    {
                        
                    }
                });
            }
            clientSocket = null;
        }


        //out.close();
        //in.close();
        //clientSocket.close();
        //serverSocket.close();

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

     public static void serve(Socket clientSocket) throws IOException
     {
         System.out.println("Connection successful");
         System.out.println("Waiting for input.....");
         DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
         BufferedReader in = new BufferedReader(
                 new InputStreamReader(clientSocket.getInputStream()));

         String requestLine = in.readLine(); // Request-Line ; Section 5.1
         String param = in.readLine();
         List<String> headerParams = new ArrayList<>();
         while (param.length() > 0) {
             System.out.println(param);
             headerParams.add(param);
             param = in.readLine();
         }
         System.out.println(requestLine);
         System.out.println(headerParams);
         String[] request = requestLine.split(" ");
         String filepath = request[1];
         filepath = filepath.replaceAll("/", "");
         File testFile = new File(filepath);
         BufferedInputStream bis = null;
         OutputStream os = null;
         byte[] mybytearray = new byte[(int) testFile.length()];
         FileInputStream fis = new FileInputStream(testFile);
         bis = new BufferedInputStream(fis);
         bis.read(mybytearray, 0, mybytearray.length);
         os = clientSocket.getOutputStream();
         System.out.println("Sending " + testFile + "(" + mybytearray.length + " bytes)");
         SimpleDateFormat dateFormat = new SimpleDateFormat(
                 "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
         dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
         String time = dateFormat.format(Calendar.getInstance().getTime());

         out.writeBytes("HTTP/1.1 200 OK\r\n");
         out.writeBytes("Date: " + time + "\r\n");
         out.writeBytes("Connection: Keep-Alive\r\n");
         out.writeBytes("Content-Type: " + getContentType(filepath) + "\r\n\r\n");
         System.out.println(getContentType(filepath));
         os.write(mybytearray, 0, mybytearray.length);
         System.out.println("Done.");
     }
} 