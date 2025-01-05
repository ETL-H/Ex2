package Code;

import java.util.regex.*;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

// Define CellMath Class
class CellMath {
    private String content;

    public CellMath(String content) {
        this.content = content;
    }

    public boolean isNumber(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("-?\\d+(\\.\\d+)?");
    }

    public boolean isText(String text) {
        return !isNumber(text) && !isForm(text);
    }

    public boolean isForm(String text) {
        return text != null && text.startsWith("=") && isValidForm(text.substring(1));
    }

    private boolean isValidForm(String text) {
        int openParens = 0;
        for (char ch : text.toCharArray()) {
            if (ch == '(') openParens++;
            else if (ch == ')') openParens--;
            if (openParens < 0) return false; // Unbalanced parentheses
        }

        try {
            // Match either a valid math expression or a cell reference
            if (openParens == 0 && (text.matches("[0-9+\\-*/().A-Z]*") || text.matches("[A-Z][0-9]+"))) {
                // Optionally validate using Exp4j for math expressions
                if (!text.matches("[A-Z][0-9]+")) {
                    new ExpressionBuilder(text).build(); // Validate math expression
                }
                return true; // Valid formula or cell reference
            }
            return false;
        } catch (Exception e) {
            return false; // Invalid formula syntax
        }
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
            Expression exp = new ExpressionBuilder(expression).build();
            return exp.evaluate();
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating formula: " + form, e);
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
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                cells[i][j] = new CellMath(""); // Initialize cells
            }
        }
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
        try {
            String rowPart = c.replaceAll("[A-Z]", "");
            return Integer.parseInt(rowPart) - 1; // Convert to zero-based index
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String eval(int x, int y) {
        CellMath cell = get(x, y);
        if (cell == null) return "null"; // Return empty for null cells

        String content = cell.getContent();
        if (content.isEmpty()) {
            return "null"; // Return empty for uninitialized cells
        }
        if (cell.isNumber(content)) {
            return content; // Return the number as a string
        } else if (cell.isText(content)) {
            return content; // Return the text content directly
        } else if (cell.isForm(content)) {
            if (computeDepth(x, y, new boolean[width()][height()]) == -1) {
                return "ERR"; // Cycle detected
            }
            try {
                return String.valueOf(cell.computeForm(content, this));
            } catch (Exception e) {
                return "ERR"; // Formula evaluation error
            }
        } else {
            return ""; // Default case for unexpected content
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

    private int computeDepth(int x, int y, boolean[][] visited) {
        if (visited[x][y]) {
            return -1; // Cycle detected
        }

        CellMath cell = get(x, y);
        if (cell == null || cell.isNumber(cell.getContent()) || cell.isText(cell.getContent())) {
            return 0;
        } else if (cell.isForm(cell.getContent())) {
            visited[x][y] = true;
            String content = cell.getContent().substring(1);
            int maxDepth = 0;

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

    public void printSheet() {
        String[][] allValues = evalAll();
        for (String[] row : allValues) {
            for (String value : row) {
                System.out.print((value == null || value.isEmpty() ? " " : value) + "\t");
            }
            System.out.println();
        }
    }

}

// Example usage
public class Main {
    public static void main(String[] args) {
        Spreadsheet sheet = new Spreadsheet(9, 9);

        // Setting up cells
        sheet.set(0, 5, new CellMath("=A4")); // A6
        sheet.set(0, 3, new CellMath("=A6")); // A4
        sheet.set(0, 0, new CellMath("5"));   // A1
        sheet.set(1, 0, new CellMath("Hello")); // B1
        sheet.set(0, 1, new CellMath("=5+6*5")); // A2
        sheet.set(1, 1, new CellMath("=A1+10")); // B2
        sheet.set(2, 2, new CellMath("=B1")); // C3

        sheet.printSheet();
    }
}
