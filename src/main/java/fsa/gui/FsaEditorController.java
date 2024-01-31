package fsa.gui;

import fsa.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


public class FsaEditorController {
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();

    @FXML private Canvas graphCanvas;
    @FXML private AnchorPane canvasAnchor;
    @FXML private TableView<DatabaseObject> graphList;
    @FXML private TableColumn<DatabaseObject, Integer> idColumn;
    @FXML private TableColumn<DatabaseObject, String> nameColumn;
    @FXML private TableColumn<DatabaseObject, Date> creationDateColumn;
    @FXML private Button removeGraphBtn;
    @FXML private TextField searchTextField;
    @FXML private ChoiceBox<String> typeFilterChoice;
    @FXML private ChoiceBox<OutputSymbol> outSymbol;
    @FXML private TextField graphTypeField;
    @FXML private TextField graphNameField;
    @FXML private TextField inputAlphabetField;
    @FXML private TextField outputAlphabetField;

    private final ObservableList<DatabaseObject> visibleDatabaseObjects = FXCollections.observableArrayList();
    private final List<DatabaseObject> allDatabaseObjects = new ArrayList<>();

    private GraphCanvasController graphCanvasController;

    public void initialize() {
        graphCanvas.widthProperty().bind(canvasAnchor.widthProperty());
        graphCanvas.heightProperty().bind(canvasAnchor.heightProperty());

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        creationDateColumn.setCellValueFactory(new PropertyValueFactory<>("creationDate"));

        graphList.setItems(visibleDatabaseObjects);
        graphCanvas.requestFocus();

        graphCanvasController = new GraphCanvasController(graphCanvas);

        graphList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> updateGraphForDisplay(newSelection));

        typeFilterChoice.setItems(FXCollections.observableArrayList("Wyświetl wszystkie rodzaje", "Mealy", "Moore", "Inne"));
        typeFilterChoice.getSelectionModel().select(0);
        typeFilterChoice.getSelectionModel().selectedItemProperty().addListener(val -> updateVisibleDatabaseObjects());
    }

    private void updateGraphForDisplay(DatabaseObject newSelection) {
        graphCanvasController.setGraph((Graph) newSelection);
        removeGraphBtn.setDisable(newSelection == null);
        if (newSelection instanceof DeterministicFiniteAutomaton fsa) {
            outSymbol.setItems(FXCollections.observableArrayList(fsa.getOutputAlphabet()));
            outSymbol.getSelectionModel().select(0);
        } else {
            outSymbol.setItems(FXCollections.observableArrayList());
        }
        DatabaseObject selected = graphList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        graphTypeField.setText(selected instanceof GenericGraph ? "Inny graf" :
                selected instanceof MealyAutomaton ? "Mealy" : "Moore");
                graphNameField.setText(selected.getName());
        String inputAlphabetString = selected instanceof DeterministicFiniteAutomaton fsa ?
                fsa.getInputAlphabet().stream().map(Object::toString).collect(Collectors.joining(", ")) :
                "";
        String outputAlphabetString = selected instanceof DeterministicFiniteAutomaton fsa ?
                fsa.getOutputAlphabet().stream().map(Object::toString).collect(Collectors.joining(", ")) :
                "";
        inputAlphabetField.setText(inputAlphabetString);
        outputAlphabetField.setText(outputAlphabetString);
    }

    @FXML private void onCanvasClick(MouseEvent event) {
        graphCanvasController.mouseClicked(event);
    }

    @FXML private void onCanvasKeyReleased(KeyEvent keyEvent) {
        if (graphCanvas.isHover()) {
            graphCanvasController.keyReleased(keyEvent);
        }
    }

    @FXML private void onCanvasMouseMoved(MouseEvent mouseEvent) {
        graphCanvasController.mouseMoved(mouseEvent);
    }

    @FXML private void onCanvasMouseDragged(MouseEvent mouseEvent) {
        graphCanvasController.mouseDragged(mouseEvent);
    }

    @FXML private void onCanvasMouseReleased(MouseEvent mouseEvent) {
        graphCanvasController.mouseReleased(mouseEvent);
    }

    @FXML private void onFillMissingEdges(ActionEvent actionEvent) {
        for (DatabaseObject obj : visibleDatabaseObjects) {
            if (obj instanceof StateMachineGraph stateMachine) {
                stateMachine.fillMissingEdges();
            }
        }
        graphCanvasController.updateDisplay();
    }

    @FXML private void onAddGraph(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nazwa grafu");
        dialog.setHeaderText("Podaj nazwę grafu");
        dialog.setContentText("Nazwa grafu:");

        Optional<String> name = dialog.showAndWait();
        if (name.isEmpty() || name.get().trim().isEmpty()){
            return;
        }

        List<String> choices = new ArrayList<>();
        choices.add("Moore");
        choices.add("Mealy");
        choices.add("Inny graf");

        ChoiceDialog<String> choice = new ChoiceDialog<>("Moore", choices);
        choice.setTitle("Rodzaj grafu");
        choice.setHeaderText("Wybierz rodzaj grafu");
        choice.setContentText("Rodzaj grafu:");

        Optional<String> graphType = choice.showAndWait();
        if (graphType.isEmpty()){
            return;
        }
        Optional<String> inputAlphabet = Optional.empty();
        Optional<String> outputAlphabet = Optional.empty();
        if (!graphType.get().equals("Inny graf")) {
            dialog = new TextInputDialog(InputSymbol.getDefaultAlphabet().stream().map(InputSymbol::name).collect(Collectors.joining(", ")));
            dialog.setTitle("Alfabet wejściowy");
            dialog.setHeaderText("Podaj alfabet wejściowy");
            dialog.setContentText("Alfabet wejściowy:");

            inputAlphabet = dialog.showAndWait();
            if (inputAlphabet.isEmpty() || inputAlphabet.get().trim().isEmpty()) {
                return;
            }

            dialog = new TextInputDialog(OutputSymbol.getDefaultAlphabet().stream().map(OutputSymbol::name).collect(Collectors.joining(", ")));
            dialog.setTitle("Alfabet wyjściowy");
            dialog.setHeaderText("Podaj alfabet wyjściowy");
            dialog.setContentText("Alfabet wyjściowy:");

            outputAlphabet = dialog.showAndWait();
            if (outputAlphabet.isEmpty() || outputAlphabet.get().trim().isEmpty()) {
                return;
            }
        }

        String nameTxt = name.get();
        List<InputSymbol> inputs = inputAlphabet.map(in -> InputSymbol.createInputAlphabet(in.split("\s*,\s*"))).orElseGet(ArrayList::new);
        List<OutputSymbol> outputs = outputAlphabet.map(out -> OutputSymbol.createOutputAlphabet(out.split("\s*,\s*"))).orElseGet(ArrayList::new);
        switch (graphType.get()) {
            case "Moore":
                allDatabaseObjects.add(new MooreAutomaton(ID_COUNTER.getAndIncrement(), nameTxt, inputs, outputs));
                break;
            case "Mealy":
                allDatabaseObjects.add(new MealyAutomaton(ID_COUNTER.getAndIncrement(), nameTxt, inputs, outputs));
                break;
            case "Inny graf":
                allDatabaseObjects.add(new GenericGraph(ID_COUNTER.getAndIncrement(), nameTxt));
                break;
        }
        updateVisibleDatabaseObjects();
    }

    @FXML private void onRemoveGraph(ActionEvent actionEvent) {
        int selectedIndex = graphList.getSelectionModel().getSelectedIndex();
        DatabaseObject removed = visibleDatabaseObjects.remove(selectedIndex);
        if (removed != null) {
            allDatabaseObjects.remove(removed);
        }
    }

    @FXML private void onSearchFieldAction(ActionEvent actionEvent) {
        updateVisibleDatabaseObjects();
    }

    @FXML private void onGraphRename(ActionEvent event) {
        DatabaseObject selected = graphList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setName(graphNameField.getText());
        }
        graphList.refresh();
    }

    private void updateVisibleDatabaseObjects() {
        ArrayList<DatabaseObject> prevSelected = new ArrayList<>(graphList.getSelectionModel().getSelectedItems());
        visibleDatabaseObjects.clear();
        for (DatabaseObject obj : allDatabaseObjects) {
            if (obj.getName().contains(searchTextField.getCharacters()) && getSelectedTypeFilter().isInstance(obj)) {
                visibleDatabaseObjects.add(obj);
                if (prevSelected.contains(obj)) {
                    graphList.getSelectionModel().select(obj);
                }
            }
        }
    }

    private Class<?> getSelectedTypeFilter() {
        String selectedItem = typeFilterChoice.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            return Object.class;
        }
        return switch (selectedItem) {
            case "Wyświetl wszystkie rodzaje" -> Object.class;
            case "Mealy" -> MealyAutomaton.class;
            case "Moore" -> MooreAutomaton.class;
            case "Inne" -> GenericGraph.class;
            default -> Object.class;
        };
    }
}
