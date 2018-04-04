import static java.lang.Integer.parseInt;
import static jdk.nashorn.internal.objects.ArrayBufferView.length;

public class Peer {
    private String uuid;
    private String hostname;
    private int node;
    private int fport;
    private int bport;
    private int distance;

    public String getUuid() { return uuid; }

    public void setBport(int bport) {
        this.bport = bport;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setNode(int node) {
        this.node = node;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setFport(int fport) {
        this.fport = fport;
    }

    public int getNode() {return node; }
    public String getHostname() { return hostname; }
    public int getFport() { return fport; }
    public int getBport() { return bport; }
    public int getDistance() { return distance; }

    public void update_params(String peer_name, String peer_info) {
        node = parseInt(peer_name.split("_")[1]);

        int start = 0;
        int end = peer_info.indexOf(",", start);
        System.out.print(peer_info.substring(start,end) + " | ");
        uuid = peer_info.substring(start, end);

        start = end+1;
        end = peer_info.indexOf(",", start);
        System.out.print(peer_info.substring(start,end) + " | ");
        hostname = peer_info.substring(start, end);

        start = end+1;
        end = peer_info.indexOf(",", start);
        System.out.print(peer_info.substring(start,end) + " | ");
        fport = parseInt(peer_info.substring(start, end));

        start = end+1;
        end = peer_info.indexOf(",", start);
        System.out.print(peer_info.substring(start,end) + " | ");
        bport = parseInt(peer_info.substring(start, end));

        start = end+1;
        end = peer_info.length();
        System.out.println(peer_info.substring(start,end));
        distance = parseInt(peer_info.substring(start, end));
    }
}
