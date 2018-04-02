import static java.lang.Integer.parseInt;
import static jdk.nashorn.internal.objects.ArrayBufferView.length;

public class Peer {
    private String uuid;
    private String hostname;
    private int fport;
    private int bport;
    private int distance;

    public String getUuid() { return uuid; }
    public String getName() { return hostname; }
    public int getFport() { return fport; }
    public int getBport() { return bport; }
    public int getDistance() { return distance; }

    public void update_params(String peer_info) {
        int start = 0;
        int end = peer_info.indexOf(",", start);
        uuid = peer_info.substring(start, end);

        start = end;
        end = peer_info.indexOf(",", start);
        hostname = peer_info.substring(start, end);

        start = end;
        end = peer_info.indexOf(",", start);
        fport = parseInt(peer_info.substring(start, end));

        start = end;
        end = peer_info.indexOf(",", start);
        bport = parseInt(peer_info.substring(start, end));

        start = end;
        end = peer_info.indexOf(",", start);
        distance = parseInt(peer_info.substring(start, end));
    }
}
