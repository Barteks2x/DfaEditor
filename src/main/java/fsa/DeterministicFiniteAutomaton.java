package fsa;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.unmodifiableList;

public abstract class DeterministicFiniteAutomaton extends Graph implements StateMachineGraph {

    private final List<InputSymbol> inputAlphabet;
    private final List<OutputSymbol> outputAlphabet;


    public DeterministicFiniteAutomaton(int id, String name, List<InputSymbol> inputAlphabet, List<OutputSymbol> outputAlphabet) {
        super(id, name);
        this.inputAlphabet = inputAlphabet;
        this.outputAlphabet = outputAlphabet;
    }


    @Override
    public Node newNode(String name, NodePosition position) {
        DfaState dfaState = new DfaState(name, position, StateType.FINAL);
        this.addNode(dfaState);
        return dfaState;
    }

    @SuppressWarnings("unchecked")
    @Override public Set<DfaState> getNodes() {
        return Collections.unmodifiableSet((Set<? extends DfaState>) super.getNodes());
    }

    @Override
    public void fillMissingEdges() {
        for (DfaState node : getNodes()) {
            Set<InputSymbol> remainingInputSymbols = new HashSet<>(getInputAlphabet());
            for (Edge edge : node.getEdgesOut()) {
                remainingInputSymbols.removeAll(node.getTransitions(edge));
            }
            for (InputSymbol remainingInputSymbol : remainingInputSymbols) {
                Edge edge = new Edge(node, node);
                node.addEdge(edge);
                node.addTransitionInput(edge, remainingInputSymbol);
            }
        }
    }

    public Optional<DfaState> getStartNode() {
        return getNodes().stream().filter(n -> n.getType().isInitial()).findAny();
    }

    public List<InputSymbol> getInputAlphabet() {
        return unmodifiableList(inputAlphabet);
    }

    public List<OutputSymbol> getOutputAlphabet() {
        return unmodifiableList(outputAlphabet);
    }

    public void addTransitionInput(Edge edge, InputSymbol input) {
        DfaState source = (DfaState) edge.source();
        source.addTransitionInput(edge, input);
    }

    public Stream<InputSymbol> getUnassignedInputs(DfaState state) {
        Set<InputSymbol> usedInputs = state.getEdgesOut().stream()
                .flatMap(e -> state.getTransitions(e).stream())
                .collect(Collectors.toSet());
        return inputAlphabet.stream().filter(e -> !usedInputs.contains(e));    }

    public abstract String getEdgeDisplayText(Edge edge);
}
