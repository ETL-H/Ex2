package Spreadsheet;

public interface CellInterface {
    String getValue();  // Get the value of the cell
    void setValue(String value);  // Set the value of the cell
    boolean isNumber();  // Check if the value is a number
    boolean isText();  // Check if the value is text
    boolean isFormula();  // Check if the value is a formula
}
