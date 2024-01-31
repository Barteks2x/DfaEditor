package fsa;

import java.util.*;
import java.util.stream.Collectors;

public class MealyAutomaton extends DeterministicFiniteAutomaton {

    private final Map<Edge, Map<InputSymbol, OutputSymbol>> outputMap = new HashMap<>();

    public MealyAutomaton(int id, String name, List<InputSymbol> inputAlphabet, List<OutputSymbol> outputAlphabet) {
        super(id, name, inputAlphabet, outputAlphabet);
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

    public void setOutput(Edge edge, InputSymbol input, OutputSymbol output) {
        Map<InputSymbol, OutputSymbol> inToOutMapping = outputMap.computeIfAbsent(edge, x -> new HashMap<>());
        inToOutMapping.put(input, output);
    }

    @Override
    public void addTransitionInput(Edge edge, InputSymbol input) {
        super.addTransitionInput(edge, input);
        Map<InputSymbol, OutputSymbol> inToOutMapping = outputMap.computeIfAbsent(edge, x -> new HashMap<>());
        inToOutMapping.put(input, getOutputAlphabet().get(0));
    }

    @Override
    public String getEdgeDisplayText(Edge edge) {
        List<InputSymbol> transitions = ((DfaState) edge.source()).getTransitions(edge);
        if (transitions.isEmpty()) {
            return "-/-";
        }
        Map<InputSymbol, OutputSymbol> inToOutMapping = outputMap.get(edge);

        return transitions.stream()
                .map(in -> in.name() + "/" + inToOutMapping.get(in).name())
                .collect(Collectors.joining("\n"));
    }
}
