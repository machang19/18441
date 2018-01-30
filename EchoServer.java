import java.net.*; 
import java.io.*; 

public class EchoServer 
{ 
 public static void main(String[] args) throws IOException 
   {
    ServerSocket serverSocket = null;

    try {
         serverSocket = new ServerSocket(8000);
        }
    catch (IOException e)
        {
         System.err.println("Could not listen on port: 10007.");
         System.exit(1);
        }
    Socket clientSocket = null;
    System.out.println("Waiting for connection.....");

    try {
        clientSocket = serverSocket.accept();
    } catch (IOException e) {
        System.err.println("Accept failed.");
        System.exit(1);
    }

    System.out.println("Connection successful");
    System.out.println("Waiting for input.....");

    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
    BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));



    File testFile = new File("C:\\Users\\Michael Chang\\18441\\test.txt");
    BufferedInputStream bis = null;
    OutputStream os = null;
    byte[] mybytearray = new byte[(int) testFile.length()];
    FileInputStream fis = new FileInputStream(testFile);
    bis = new BufferedInputStream(fis);
    bis.read(mybytearray, 0, mybytearray.length);
    os = clientSocket.getOutputStream();
    System.out.println("Sending " + testFile + "(" + mybytearray.length + " bytes)");
    out.writeBytes("HTTP/1.1 200 OK\r\n");
    out.writeBytes("Content-Type: text/html\r\n\r\n");
    os.write(mybytearray, 0, mybytearray.length);
    System.out.println("Done.");



    //out.close();
    //in.close();
    //clientSocket.close();
    //serverSocket.close();
   } 
} 