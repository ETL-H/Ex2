package Code;

import java.io.*;

public class Ex2Sheet implements Sheet {
    private SCell[][] table;

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
            if (table[x][y].getType() == Ex2Utils.ERR_FORM_FORMAT){
                return Ex2Utils.ERR_FORM;
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
                eval(i, j);
            }
        }
    }

    @Override
    public String eval(int x, int y) {
        if (isIn(x, y)) {
            try {
                String value = table[x][y].getData();
                int type = table[x][y].getType();



                // Check if the value is text and return it as is
                if (type == Ex2Utils.TEXT) {
                    return value;
                }

                // Evaluate the cell's value
                String evaluatedValue = String.valueOf(table[x][y].evaluate(this, table[x][y]));

                // If the evaluated value is 0 (or 0.0), explicitly return "0"
                if (evaluatedValue.equals("0") || evaluatedValue.equals("0.0")) {
                    return "0.0";
                }

                return evaluatedValue;

            } catch (Exception e) {
                table[x][y].setType(Ex2Utils.ERR_FORM_FORMAT);
                return Ex2Utils.ERR_FORM;
            }
        }
        return Ex2Utils.EMPTY_CELL;
    }



    @Override
    public boolean isIn(int xx, int yy) {
        return xx >= 0 && yy >= 0 && xx < width() && yy < height();
    }

    @Override
    public int[][] depth() {
        int[][] depthMatrix = new int[width()][height()];

        // Initialize the depth matrix with default values (0).
        for (int i = 0; i < width(); i++) {
            for (int j = 0; j < height(); j++) {
                depthMatrix[i][j] = 0;  // Start with depth 0.
            }
        }

        // Iterate through all cells and evaluate their dependencies.
        for (int x = 0; x < width(); x++) {
            for (int y = 0; y < height(); y++) {
                // Skip empty cells or cells that already have a depth value
                if (table[x][y].getData().isEmpty() || depthMatrix[x][y] != 0) continue;

                // Call eval to check for dependencies
                eval(x, y);  // This will evaluate the cell and compute any potential dependencies

                // Mark this cell with a depth of 1 (or adjust depending on your logic)
                depthMatrix[x][y] = 1;

                // Check other cells that may reference this one (basic evaluation logic)
                for (int i = 0; i < width(); i++) {
                    for (int j = 0; j < height(); j++) {
                        if (table[i][j].getData().contains("CELL(" + x + "," + y + ")")) {
                            // Mark referencing cell's depth
                            depthMatrix[i][j] = Math.max(depthMatrix[i][j], depthMatrix[x][y] + 1);
                        }
                    }
                }
            }
        }

        return depthMatrix;
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
