package com.invsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CalculatorEngine {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\s*([+\\-*/xX]|\\d+(?:\\.\\d+)?(?:[kKmM])?)\\s*");

    public static Optional<Double> evaluate(String expression) {
        if (expression == null || expression.isBlank()) return Optional.empty();

        List<Double> numbers = new ArrayList<>();
        List<Character> operators = new ArrayList<>();

        Matcher matcher = TOKEN_PATTERN.matcher(expression);
        int lastEnd = 0;
        boolean expectNumber = true;

        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                // There are unmatched characters (stray characters)
                return Optional.empty();
            }
            lastEnd = matcher.end();
            String token = matcher.group(1).toLowerCase();

            if (expectNumber) {
                if (token.equals("+") || token.equals("-") || token.equals("*") || token.equals("/") || token.equals("x")) {
                    // Encountered operator when expecting number (e.g. trailing operator or double operator)
                    // Note: unary minus is not explicitly required by spec, but we can fail cleanly
                    return Optional.empty();
                }
                double multiplier = 1.0;
                if (token.endsWith("k")) {
                    multiplier = 1000.0;
                    token = token.substring(0, token.length() - 1);
                } else if (token.endsWith("m")) {
                    multiplier = 1000000.0;
                    token = token.substring(0, token.length() - 1);
                }
                try {
                    double val = Double.parseDouble(token) * multiplier;
                    numbers.add(val);
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
                expectNumber = false;
            } else {
                if (token.length() != 1 || "+-*/x".indexOf(token.charAt(0)) == -1) {
                    return Optional.empty();
                }
                char op = token.charAt(0);
                if (op == 'x') op = '*';
                operators.add(op);
                expectNumber = true;
            }
        }

        if (lastEnd != expression.length() || expectNumber) {
            // Stray characters at the end, or ended with an operator
            return Optional.empty();
        }

        // Pass 1: resolve * and /
        for (int i = 0; i < operators.size(); ) {
            char op = operators.get(i);
            if (op == '*' || op == '/') {
                double left = numbers.get(i);
                double right = numbers.get(i + 1);
                double result;
                if (op == '*') {
                    result = left * right;
                } else {
                    if (right == 0.0) return Optional.empty();
                    result = left / right;
                }
                numbers.set(i, result);
                numbers.remove(i + 1);
                operators.remove(i);
            } else {
                i++;
            }
        }

        // Pass 2: resolve + and -
        double result = numbers.get(0);
        for (int i = 0; i < operators.size(); i++) {
            char op = operators.get(i);
            double right = numbers.get(i + 1);
            if (op == '+') result += right;
            else if (op == '-') result -= right;
        }

        return Optional.of(result);
    }

    public static void main(String[] args) {
        System.out.println("=5k+3.5m -> " + evaluate("5k+3.5m")); // expected: 3505000.0
        System.out.println("=10k*3-2k -> " + evaluate("10k*3-2k")); // expected: 28000.0
        System.out.println("=2.5m/5 -> " + evaluate("2.5m/5")); // expected: 500000.0
        System.out.println("=5k+ -> " + evaluate("5k+")); // expected: empty
        System.out.println("=5k(3) -> " + evaluate("5k(3)")); // expected: empty
    }
}
