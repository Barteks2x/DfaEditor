package fsa;

public class GenericGraph extends Graph {
    public GenericGraph(int id, String name) {
        super(id, name);
    }

    @Override
    public Node newNode(String name, NodePosition position) {
        Node node = new Node(name, position);
        addNode(node);
        return node;
    }
}
