package Spreadsheet;


public class Cell {
    public boolean isNumber(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isText(String text) {
        return !isNumber(text) && !text.startsWith("=");
    }

    public boolean isForm(String text) {
        return text.startsWith("=") && !isText(text);
    }

    public Double computeForm(String form) {
        return null;
    }
}