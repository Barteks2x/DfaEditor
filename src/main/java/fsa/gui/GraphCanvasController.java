package fsa.gui;

import fsa.*;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;

public class GraphCanvasController {
    // przezroczystość (alpha) krawędzi która jest aktualnie zaznaczona do dodania
    private static final int ADDING_EDGE_ALPHA = 100;
    private static final Color HOVERED_COLOR = Color.RED;
    private static final Color START_NODE_COLOR = Color.GRAY;
    private final Canvas canvas;

    private Graph graph;

    private Node hoveredNode = null;
    private Edge hoveredEdge = null;

    private Node movingNode = null;
    private Edge movingEdge = null;

    private Node addingEdgeSource = null;

    double lastDraggedX = Double.NEGATIVE_INFINITY, lastDraggedY;
    double trackedMouseX, trackedMouseY;

    public GraphCanvasController(Canvas canvas) {
        this.canvas = canvas;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        this.hoveredEdge = null;
        this.hoveredNode = null;
        this.movingEdge = null;
        this.movingNode = null;
        this.addingEdgeSource = null;
        this.lastDraggedX = Double.NEGATIVE_INFINITY;
        updateGraphView();
    }

    public void updateDisplay() {
        updateGraphView();
    }

    public void mouseMoved(MouseEvent e) {
        trackedMouseX = e.getX();
        trackedMouseY = e.getY();
        updateHoverState();
    }

    public void mouseReleased(MouseEvent e) {
        onMouseReleased();
    }

    public void mouseDragged(MouseEvent e) {
        if (lastDraggedX != Double.NEGATIVE_INFINITY) {
            onMouseDragged(lastDraggedX, lastDraggedY, e.getX(), e.getY());
        }
        lastDraggedX = e.getX();
        lastDraggedY = e.getY();
    }

    public void mouseClicked(MouseEvent e) {
        if (graph == null) {
            return;
        }
        if (lastDraggedX != Double.NEGATIVE_INFINITY) {
            lastDraggedX = Double.NEGATIVE_INFINITY;
            return;
        }
        if (e.getButton() == MouseButton.PRIMARY) {
            if (addingEdgeSource != null && hoveredNode != null) {
                Node otherNode = hoveredNode;
                graph.addEdge(addingEdgeSource, otherNode);
                addingEdgeSource = null;
            } else if (hoveredNode != null &&
                    (!(graph instanceof DeterministicFiniteAutomaton fsa) || fsa.getUnassignedInputs((DfaState) hoveredNode).findAny().isPresent())) {
                addingEdgeSource = hoveredNode;
            } else if (hoveredNode == null && addingEdgeSource == null) {
                graph.newNode("S" + graph.getNodes().size(), new NodePosition(mouseToScreenX(trackedMouseX), mouseToScreenY(trackedMouseY)));
            }
            updateGraphView();
        }
    }

    public void keyReleased(KeyEvent e) {
        int dist = e.isShiftDown() ? 10 : 1;
        switch (e.getCode()) {
            case KeyCode.LEFT:
                graph.getNodes().forEach(n -> n.move(-dist, 0));
                break;
            case KeyCode.RIGHT:
                graph.getNodes().forEach(n -> n.move(dist, 0));
                break;
            case KeyCode.UP:
                graph.getNodes().forEach(n -> n.move(0, -dist));
                break;
            case KeyCode.DOWN:
                graph.getNodes().forEach(n -> n.move(0, dist));
                break;
            case KeyCode.DELETE:
            case KeyCode.BACK_SPACE:
            case KeyCode.D:
            case KeyCode.R:
                if (hoveredNode != null) {
                    graph.removeNode(hoveredNode);
                } else if (hoveredEdge != null) {
                    hoveredEdge.source().removeEdge(hoveredEdge);
                }
                break;
            case KeyCode.E:
                if (hoveredNode != null && (!(graph instanceof DeterministicFiniteAutomaton fsa) || fsa.getUnassignedInputs((DfaState) hoveredNode).findAny().isPresent())) {
                    addingEdgeSource = hoveredNode;
                }
                break;
            case KeyCode.N: {
                if (hoveredNode == null) {
                    graph.newNode("S" + graph.getNodes().size(), new NodePosition(mouseToScreenX(trackedMouseX), mouseToScreenY(trackedMouseY)));
                    updateHoverState();
                }
                break;
            }
            case KeyCode.S:
                if (hoveredNode != null && graph instanceof DeterministicFiniteAutomaton fsa) {
                    fsa.getNodes().forEach(n -> n.changeType(n.getType().toNonInitial()));
                    ((DfaState) hoveredNode).changeType(((DfaState) hoveredNode).getType().toInitial());
                }
                break;
            case KeyCode.G:
                if (hoveredNode != null) {
                    int dx = Math.floorMod((int) Math.floor(hoveredNode.getPosition().x()), 20);
                    int dy = Math.floorMod((int) Math.floor(hoveredNode.getPosition().y()), 20);
                    hoveredNode.move(-dx, -dy);
                }
                break;
        }
        updateGraphView();
    }

    private void updateHoverState() {
        Node newHovered = findNode(mouseToScreenX(trackedMouseX), mouseToScreenY(trackedMouseY)).orElse(null);
        if (newHovered != hoveredNode) {
            hoveredNode = newHovered;
            hoveredEdge = null;
            updateGraphView();
        }
        if (hoveredNode == null) {
            Edge edge = findEdge(mouseToScreenX(trackedMouseX), mouseToScreenY(trackedMouseY)).orElse(null);
            if (hoveredEdge != edge) {
                hoveredEdge = edge;
                updateGraphView();
            }
        }
    }

    private Optional<? extends Node> findNode(double x, double y) {
        return graph.getNodes().stream().filter(n -> intersects(n, x, y)).findAny();
    }

    private boolean intersects(Node node, double x, double y) {
        double dx = x - node.getPosition().x();
        double dy = y - node.getPosition().y();
        double r = 20;
        return dx * dx + dy * dy <= r * r;
    }

    private Optional<Edge> findEdge(double x, double y) {
        return graph.getNodes().stream()
                .flatMap(n -> n.getEdgesOut().stream())
                .filter(e -> intersectsEdge(e, x, y))
                .findAny();
    }

    private boolean intersectsEdge(Edge edge, double x, double y) {
        Set<Edge> allSrcEdges = gatherAllEdgesFor(edge.source());
        if (edge.source() != edge.destination()) {
            Line line = computeLine(edge, allSrcEdges);
            if (line.intersects(x - 1.5, y - 1.5, 3, 3)) {
                return true;
            }
            NodePosition srcPos = edge.source().getPosition();
            NodePosition dstPos = edge.destination().getPosition();
            double dx = dstPos.x() - srcPos.x();
            double dy = dstPos.y() - srcPos.y();
            double lengthInv = 1.0 / sqrt(dx * dx + dy * dy);
            double nx = -dy * lengthInv;
            double ny = dx * lengthInv;
            Rectangle r = getStringBoundingBox(getDisplayText(edge));
            Point2D pos = computeLineTextPosition(line, r, nx, ny);
            return r.intersects(x - pos.getX() - 3, y - pos.getY() - 3, 6, 6);
        } else {
            Affine ellipseTransform = new Affine();
            Affine arrowTransform = new Affine();
            Ellipse ellipse = new Ellipse();
            computeEllipse(edge, ellipse, ellipseTransform, arrowTransform, allSrcEdges);

            Point2D pt;
            try {
                pt = ellipseTransform.inverseTransform(x, y);
            } catch (NonInvertibleTransformException ex) {
                throw new AssertionError(ex);
            }

            if (ellipse.intersects(pt.getX() - 1, pt.getY() - 1, 2, 2)) {
                return true;
            }

            Rectangle r = getStringBoundingBox(getDisplayText(edge));
            Point2D pos = computeEllipseTextPosition(edge, r, ellipseTransform);
            return r.intersects(x - pos.getX() - 3, y - pos.getY() - 3, 6, 6);
        }
    }

    private static final double RADIUS = 20;

    private Line computeLine(Edge edge, Set<Edge> otherSrcEdges) {
        NodePosition dstPos = edge.destination().getPosition();
        NodePosition srcPos = edge.source().getPosition();
        double dx = dstPos.x() - srcPos.x();
        double dy = dstPos.y() - srcPos.y();
        double lengthInv = 1.0 / sqrt(dx * dx + dy * dy);

        dx *= lengthInv;
        dy *= lengthInv;
        // kierunek prostpoadły - do przesunięcia linii (w przypadku kiedy istnieje połączenie w obu kierunkach)
        boolean needsOffset = otherSrcEdges.stream().anyMatch(e -> e.source() == edge.destination() && e.destination() == edge.source());
        double nx = -dy;
        double ny = dx;
        dx *= RADIUS;
        dy *= RADIUS;

        double srcX = srcPos.x() + dx + (needsOffset ? nx * 3 : 0);
        double srcY = srcPos.y() + dy + (needsOffset ? ny * 3 : 0);
        double destX = dstPos.x() - dx + (needsOffset ? nx * 3 : 0);
        double destY = dstPos.y() - dy + (needsOffset ? ny * 3 : 0);
        return new Line(srcX, srcY, destX, destY);
    }

    private void drawEdge(Edge edge, GraphicsContext g, Set<Edge> allSrcEdges) {
        if (edge.source() != edge.destination()) {
            drawStraightLine(edge, g, allSrcEdges);
        } else {
            drawLoop(edge, g, allSrcEdges);
        }
    }

    private void drawStraightLine(Edge edge, GraphicsContext g, Set<Edge> otherSrcEdges) {
        Line line = computeLine(edge, otherSrcEdges);
        g.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());

        NodePosition dstPos = edge.destination().getPosition();
        NodePosition srcPos = edge.source().getPosition();

        double dx = dstPos.x() - srcPos.x();
        double dy = dstPos.y() - srcPos.y();

        double lengthInv = 1.0 / sqrt(dx * dx + dy * dy);
        double nx = -dy * lengthInv;
        double ny = dx * lengthInv;

        double angle = Math.atan2(-dx, -dy);
        double a1 = angle - Math.PI / 7;
        double a2 = angle + Math.PI / 7;

        double size = 7;
        // strazłka
        g.strokeLine(line.getEndX(), line.getEndY(), line.getEndX() + Math.sin(a1) * size, line.getEndY() + Math.cos(a1) * size);
        g.strokeLine(line.getEndX(), line.getEndY(), line.getEndX() + Math.sin(a2) * size, line.getEndY() + Math.cos(a2) * size);

        String txt = getDisplayText(edge);
        Rectangle r = getStringBoundingBox(txt);
        Point2D translate = computeLineTextPosition(line, r, nx, ny);
        g.translate(translate.getX(), translate.getY());
        g.strokeText(txt, 0, 0);
        g.translate(-translate.getX(), -translate.getY());
    }

    private String getDisplayText(Edge edge) {
        if (graph instanceof DeterministicFiniteAutomaton fsa) {
            return fsa.getEdgeDisplayText(edge);
        }
        return "";
    }

    private String getDisplayText(Node state) {
        return graph.getNodeDisplayText(state);
    }

    private Rectangle getStringBoundingBox(String txt) {
        Text text = new Text(txt);
        return new Rectangle(0, 0, text.getBoundsInLocal().getWidth(), text.getBoundsInLocal().getHeight());
    }

    private Point2D computeLineTextPosition(Line line, Rectangle txtRect, double normalX, double normalY) {
        return new Point2D(
                (float) ((line.getStartX() + line.getEndX()) * 0.5 + normalX * (txtRect.getWidth() / 2 + 4)),
                (float) ((line.getStartY() + line.getEndY()) * 0.5 + normalY * (txtRect.getHeight() / 2 + 4))
        );
    }

    private void drawLoop(Edge edge, GraphicsContext g, Set<Edge> allEdges) {
        Affine oldTransform = g.getTransform();

        Affine newTransform = new Affine();
        Affine arrowTransform = new Affine();
        Ellipse ellipse = new Ellipse();

        computeEllipse(edge, ellipse, newTransform, arrowTransform, allEdges);

        g.transform(newTransform);
        g.strokeOval(ellipse.getCenterX(), ellipse.getCenterY(), ellipse.getRadiusX(), ellipse.getRadiusY());

        g.transform(arrowTransform);
        g.strokeLine(0, 0, 3.5, 6);
        g.strokeLine(0, 0, -3.5, 6);
        g.setTransform(oldTransform);


        Rectangle r = getStringBoundingBox(getDisplayText(edge));
        Point2D textTransform = computeEllipseTextPosition(edge, r, newTransform);
        g.translate(textTransform.getX(), textTransform.getY());
        g.strokeText(getDisplayText(edge), 0, 0);
        g.setTransform(oldTransform);
    }

    private Point2D computeEllipseTextPosition(Edge edge, Rectangle r, Affine ellipseTransform) {
        Point2D txtPos = ellipseTransform.transform(0, RADIUS * 3 + 3);

        double normalX = (txtPos.getX() - edge.source().getPosition().x());
        double normalY = (txtPos.getY() - edge.source().getPosition().y());
        double lenInv = 1. / Math.sqrt(normalX * normalX + normalY * normalY);
        normalX *= lenInv * r.getWidth() * 0.5;
        normalY *= lenInv * r.getHeight() * 0.5;

        return new Point2D((float) (txtPos.getX() + normalX), (float) (txtPos.getY() + normalY));
    }

    private void computeEllipse(Edge edge, Ellipse ellipse, Affine ellipseTransform, Affine arrowTransform, Set<Edge> allEdges) {
        // wyznaczenie pozycji "pętli" - idea algorytmu to znalezienie gdzie jest najwięcej miejsca,
        // i dopasowanie parametrów elipsy do rozmiarów dostępnego miejsca

        // krok 1. wyznaczenie kątów wszystkich innych krawędzi wchodzących i wychodzących, i posortowanie
        double[] edges = allEdges.stream()
                .filter(e -> e.source() != e.destination())
                .map(e -> new Edge(edge.source(), e.source() == edge.source() ? e.destination() : e.source()))
                .mapToDouble(e -> atan2(
                        -(e.destination().getPosition().x() - e.source().getPosition().x()),
                        e.destination().getPosition().y() - e.source().getPosition().y()
                ))
                .sorted()
                .toArray();
        double drawAngle;
        double angleSpread;
        if (edges.length == 0) {
            // jeśli nie ma innych krawędzi - można wybrać dowolny kąt
            drawAngle = 0;
            angleSpread = PI / 2;
        } else if (edges.length == 1) {
            // jeśli jest tylko jedna inna krawędż - najlepszy jest kąt po przeciwnej stronie (+pi)
            drawAngle = edges[0] + PI;
            angleSpread = PI / 2;
        } else {
            // jeśli jest więcej krawędzi - szukanie największej "przerwy" pomiędzi posortowanymi kątami
            // z uwzględnieniem "zapętelenia" po pełnym obrocie
            double maxAngleDiff = Double.NEGATIVE_INFINITY;
            int maxDiffIdx = -1;
            for (int i = 0; i < edges.length; i++) {
                double angle = edges[i];
                double nextAngle = edges[(i + 1) % edges.length];
                // + 2*PI dlatego, że -1 % (Math.PI*2) == -1 a nie Math.PI*2 - 1
                double delta = (nextAngle - angle + 2 * PI) % (PI * 2);
                assert delta >= 0;
                if (delta > maxAngleDiff) {
                    maxAngleDiff = delta;
                    maxDiffIdx = i;
                }
            }
            // ta sytuacja może wystąpić, gdy jedyne inne krawędzie łączą się z węzłem w tym samym kierunku
            if (maxAngleDiff == 0) {
                maxAngleDiff = Math.PI * 2;
            }
            assert maxDiffIdx >= 0;
            // pierwszy kąt z pary najbardziej oddalonych od siebie krawędzi
            double angle1 = (edges[maxDiffIdx] + 2 * PI) % (2 * PI);
            // drugi kąt to angle1+maxAngleDiff
            // petla jest rysowana po srodku - średnia arytmetyczna
            drawAngle = (angle1 + (angle1 + maxAngleDiff)) * 0.5;
            // rozmiar dostępnego miejsca - maksymalnie 90 stopni, lub 90% miejsca dostępnego pomiędzy krawędziami
            angleSpread = Math.min(PI / 2, maxAngleDiff * 0.9);
        }

        // dostępna szerokość po uwzględnieniu dostępnego miejsca
        double width = RADIUS * Math.sin(angleSpread / 2);

        double k = RADIUS / width;
        double r = RADIUS;
        // Jedno z rozwiązań układu równań, które opisuje 2 elipsy (węzeł i "pętla")
        //   (kx)^2 + (y/2 - r)*2 = r^2
        //   x^2 + y^2 = r^2
        double v = sqrt((4 * (k * k) * (k * k) - k * k + 1) * (r * r) * (r * r));
        double x = sqrt(-4 * (k * k) * (r * r) - 7 * (r * r) + 8 * v)
                / abs(4 * (k * k) - 1);
        double y = -2 * (r * r - v) / ((4 * (k * k) - 1) * r);

        ellipseTransform.setToIdentity();
        ellipseTransform.appendTranslation(edge.source().getPosition().x(), edge.source().getPosition().y());
        ellipseTransform.appendRotation(toDegrees(drawAngle));

        ellipse.setCenterX(ellipse.getCenterX() + -width + 1);
        ellipse.setRadiusX(width * 2 - 2);
        ellipse.setRadiusY(RADIUS * 3);

        arrowTransform.setToIdentity();
        arrowTransform.appendTranslation(x, y);
        arrowTransform.appendRotation(-15);
    }

    private void onMouseReleased() {
        movingEdge = null;
        movingNode = null;
    }

    private void onMouseDragged(double lastX, double lastY, double x, double y) {
        Node node = movingNode != null ? movingNode : hoveredNode;
        if (node != null && movingEdge == null) {
            movingNode = node;
            node.move(x - lastX, y - lastY);
            updateGraphView();
        } else {
            movingNode = null;
            Edge edge = movingEdge != null ? movingEdge : hoveredEdge;
            if (edge != null) {
                movingEdge = edge;
                edge.source().move(x - lastX, y - lastY);
                if (edge.source() != edge.destination()) {
                    edge.destination().move(x - lastX, y - lastY);
                }
                updateGraphView();
            }
        }

        if (movingEdge == null && movingNode == null) {
            graph.getNodes().forEach(n -> n.move(x - lastX, y - lastY));
            updateGraphView();
        }
    }

    private double mouseToScreenX(double x) {
        return x - canvas.getWidth() * 0.5;
    }

    private double mouseToScreenY(double y) {
        return y - canvas.getHeight() * 0.5;
    }

    private void updateGraphView() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.setStroke(Color.BLACK);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);

        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (graph == null) {
            return;
        }
        double dx = canvas.getWidth() * 0.5;
        double dy = canvas.getHeight() * 0.5;

        g.translate(dx, dy);
        drawEdges(g);
        drawToBeAddedEdge(g);
        drawNodes(g);
        g.translate(-dx, -dy);
    }

    private void drawEdges(GraphicsContext g) {
        Node addingEdgeDest = hoveredNode == null ? addingEdgeSource : hoveredNode;
        Paint defaultColor = g.getStroke();
        graph.getNodes().forEach(n ->
                n.getEdgesOut().forEach(e -> {
                    // krawędź, która jest taka sama jak ta aktualnie zaznaczona do dodanie jest pominięta
                    // aby uniknąć nakładanie się dwóch tylko nieznacznie różniących się krawędzi
                    if (e.source() != addingEdgeSource || e.destination() != addingEdgeDest) {
                        g.setStroke((e == hoveredEdge || e == movingEdge) ? HOVERED_COLOR : defaultColor);
                        Set<Edge> allEdges = new HashSet<>(e.source().getEdgesOut());
                        allEdges.addAll(e.source().getEdgesIn());
                        drawEdge(e, g, allEdges);
                    }
                })
        );
        g.setStroke(defaultColor);
    }

    private void drawToBeAddedEdge(GraphicsContext g) {
        if (addingEdgeSource != null) {
            Node otherNode = hoveredNode == null ? addingEdgeSource : hoveredNode;
            Edge edge = new Edge(addingEdgeSource, otherNode);
            Set<Edge> allEdgesForSrc = gatherAllEdgesFor(addingEdgeSource);
            Color oldColor = ((Color) g.getStroke());
            g.setStroke(Color.color(oldColor.getRed(), oldColor.getGreen(), oldColor.getBlue(), ADDING_EDGE_ALPHA * (1 / 255.0)));
            drawEdge(edge, g, allEdgesForSrc);
            g.setStroke(oldColor);
        }
    }

    private Set<Edge> gatherAllEdgesFor(Node state) {
        Stream<Edge> outgoingEdges = state.getEdgesOut().stream();
        Stream<Edge> incomingEdges = state.getEdgesIn().stream();
        return Stream.concat(incomingEdges, outgoingEdges).collect(Collectors.toSet());
    }

    private void drawNodes(GraphicsContext g) {
        Color defaultColor = (Color) g.getStroke();
        graph.getNodes().forEach(n -> {
            Node startNode = graph.getStartNode().orElse(null);
            g.setStroke((n == hoveredNode || n == movingNode) ?
                    HOVERED_COLOR :
                    n == startNode ? START_NODE_COLOR : defaultColor);

            drawNode(n, g);
        });
        g.setStroke(defaultColor);
    }

    private void drawNode(Node n, GraphicsContext g) {
        NodePosition pos = n.getPosition();

        g.fillOval(pos.x() - RADIUS, pos.y() - RADIUS, RADIUS * 2, RADIUS * 2);
        g.strokeOval(pos.x() - RADIUS + 1, pos.y() - RADIUS + 1, RADIUS * 2 - 2, RADIUS * 2 - 2);

        String txt = getDisplayText(n);
        g.strokeText(txt, n.getPosition().x(), n.getPosition().y());
    }
}
