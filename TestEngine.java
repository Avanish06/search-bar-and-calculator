import java.util.Optional;
public class TestEngine {
    public static void main(String[] args) {
        Optional<Double> r = com.invsearch.CalculatorEngine.evaluate(" 3m+8m");
        System.out.println("Result for ' 3m+8m': " + (r.isPresent() ? r.get() : "empty"));
        
        Optional<Double> r2 = com.invsearch.CalculatorEngine.evaluate(" 3m + 8m ");
        System.out.println("Result for ' 3m + 8m ': " + (r2.isPresent() ? r2.get() : "empty"));
        
        Optional<Double> r3 = com.invsearch.CalculatorEngine.evaluate("3m + 8m");
        System.out.println("Result for '3m + 8m': " + (r3.isPresent() ? r3.get() : "empty"));
        
        Optional<Double> r4 = com.invsearch.CalculatorEngine.evaluate("3m+ 8m");
        System.out.println("Result for '3m+ 8m': " + (r4.isPresent() ? r4.get() : "empty"));
        
        Optional<Double> r5 = com.invsearch.CalculatorEngine.evaluate(" 3m+8m ");
        System.out.println("Result for ' 3m+8m ': " + (r5.isPresent() ? r5.get() : "empty"));
    }
}
