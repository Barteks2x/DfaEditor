package fsa;

import java.util.*;

public abstract class Graph extends DatabaseObject {
    private final Set<Node> nodes = Collections.newSetFromMap(new IdentityHashMap<>());

    public Graph(int id, String name) {
        super(id, name);
    }

    public Edge addEdge(Node fromNode, Node toNode) {
        //noinspection SuspiciousMethodCalls
        if (!nodes.contains(fromNode) || !nodes.contains(toNode)) {
            throw new IllegalArgumentException("Edge nodes must already exist!");
        }
        Edge edge = new Edge(fromNode, toNode);
        edge.source().removeEdge(edge);
        edge.destination().removeEdge(edge);

        edge.source().addEdge(edge);
        edge.destination().addEdge(edge);
        return edge;
    }

    public abstract Node newNode(String name, NodePosition position);

    protected void addNode(Node node) {
        this.nodes.add(node);
    }

    public void removeNode(Node node) {
        nodes.remove(node);
    }

    public Set<? extends Node> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public String getNodeDisplayText(Node node) {
        return node.getName();
    }
}
