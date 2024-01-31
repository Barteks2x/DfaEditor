package fsa;

import java.util.Date;

public class DatabaseObject {
    private final int id;
    private final Date creationDate;
    private String name;

    public DatabaseObject(int id, String name) {
        this.id = id;
        this.name = name;
        this.creationDate = new Date();
    }

    public int getId() {
        return id;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
