package fsa;

public record NodePosition(double x, double y) {

    public NodePosition add(double dx, double dy) {
        return new NodePosition(this.x() + dx, this.y() + dy);
    }
}
