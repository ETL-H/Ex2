package Spreadsheet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Ex2 {
    private static final int ROWS = 17; // 16 rows + 1 header
    private static final int COLS = 10; // 9 columns + 1 header (A-I)
    private static CellInterface[][] cells; // Backend cells

    public static void main(String[] args) {
        // Initialize backend with Cell (which implements CellInterface)
        cells = new Cell[ROWS - 1][COLS - 1]; // Exclude header row and column
        for (int row = 0; row < ROWS - 1; row++) {
            for (int col = 0; col < COLS - 1; col++) {
                cells[row][col] = new Cell(""); // Initialize cells with empty strings
            }
        }

        // Create main frame
        JFrame frame = new JFrame("Spreadsheet");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Create a grid layout for the spreadsheet
        JPanel gridPanel = new JPanel(new GridLayout(ROWS, COLS));
        JLabel[][] cellLabels = new JLabel[ROWS - 1][COLS - 1];

        // Populate the grid
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (row == 0 && col == 0) {
                    // Top-left corner (empty cell)
                    gridPanel.add(new JLabel(""));
                } else if (row == 0) {
                    // Column headers (A-I)
                    char colName = (char) ('A' + col - 1);
                    JLabel label = new JLabel(String.valueOf(colName), SwingConstants.CENTER);
                    label.setFont(new Font("Arial", Font.BOLD, 12));
                    gridPanel.add(label);
                } else if (col == 0) {
                    // Row headers (0-16)
                    JLabel label = new JLabel(String.valueOf(row - 1), SwingConstants.CENTER);
                    label.setFont(new Font("Arial", Font.BOLD, 12));
                    gridPanel.add(label);
                } else {
                    // Editable cells
                    JLabel cellLabel = new JLabel("", SwingConstants.CENTER);
                    cellLabel.setOpaque(true);
                    cellLabel.setBackground(Color.WHITE);
                    cellLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                    int cellRow = row - 1;
                    int cellCol = col - 1;

                    // Set initial value from the backend
                    cellLabel.setText(cells[cellRow][cellCol].getValue());

                    // Add mouse listener to open a dialog for editing
                    cellLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            String currentValue = cells[cellRow][cellCol].getValue();
                            String newValue = JOptionPane.showInputDialog(
                                    frame,
                                    "Cell: " + (char) ('A' + cellCol) + (cellRow),
                                    currentValue
                            );

                            if (newValue != null) { // If user didn't cancel
                                cells[cellRow][cellCol].setValue(newValue); // Update backend
                                cellLabel.setText(newValue); // Update UI
                            }
                        }
                    });

                    cellLabels[cellRow][cellCol] = cellLabel;
                    gridPanel.add(cellLabel);
                }
            }
        }

        // Add grid panel to frame
        frame.add(gridPanel);
        frame.setVisible(true);
    }
}
