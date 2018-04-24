import java.io.*;

public class HttpHandler {

    private DataOutputStream out;
    private String request;

    public void setDataStream(OutputStream os) {
        out = new DataOutputStream(os);
    }

    public void sendHttp() throws IOException {
        if (request == null) {
            out.writeBytes("");
        }
        out.writeBytes(request);
    }

    public void setHttp(String[] args) {
        String newLine;
        for (int i = 0; i < args.length; i++) {
            newLine = args[i];
            newLine.concat("\r\n");
            request.concat(newLine);
        }
        request.concat("\r\n");
    }
}
