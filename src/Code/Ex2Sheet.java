package Code;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Ex2Sheet implements Sheet {
    private SCell[][] table;
    private Map<String, Set<String>> dependencies = new HashMap<>();

    private String cellKey(int x, int y) {
        return x + "," + y;
    }


    public Ex2Sheet(int x, int y) {
        table = new SCell[x][y];
        for (int i = 0; i < x; i++) {
            for (int j = 0; j < y; j++) {
                table[i][j] = new SCell("");
            }
        }
        eval();
    }

    public Ex2Sheet() {
        this(Ex2Utils.WIDTH, Ex2Utils.HEIGHT);
    }

    @Override
    public String value(int x, int y) {
        if (isIn(x, y)) {
            String cellValue = eval(x, y);
            // Check if the value is explicitly set to "0.0"
            if (table[x][y].getType() == Ex2Utils.ERR_FORM_FORMAT && !cellValue.equals("-2.0")){
                table[x][y].setType(Ex2Utils.ERR_FORM_FORMAT);
                return cellValue;
            }
            if (table[x][y].getType() == Ex2Utils.ERR_FORM_FORMAT){

                return Ex2Utils.ERR_FORM;
            }
            if (table[x][y].getType() == Ex2Utils.ERR_CYCLE_FORM){
                return Ex2Utils.ERR_CYCLE;
            }
            if ("0.0".equals(cellValue)) {
                return "0.0";  // Explicitly show "0.0" if it's an explicit value
            }
            // If the evaluated value is an empty string, return an empty value
            if (cellValue.isEmpty()) {
                return "";  // Return empty string for empty cells
            }

            return cellValue;  // Return the evaluated value
        }
        return Ex2Utils.EMPTY_CELL;
    }



    @Override
    public SCell get(int x, int y) {
        return isIn(x, y) ? table[x][y] : null;
    }

    @Override
    public SCell get(String cords) {
        int[] coord = cellCoordinates(cords);
        return get(coord[0], coord[1]);
    }

    @Override
    public int width() {
        return table.length;
    }

    @Override
    public int height() {
        return table[0].length;
    }

    @Override
    public void set(int x, int y, String s) {
        if (isIn(x, y)) {
            table[x][y].setData(s);
            eval();
        }
    }

    @Override
    public void eval() {
        for (int i = 0; i < width(); i++) {
            for (int j = 0; j < height(); j++) {
                eval(i, j); // Evaluate each cell
            }
        }
    }

    @Override
    public String eval(int x, int y) {
        if (!isIn(x, y)) return Ex2Utils.EMPTY_CELL;

        SCell cell = table[x][y];
        if (cell.isVisited()) {
            // If the cell is already being evaluated, it's a cycle
            cell.setType(Ex2Utils.ERR_CYCLE_FORM);
            return Ex2Utils.ERR_CYCLE;
        }


        if (cell.getType() == Ex2Utils.TEXT) {
            return cell.getData();
        }
        if (cell.getType() == Ex2Utils.NUMBER) {
                return String.valueOf(Double.parseDouble(cell.getData())); // Return non-formula cells directly
            }

        try {
            cell.setVisited(true); // Mark the cell as visited
            double result = cell.evaluate(this, cell);
            cell.setVisited(false); // Clear the visited state
            if(cell.getType() == Ex2Utils.ERR_FORM_FORMAT){
                throw new IOException("Invalid sheet dimensions in file.");
            }
            return String.valueOf(result);
        } catch (Exception e) {
            cell.setType(Ex2Utils.ERR_FORM_FORMAT); // Set error type if evaluation fails
            return Ex2Utils.ERR_FORM;
        }
    }




    @Override
    public boolean isIn(int xx, int yy) {
        return xx >= 0 && yy >= 0 && xx < width() && yy < height();
    }

    @Override
    public int[][] depth() {
        int[][] depthMatrix = new int[width()][height()];
        boolean[][] visited = new boolean[width()][height()];

        for (int x = 0; x < width(); x++) {
            for (int y = 0; y < height(); y++) {
                if (!visited[x][y]) {
                    if (!updateDepth(x, y, visited, new HashSet<>(), depthMatrix)) {
                        // Set a special error depth if a cycle is detected
                        depthMatrix[x][y] = Ex2Utils.ERR_CYCLE_FORM;
                    }
                }
            }
        }

        return depthMatrix;
    }

    private boolean updateDepth(int x, int y, boolean[][] visited, Set<String> stack, int[][] depthMatrix) {
        if (!isIn(x, y)) return true;
        if(table[x][y].getType() == Ex2Utils.ERR_CYCLE_FORM){
            return false;
        }

        String cellId = x + "," + y;
        if (stack.contains(cellId)) {
            table[x][y].setType(Ex2Utils.ERR_CYCLE_FORM);
            return false;
        } // Cycle detected

        if (visited[x][y]) {
            return true;
        }; // Already processed

        visited[x][y] = true;
        stack.add(cellId);

        String data = table[x][y].getData();
        if (data.startsWith("=")) {
            for (String ref : data.substring(1).split("[^A-Za-z0-9]")) {
                ref = ref.toUpperCase();
                if (ref.matches("[A-Za-z]+[0-9]+")) {
                    int[] coords = cellCoordinates(ref);
                    if (!updateDepth(coords[0], coords[1], visited, stack, depthMatrix)) {
                        table[x][y].setType(Ex2Utils.ERR_CYCLE_FORM);
                        return false;
                    }
                    depthMatrix[x][y] = Math.max(depthMatrix[x][y], depthMatrix[coords[0]][coords[1]] + 1);
                }
            }
        }

        stack.remove(cellId);
        return true;
    }




    @Override
    public void load(String fileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            // Read dimensions from the first line
            String[] dimensions = reader.readLine().split(",");
            int newWidth = Integer.parseInt(dimensions[0]);
            int newHeight = Integer.parseInt(dimensions[1]);

            // Validate dimensions
            if (newWidth <= 0 || newHeight <= 0) {
                throw new IOException("Invalid sheet dimensions in file.");
            }

            // Resize the table
            table = new SCell[newWidth][newHeight];
            for (int i = 0; i < newWidth; i++) {
                for (int j = 0; j < newHeight; j++) {
                    table[i][j] = new SCell(""); // Ensure every cell is initialized
                }
            }

            // Load data into the table
            int rowIndex = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines or lines with insufficient columns
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] row = line.split(",");
                if (row.length != newHeight) {
                    // Fill missing cells with empty values if the row has fewer columns
                    String[] paddedRow = new String[newHeight];
                    System.arraycopy(row, 0, paddedRow, 0, row.length);
                    for (int i = row.length; i < newHeight; i++) {
                        paddedRow[i] = "";
                    }
                    row = paddedRow;
                }

                for (int j = 0; j < newHeight; j++) {
                    table[rowIndex][j].setData(row[j]);
                }
                rowIndex++;
            }

            // Validate row count
            if (rowIndex != newWidth) {
                throw new IOException(
                        "Data row count mismatch. Expected " + newWidth + ", but got " + rowIndex + "."
                );
            }

            eval(); // Re-evaluate the table
        } catch (IOException | NumberFormatException e) {
            throw new IOException("Error loading the sheet: " + e.getMessage(), e);
        }
    }

    @Override
    public void save(String fileName) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Save the dimensions
            writer.write(width() + "," + height());
            writer.newLine();

            // Save the table data
            for (int i = 0; i < width(); i++) {
                StringBuilder row = new StringBuilder();
                for (int j = 0; j < height(); j++) {
                    row.append(table[i][j].getData());
                    if (j < height() - 1) {
                        row.append(","); // Separate cells with a comma
                    }
                }
                writer.write(row.toString());
                writer.newLine(); // New row
            }
        } catch (IOException e) {
            throw new IOException("Error saving the sheet to file: " + e.getMessage(), e);
        }
    }


    public int[] cellCoordinates(String cords) {
        String column = cords.replaceAll("[^A-Za-z]", "");
        String row = cords.replaceAll("[^0-9]", "");
        int colIndex = column.charAt(0) - 'A'; // Assuming single letter columns
        int rowIndex = Integer.parseInt(row);
        return new int[]{colIndex, rowIndex};
    }

}
