package Code;

public class CellEntry implements Index2D {
    private int x; // X-coordinate
    private int y; // Y-coordinate

    // Constructor
    public CellEntry(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean isValid() {
        return x >= 0 && y >= 0 && y <= 16;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        // Convert the x-coordinate to a letter (A-Z) and combine it with y-coordinate
        char column = (char) ('A' + x);
        return column + Integer.toString(y);
    }
}
