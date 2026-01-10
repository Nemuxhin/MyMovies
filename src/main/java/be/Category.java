package be;

public class Category {
    private final int id;
    private String name;

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // This is important for GUI elements (like ComboBoxes)
    // to display the name instead of the object memory address
    @Override
    public String toString() {
        return name;
    }
}