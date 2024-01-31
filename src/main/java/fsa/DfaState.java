package fsa;

import java.util.*;
import java.util.stream.Stream;

public class DfaState extends Node {

    private final Map<Edge, List<InputSymbol>> transitionInputsForOutEdges = new HashMap<>();
    private StateType type;

    public DfaState(String name, NodePosition position, StateType type) {
        super(name, position);
        this.type = type;
    }

    public StateType getType() {
        return type;
    }

    public void changeType(StateType type) {
        this.type = type;
        if (type == StateType.FINAL || type == StateType.INITIAL_FINAL) {
            transitionInputsForOutEdges.clear();
        }
    }

    public void setTransitions(Edge edge, List<InputSymbol> transitionInputs) {
        transitionInputsForOutEdges.put(edge, transitionInputs);
        updateType();
    }

    public void addTransitionInput(Edge edge, InputSymbol newValue) {
        transitionInputsForOutEdges.computeIfAbsent(edge, x -> new ArrayList<>()).add(newValue);
        updateType();
    }

    private void updateType() {
        boolean hasTransitionsToOtherNodes = transitionInputsForOutEdges.entrySet().stream()
                .filter(e -> e.getKey().destination() != this)
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .findAny().isPresent();
        if (hasTransitionsToOtherNodes) {
            this.type = this.type.toNonFinal();
        } else {
            this.type = this.type.toFinal();
        }
    }


    public List<InputSymbol> getTransitions(Edge edge) {
        return Collections.unmodifiableList(transitionInputsForOutEdges.computeIfAbsent(edge, x -> new ArrayList<>()));
    }
}
