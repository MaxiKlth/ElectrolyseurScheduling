import java.util.HashMap;
import java.util.Map;

public class OptimizationResult {
    private Map<Integer, Map<String, Object>> results;

    public OptimizationResult() {
        results = new HashMap<>();
    }

    public void addResult(int period, double electricityPrice, boolean standbyState, boolean idleState,
            boolean productionState, double setpoint, double mLCOH, double productionQuantity,
            double demand) {
        Map<String, Object> result = new HashMap<>();
        result.put("ElectricityPrice", electricityPrice);
        result.put("StandbyState", standbyState);
        result.put("IdleState", idleState);
        result.put("ProductionState", productionState);
        result.put("Setpoint", setpoint);
        result.put("mLCOH", mLCOH);
        result.put("ProductionQuantity", productionQuantity);
        result.put("Demand", demand);

        results.put(period, result);
    }

    public Map<String, Object> getResult(int period) {
        return results.get(period);
    }
}
