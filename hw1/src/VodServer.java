

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

import static java.lang.Integer.parseInt;

public class VodServer
{
    static ExecutorService threadPool = Executors.newFixedThreadPool(12);
    public static void main(String[] args) throws IOException {
         ServerSocket serverSocket = null;

        if (args.length == 0)
        {
            System.err.println("please specify a port number");
            System.exit(1);
        }
        try {
            serverSocket = new ServerSocket(parseInt(args[0]));
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



        serverSocket.close();

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

         String requestLine = in.readLine();
         String param = in.readLine();
         List<String> headerParams = new ArrayList<>();
         String range = "";
         while (param.length() > 0) {
             String[] temp = param.split(":");

             if (range.equals("") && param.split(":")[0].equals("Range"))
             {
                 range = (param.split(":")[1]).trim();
             }
             headerParams.add(param);
             param = in.readLine();
         }
         System.out.println(range);
         String[] request = requestLine.split(" ");
         String filepath = request[1];
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
                 if (start == 0 && length == mybytearray.length)
                 {
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
         catch (FileNotFoundException e)
         {
             out.writeBytes("HTTP/1.1 404 Not Found\r\n");
             out.writeBytes("Date: " + time + "\r\n");
             out.writeBytes("Connection: Keep-Alive\r\n");
         }

         out.close();
         in.close();
         clientSocket.close();
     }

} 