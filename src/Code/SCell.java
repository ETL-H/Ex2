package Code;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.Map;
import java.util.HashMap;

public class SCell implements Cell {
    private String line;
    private int type;
    private int order;
    private Map<String, Double> variables;

    public SCell(String s) {
        setData(s);
        this.variables = new HashMap<>();
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void setOrder(int t) {
        this.order = t;
    }

    @Override
    public String toString() {
        return getData();
    }

    @Override
    public void setData(String s) {
        this.line = s;
        if (s.startsWith("=")) {
            this.type = Ex2Utils.FORM;
        } else {
            try {
                Double.parseDouble(s);
                this.type = Ex2Utils.NUMBER;
            } catch (NumberFormatException e) {
                this.type = Ex2Utils.TEXT;
            }
        }
    }

    @Override
    public String getData() {
        return line;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void setType(int t) {
        this.type = t;
    }

    public double evaluate(Ex2Sheet sheet, SCell scell) {
        if (type == Ex2Utils.NUMBER) {
            return Double.parseDouble(line);
        } else if (type == Ex2Utils.FORM) {
            String formula = line.substring(1);
            variables.clear();

            // Extract cell references and replace them in the formula
            for (String var : formula.split("[^A-Za-z0-9]")) {
                if (var.matches("[A-Za-z]+[0-9]+")) {
                    int[] cellCoordinates = sheet.cellCoordinates(var);
                    if (sheet.isIn(cellCoordinates[0], cellCoordinates[1])) {
                        String cellValue = sheet.eval(cellCoordinates[0], cellCoordinates[1]);
                        try {
                            variables.put(var, Double.parseDouble(cellValue));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Referenced cell is not a number: " + var);
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid cell reference: " + var);
                    }
                }
            }

            Expression expression = new ExpressionBuilder(formula)
                    .variables(variables.keySet())
                    .build();

            for (Map.Entry<String, Double> entry : variables.entrySet()) {
                expression.setVariable(entry.getKey(), entry.getValue());
            }

            return expression.evaluate();
        }

        scell.setType(Ex2Utils.ERR_FORM_FORMAT);
        return Ex2Utils.ERR_FORM_FORMAT;
    }
}
