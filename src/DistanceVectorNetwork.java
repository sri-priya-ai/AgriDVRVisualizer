import java.util.*;

public class DistanceVectorNetwork {
    public final List<SensorNode> nodes = new ArrayList<>();
    public final List<NetworkLink> links = new ArrayList<>();
    private final Map<SensorNode, Map<String, Integer>> lastReceived = new HashMap<>();

    public void step() {
        // Each node sends its distance vector to neighbors.
        for (SensorNode node : nodes) {
            for (NetworkLink link : links) {
                SensorNode neighbor = null;
                if (link.nodeA == node) neighbor = link.nodeB;
                else if (link.nodeB == node) neighbor = link.nodeA;
                if (neighbor != null) {
                    Map<String, Integer> received = new HashMap<>(node.routingTable);
                    lastReceived.put(neighbor, received);
                }
            }
        }
        // Each node updates its routing table.
        for (SensorNode node : nodes) {
            Map<String, Integer> updated = new HashMap<>();
            for (SensorNode dest : nodes) {
                if (dest == node) continue;
                int minCost = Integer.MAX_VALUE;
                for (NetworkLink link : links) {
                    SensorNode neighbor = null;
                    if (link.nodeA == node) neighbor = link.nodeB;
                    else if (link.nodeB == node) neighbor = link.nodeA;
                    if (neighbor != null && lastReceived.containsKey(neighbor)) {
                        int costToNeighbor = link.cost;
                        int costNeighborToDest = lastReceived.get(neighbor).getOrDefault(dest.id, Integer.MAX_VALUE / 2);
                        int totalCost = costToNeighbor + costNeighborToDest;
                        if (totalCost < minCost) minCost = totalCost;
                    }
                }
                if (minCost < Integer.MAX_VALUE) updated.put(dest.id, minCost);
            }
            node.routingTable.putAll(updated);
        }
    }

    // Minimal SensorNode class nested inside to fix missing type error.
    public static class SensorNode {
        public final String id;
        public Map<String, Integer> routingTable = new HashMap<>();

        public SensorNode(String id) {
            this.id = id;
        }
    }

    // Minimal NetworkLink class nested inside.
    public static class NetworkLink {
        public SensorNode nodeA;
        public SensorNode nodeB;
        public int cost;

        public NetworkLink(SensorNode nodeA, SensorNode nodeB, int cost) {
            this.nodeA = nodeA;
            this.nodeB = nodeB;
            this.cost = cost;
        }
    }
}
