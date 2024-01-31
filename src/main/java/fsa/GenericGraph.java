package fsa;

import java.util.Optional;

public class GenericGraph extends Graph {
    private Node firstNode;
    public GenericGraph(int id, String name) {
        super(id, name);
    }

    @Override public Optional<Node> getStartNode() {
        return Optional.ofNullable(firstNode);
    }

    @Override
    public Node newNode(String name, NodePosition position) {
        Node node = new Node(name, position);
        if (firstNode == null) {
            firstNode = node;
        }
        addNode(node);
        return node;
    }
}
