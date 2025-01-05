package Code;

import java.util.regex.*;

// Define CellMath Class
class CellMath {
    private String content;

    public CellMath(String content) {
        this.content = content;
    }

    public boolean isNumber(String text) {
        return text.matches("-?\\d+(\\.\\d+)?");
    }

    public boolean isText(String text) {
        return !isNumber(text) && !isForm(text);
    }

    public boolean isForm(String text) {
        return text.startsWith("=") && isValidForm(text.substring(1));
    }

    private boolean isValidForm(String text) {
        // Validate formula structure (e.g., balanced parentheses, valid characters)
        int openParens = 0;
        for (char ch : text.toCharArray()) {
            if (ch == '(') openParens++;
            else if (ch == ')') openParens--;
            if (openParens < 0) return false; // Unbalanced parentheses
        }
        return openParens == 0 && text.matches("[0-9+\\-*/().A-Z]*"); // Valid characters
    }

    public Double computeForm(String form, Spreadsheet sheet) {
        if (!isForm(form)) {
            throw new IllegalArgumentException("Invalid formula");
        }
        String expression = form.substring(1); // Remove '='

        // Replace cell references with evaluated values
        Matcher matcher = Pattern.compile("[A-Z][0-9]+").matcher(expression);
        while (matcher.find()) {
            String ref = matcher.group();
            int refX = sheet.xCell(ref);
            int refY = sheet.yCell(ref);
            String refValue = sheet.eval(refX, refY);

            if (refValue.equals("ERR")) {
                throw new RuntimeException("Error in referenced cell");
            }
            expression = expression.replace(ref, refValue);
        }

        try {
            javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager().getEngineByName("JavaScript");
            return ((Number) engine.eval(expression)).doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating formula");
        }
    }


    public String getContent() {
        return content;
    }
}

// Define Spreadsheet Class
class Spreadsheet {
    private CellMath[][] cells;

    public Spreadsheet(int x, int y) {
        cells = new CellMath[x][y];
    }

    public CellMath get(int x, int y) {
        return cells[x][y];
    }

    public void set(int x, int y, CellMath c) {
        cells[x][y] = c;
    }

    public int width() {
        return cells.length;
    }

    public int height() {
        return cells[0].length;
    }

    public int xCell(String c) {
        char colChar = c.charAt(0);
        if (colChar >= 'A' && colChar <= 'Z') {
            return colChar - 'A';
        } else {
            return -1;
        }
    }

    public int yCell(String c) {
        String rowPart = c.replaceAll("[A-Z]", "");
        return Integer.parseInt(rowPart);
    }

    public String eval(int x, int y) {
        CellMath cell = get(x, y);
        if (cell == null) return "";

        String content = cell.getContent();
        if (cell.isNumber(content)) {
            return content;
        } else if (cell.isText(content)) {
            return "ERR";
        } else if (cell.isForm(content)) {
            if (computeDepth(x, y, new boolean[width()][height()]) == -1) {
                return "ERR"; // Cycle detected
            }
            try {
                return String.valueOf(cell.computeForm(content, this));
            } catch (Exception e) {
                return "ERR";
            }
        } else {
            return "";
        }
    }

    public String[][] evalAll() {
        int w = width();
        int h = height();
        String[][] result = new String[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                result[i][j] = eval(i, j);
            }
        }
        return result;
    }

    public int[][] depth() {
        int w = width();
        int h = height();
        int[][] result = new int[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                result[i][j] = computeDepth(i, j, new boolean[w][h]);
            }
        }
        return result;
    }

    private int computeDepth(int x, int y, boolean[][] visited) {
        if (visited[x][y]) {
            return -1; // Cycle detected
        }

        CellMath cell = get(x, y);
        if (cell == null || cell.isNumber(cell.getContent()) || cell.isText(cell.getContent())) {
            return 0;
        } else if (cell.isForm(cell.getContent())) {
            visited[x][y] = true;
            String content = cell.getContent().substring(1); // Extract formula
            int maxDepth = 0;

            // Simple dependency parsing: assume dependencies are cell references (e.g., A3)
            Matcher matcher = Pattern.compile("[A-Z][0-9]+").matcher(content);
            while (matcher.find()) {
                String ref = matcher.group();
                int refX = xCell(ref);
                int refY = yCell(ref);
                maxDepth = Math.max(maxDepth, computeDepth(refX, refY, visited));
            }

            visited[x][y] = false;
            if (maxDepth == -1) return -1; // Cycle propagated
            return 1 + maxDepth;
        } else {
            return 0;
        }
    }
}

// Example usage
public class Main {
    public static void main(String[] args) {
        Spreadsheet sheet = new Spreadsheet(9, 17);

        // Setting up cells
        sheet.set(0, 6, new CellMath("=A3")); // A6
        sheet.set(0, 3, new CellMath("=A6")); // A3

        // Test examples
        sheet.set(0, 0, new CellMath("5")); // A0
        sheet.set(1, 0, new CellMath("Hello")); // B0
        sheet.set(0, 1, new CellMath("=5+6*5")); // A1
        sheet.set(1, 1, new CellMath("=A0+10")); // B1
        sheet.set(2, 2, new CellMath("=B0")); // C2 (invalid reference to text cell)

        System.out.println("Eval (A6): " + sheet.eval(0, 6)); // Cycle error
        System.out.println("Eval (A3): " + sheet.eval(0, 3)); // Cycle error
        System.out.println("Eval (A0): " + sheet.eval(0, 0)); // "5"
        System.out.println("Eval (B0): " + sheet.eval(1, 0)); // "ERR"
        System.out.println("Eval (A1): " + sheet.eval(0, 1)); // "35.0"
        System.out.println("Eval (B1): " + sheet.eval(1, 1)); // "15.0"
        System.out.println("Eval (C2): " + sheet.eval(2, 2)); // "ERR"

        String[][] allValues = sheet.evalAll();
        for (String[] row : allValues) {
            for (String value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }

        int[][] depths = sheet.depth();
        for (int[] row : depths) {
            for (int value : row) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }
}
