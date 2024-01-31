package fsa;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Node {
    private final Set<Edge> edgesOut = new HashSet<>();
    private final Set<Edge> edgesIn = new HashSet<>();
    private String name;
    private NodePosition position;

    public Node(String name, NodePosition position) {
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return this.name;
    }

    public NodePosition getPosition() {
        return position;
    }

    public void setPosition(NodePosition position) {
        this.position = position;
    }

    public Set<Edge> getEdgesOut() {
        return Collections.unmodifiableSet(edgesOut);
    }

    public Set<Edge> getEdgesIn() {
        return Collections.unmodifiableSet(edgesIn);
    }

    public void addEdge(Edge edge) {
        if (edge.destination() == this) {
            edgesIn.add(edge);
        }
        if (edge.source() == this) {
            edgesOut.add(edge);
        }
    }

    public void removeEdge(Edge edge) {
        edgesIn.remove(edge);
        edgesOut.remove(edge);
    }

    public void move(double dx, double dy) {
        this.position = this.position.add(dx, dy);
    }
}
