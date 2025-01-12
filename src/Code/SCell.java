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
    private boolean visited = false;

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }


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
        } else if (type == Ex2Utils.FORM || type == Ex2Utils.ERR_FORM_FORMAT || type == Ex2Utils.ERR_CYCLE_FORM) {
            try {
                String formula = line.substring(1);
                formula = formula.toUpperCase();
                variables.clear();

                for (String var : formula.split("[^A-Za-z0-9]")) {
                    if (var.matches("[A-Za-z]+[0-9]+")) {
                        int[] coords = sheet.cellCoordinates(var);
                        if (!sheet.isIn(coords[0], coords[1])) {
                            scell.setType(Ex2Utils.ERR_FORM_FORMAT);
                            throw new IllegalArgumentException("Invalid reference: " + var);
                        }
                        String refValue = sheet.eval(coords[0], coords[1]);
                        SCell cell = sheet.get(coords[0],coords[1]);
                        if (cell.getType() != Ex2Utils.ERR_FORM_FORMAT && cell.getType() != Ex2Utils.ERR){
                            scell.setType(Ex2Utils.FORM);
                        }


                        if (refValue.equals(Ex2Utils.ERR_CYCLE)) {
                            scell.setType(Ex2Utils.ERR_CYCLE_FORM);
                            return Ex2Utils.ERR_CYCLE_FORM;
                        }

                        variables.put(var, Double.parseDouble(refValue));
                    }
                }

                Expression expression = new ExpressionBuilder(formula)
                        .variables(variables.keySet())
                        .build();

                variables.forEach(expression::setVariable);
                return expression.evaluate();
            } catch (Exception e) {
                scell.setType(Ex2Utils.ERR_FORM_FORMAT);
                return Ex2Utils.ERR_FORM_FORMAT;
            }
        }

        scell.setType(Ex2Utils.ERR_FORM_FORMAT);
        return Ex2Utils.ERR_FORM_FORMAT;
    }

}
