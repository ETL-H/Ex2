package Spreadsheet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class Cell implements CellInterface {
    private String value;

    public Cell(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean isNumber() {
        return isNumber(value);
    }

    @Override
    public boolean isText() {
        return isText(value);
    }

    @Override
    public boolean isFormula() {
        return isFormula(value);
    }

    // Static utility methods
    public static boolean isNumber(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isText(String text) {
        return !isNumber(text) && !text.startsWith("=");
    }

    public static boolean isFormula(String text) {
        return text.startsWith("=") && !isText(text);
    }

    // Formula evaluation
    public Double evaluateFormula() throws ScriptException {
        if (!isFormula()) {
            throw new IllegalArgumentException("Cell value is not a formula.");
        }
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("nashorn");
        if (engine == null) {
            throw new ScriptException("JavaScript engine not found.");
        }
        return (Double) engine.eval(value.substring(1)); // Remove leading '=' from formula string
    }
}
