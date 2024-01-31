package fsa;

public enum StateType {
    INITIAL("Stan początkowy"),
    INITIAL_FINAL("Stan początkowy i końcowy"),
    INTERMEDIATE("Stan pośredni"),
    FINAL("Stan końcowy");

    private final String displayName;

    StateType(String displayName) {
        this.displayName = displayName;
    }

    public StateType toFinal() {
        return switch (this) {
            case INITIAL, INITIAL_FINAL -> INITIAL_FINAL;
            case INTERMEDIATE, FINAL -> FINAL;
        };
    }

    public StateType toNonFinal() {
        return switch (this) {
            case INITIAL, INITIAL_FINAL -> INITIAL;
            case INTERMEDIATE, FINAL -> INTERMEDIATE;
        };
    }

    public StateType toInitial() {
        return switch (this) {
            case INITIAL, INTERMEDIATE -> INITIAL;
            case INITIAL_FINAL, FINAL -> INITIAL_FINAL;
        };
    }

    public StateType toNonInitial() {
        return switch (this) {
            case INITIAL, INTERMEDIATE -> INTERMEDIATE;
            case INITIAL_FINAL, FINAL -> FINAL;
        };
    }

    public boolean isInitial() {
        return this == INITIAL || this == INITIAL_FINAL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
