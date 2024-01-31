package fsa;

import java.util.*;
import java.util.stream.Collectors;

public class MooreAutomaton extends DeterministicFiniteAutomaton {
    private final Map<DfaState, OutputSymbol> outputMap = new HashMap<>();

    public MooreAutomaton(int id, String name, List<InputSymbol> inputAlphabet, List<OutputSymbol> outputAlphabet) {
        super(id, name, inputAlphabet, outputAlphabet);
    }

    public void setOutput(DfaState state, OutputSymbol output) {
        this.outputMap.put(state, output);
    }

    @Override
    public Edge addEdge(Node fromNode, Node toNode) {
        Set<InputSymbol> unusedInputs = getUnassignedInputs((DfaState) fromNode).collect(Collectors.toSet());
        InputSymbol input = unusedInputs.isEmpty() ? null : unusedInputs.iterator().next();
        Edge edge = fromNode.getEdgesOut().stream()
                .filter(e -> e.destination() == toNode)
                .findAny()
                .orElseGet(() -> super.addEdge(fromNode, toNode));
        addTransitionInput(edge, input);
        return edge;
    }

    @Override
    public String getEdgeDisplayText(Edge edge) {
        List<InputSymbol> transitions = ((DfaState) edge.source()).getTransitions(edge);
        if (transitions.isEmpty()) {
            return "?";
        }
        return String.join(", ", transitions.stream().map(InputSymbol::name).toList());
    }

    @Override
    public String getNodeDisplayText(Node state) {
        @SuppressWarnings("SuspiciousMethodCalls") OutputSymbol out = outputMap.get(state);
        return state.getName() + "/" + (out == null ? "-" : out.name());
    }
}
