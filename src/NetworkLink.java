public class NetworkLink {
    public SensorNode nodeA;
    public SensorNode nodeB;
    public int cost;

    public NetworkLink(SensorNode nodeA, SensorNode nodeB, int cost) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.cost = cost;
    }
}
