import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class AgriDVRVisualizer extends JFrame {
    private class SensorNode {
        String id;
        String type; // "sensor", "relay", "gateway"
        Map<String, Integer> routingTable = new HashMap<>();
        boolean anomaly = false;
        Point pos;
        public SensorNode(String id, String type) {
            this.id = id;
            this.type = type;
        }
    }

    private class NetworkLink {
        SensorNode a, b;
        int cost;

        public NetworkLink(SensorNode a, SensorNode b, int cost) {
            this.a = a;
            this.b = b;
            this.cost = cost;
        }
    }

    private List<SensorNode> nodes = new ArrayList<>();
    private List<NetworkLink> links = new ArrayList<>();
    private JTextArea logArea;
    private javax.swing.Timer timer;  // Explicit Swing Timer
    private double animProgress = 0; // animation progress [0,1]
    private NetworkLink animLink = null;

    private DrawingPanel canvas;  // Declare canvas as a class-level field

    public AgriDVRVisualizer() {
        setTitle("Agri Distance Vector Routing Visualizer");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        setupNetwork();
        layoutNodes();

        JPanel leftPanel = new JPanel(new BorderLayout());
        canvas = new DrawingPanel();  // Initialize the class-level canvas
        leftPanel.add(canvas, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton startBtn = new JButton("Start");
        JButton stopBtn = new JButton("Stop");
        JButton anomalyBtn = new JButton("Inject Anomaly");
        JButton resetBtn = new JButton("Reset");
        controls.add(startBtn);
        controls.add(stopBtn);
        controls.add(anomalyBtn);
        controls.add(resetBtn);
        leftPanel.add(controls, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.CENTER);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(350, 0));
        add(logScroll, BorderLayout.EAST);

        timer = new javax.swing.Timer(40, e -> {
            stepRouting();
            animatePacket();
            canvas.repaint();
        });

        startBtn.addActionListener(e -> timer.start());
        stopBtn.addActionListener(e -> timer.stop());
        anomalyBtn.addActionListener(e -> {
            injectAnomaly();
            canvas.repaint();  // repaint the panel after anomaly injection
        });
        resetBtn.addActionListener(e -> {
            timer.stop();
            resetNetwork();
            canvas.repaint();
        });

        setVisible(true);
    }

    private void setupNetwork() {
        nodes.clear();
        links.clear();
        SensorNode s1 = new SensorNode("Soil1", "sensor");
        SensorNode s2 = new SensorNode("Soil2", "sensor");
        SensorNode pump = new SensorNode("Pump", "relay");
        SensorNode temp = new SensorNode("Temp", "relay");
        SensorNode gateway = new SensorNode("Gateway", "gateway");
        nodes.addAll(Arrays.asList(s1, s2, pump, temp, gateway));

        links.add(new NetworkLink(s1, gateway, 1));
        links.add(new NetworkLink(s2, gateway, 2));
        links.add(new NetworkLink(pump, gateway, 2));
        links.add(new NetworkLink(temp, gateway, 1));
        links.add(new NetworkLink(s1, s2, 1));
        links.add(new NetworkLink(s2, pump, 1));
        links.add(new NetworkLink(pump, temp, 1));

        for (SensorNode n : nodes) {
            n.routingTable.clear();
            for (SensorNode d : nodes) {
                n.routingTable.put(d.id, (n == d) ? 0 : Integer.MAX_VALUE / 2);
            }
        }
    }

    private void layoutNodes() {
        Map<String, Point> positions = Map.of(
                "Soil1", new Point(100, 200),
                "Soil2", new Point(300, 150),
                "Pump", new Point(300, 400),
                "Temp", new Point(500, 300),
                "Gateway", new Point(700, 250)
        );
        for (SensorNode n : nodes) {
            n.pos = positions.get(n.id);
        }
    }

    private void stepRouting() {
        Map<SensorNode, Map<String, Integer>> newTables = new HashMap<>();
        for (SensorNode n : nodes) {
            Map<String, Integer> updatedTable = new HashMap<>(n.routingTable);
            for (SensorNode dest : nodes) {
                if (dest == n) continue;
                int minCost = Integer.MAX_VALUE;
                for (NetworkLink link : links) {
                    SensorNode neighbor = null;
                    if (link.a == n) neighbor = link.b;
                    else if (link.b == n) neighbor = link.a;
                    if (neighbor != null) {
                        int c = link.cost + neighbor.routingTable.get(dest.id);
                        if (c < minCost) minCost = c;
                    }
                }
                updatedTable.put(dest.id, minCost);
            }
            newTables.put(n, updatedTable);
        }
        for (SensorNode n : nodes) {
            n.routingTable = newTables.get(n);
        }
        updateLogs();
    }

    private void updateLogs() {
        StringBuilder sb = new StringBuilder();
        for (SensorNode n : nodes) {
            sb.append(n.id).append(": ");
            n.routingTable.forEach((k, v) -> sb.append(k).append("->").append(v == Integer.MAX_VALUE / 2 ? "∞" : v).append(" "));
            sb.append("\n");
        }
        logArea.setText(sb.toString());
    }

    private void injectAnomaly() {
        Random rnd = new Random();
        SensorNode sn = nodes.get(rnd.nextInt(nodes.size() - 1));
        sn.anomaly = true;
        for (NetworkLink l : links) {
            if (l.a == sn || l.b == sn) {
                l.cost *= 4;
            }
        }
        updateLogs();
    }

    private void resetNetwork() {
        for (SensorNode n : nodes)
            n.anomaly = false;
        setupNetwork();
        layoutNodes();
        updateLogs();
    }

    private void animatePacket() {
        animProgress += 0.02;
        if (animLink == null || animProgress > 1) {
            animProgress = 0;
            if (!links.isEmpty())
                animLink = links.get(new Random().nextInt(links.size()));
        }
    }

    private class DrawingPanel extends JPanel {
        public DrawingPanel() {
            setBackground(Color.WHITE);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    for (SensorNode n : nodes) {
                        if (n.pos.distance(e.getPoint()) < 30) {
                            setToolTipText("<html><b>" + n.id + "</b><br>Routing Table:<br>" + routingTableString(n) + "</html>");
                            return;
                        }
                    }
                    setToolTipText(null);
                }
            });
        }

        private String routingTableString(SensorNode n) {
            StringBuilder sb = new StringBuilder();
            n.routingTable.forEach((k, v) -> sb.append(k).append(": ").append(v == Integer.MAX_VALUE / 2 ? "∞" : v).append("<br>"));
            return sb.toString();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (NetworkLink l : links) {
                float costNorm = Math.min(1.0f, (float) l.cost / 10);
                g.setColor(new Color(1.0f, 1.0f - costNorm, 1.0f - costNorm));
                g.setStroke(new BasicStroke(2 + 3 * costNorm));
                g.drawLine(l.a.pos.x, l.a.pos.y, l.b.pos.x, l.b.pos.y);

                int midX = (l.a.pos.x + l.b.pos.x) / 2;
                int midY = (l.a.pos.y + l.b.pos.y) / 2;
                g.setColor(Color.DARK_GRAY);
                g.drawString("Cost: " + l.cost, midX, midY);
            }

            for (SensorNode n : nodes) {
                g.setColor(n.anomaly ? Color.RED : Color.GREEN.darker());
                g.fillOval(n.pos.x - 20, n.pos.y - 20, 40, 40);
                g.setColor(Color.BLACK);
                g.drawOval(n.pos.x - 20, n.pos.y - 20, 40, 40);
                g.drawString(n.id, n.pos.x - 10, n.pos.y + 5);
            }

            if (animLink != null) {
                int x = (int) (animLink.a.pos.x + animProgress * (animLink.b.pos.x - animLink.a.pos.x));
                int y = (int) (animLink.a.pos.y + animProgress * (animLink.b.pos.y - animLink.a.pos.y));
                g.setColor(Color.MAGENTA);
                g.fillOval(x - 8, y - 8, 16, 16);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AgriDVRVisualizer::new);
    }
}
