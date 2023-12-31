import java.util.HashMap;

import java.util.Map;


public class Agent {
	// Agent-Classification
	private int agentId; // Agenten-ID
	private static int agentCounter = 0; // Statische Zählervariable

	// External Parameters
	private double CapEx; // Capital-Costs
	private double OMFactor; // Factor for Operation & Maintenance
	private int n = 10; // Lifetime of Electrolyzer
	private double minPower; // Minimum Power from Electrolyzer
	private double maxPower; // Maximum Power from Electrolyzer
	private double CapEX_nt; // CapEx bezogen auf Lebensdauer
	private double OMEx_nt; // O&M-Kosten bezogen auf Lebensdauer
	private double PEL; // Elektrische Leistung des Elektrolyseur
	private double ProductionCoefficientA;
	private double ProductionCoefficientB;
	private double ProductionCoefficientC;
	private double Demand; // Demand in Period t
	private double ElectricityPrice; // Current Electricity Price (€/kWh)
	
	// ADMM - Lagrange Multiplicators
	private double lambda; // Lagrange-Multiplicator for Demand Constraint
	private double penaltyFactor; // Penalty-Term
	private int iteration = 0; // Iteration
	private double epsilonProduction = 1; // Tolerable deviation from the required production quantity
	private int currentPeriod = 1;
	private boolean schedulingComplete = false;
	
	private boolean stateProduction;
	private boolean stateStandby;
	private boolean stateIdle;
	
	// Gather-Information
	private double sumProduction;
	private double dzProduction;

	// Equations
	private double mH2_nt;

	// HashMap to save Optimization Results
	private OptimizationResult optimizationResult = new OptimizationResult();

	// Liste zur Abspeicherung von Strompreis und Nachfrage
	private Map<Integer, Map<String, Double>> externalInformation = new HashMap<>();

	// Methode zum Hinzufügen von Informationen für eine Periode
	public void addExternalDSMInformation(int period, double demand, double electricityPrice) {
		// Prüfen, ob es bereits Informationen für diese Periode gibt
		if (!externalInformation.containsKey(period)) {
			externalInformation.put(period, new HashMap<>());
		}

		// Die Informationen für die Periode in die Map einfügen
		Map<String, Double> DSMInfo = externalInformation.get(period);
		DSMInfo.put("Demand", demand);
		DSMInfo.put("ElectricityPrice", electricityPrice);
	}
	


	// Constructor
	public Agent(double CapEx, double OMFactor, double minPower, double maxPower, double Pel,
			double ProductionCoefficientA, double ProductionCoefficientB, double ProductionCoefficientC, boolean initialState) {
		agentId = agentCounter++; // Agenten-ID erhöhen und zuweisen
		this.CapEx = CapEx;
		this.OMFactor = OMFactor;
		this.minPower = minPower;
		this.maxPower = maxPower;
		this.PEL = Pel;
		this.ProductionCoefficientA = ProductionCoefficientA;
		this.ProductionCoefficientB = ProductionCoefficientB;
		this.ProductionCoefficientC = ProductionCoefficientC;
		this.stateProduction = initialState;

		// Calculate Constant Parameters
		this.CapEX_nt = CapEx / (n * 8760);
		this.OMEx_nt = CapEX_nt * OMFactor;
		System.out.println("Agent (ID" + agentId + ") erfolgreich erstellt");
	}

	// --------- ADMM - Optimization ---------

	// Variables
	private double x; // Leistung von Elektrolyseur/Agent
	private double z; // Hilfsvariable für z
	private double previousX;
	private double previousZ;

	// Broadcast information to other Agents
	public void BrodcastData(Matrix matrix) {
		x = getCurrentX();
		z = getCurrentZ();
		double productionQuantity = calculateProductionQuantity(x);
		double cost = calculatemLCOH(x);
		double lambda = this.lambda;
		double penaltyfactor = this.penaltyFactor;
		double mLCOHGradient = calculateGradientmLCOH(x);

		// Update Matrix
		matrix.updateData(currentPeriod, this.agentId, this.iteration, productionQuantity, cost, lambda, penaltyfactor, x, z,
				dzProduction, mLCOHGradient);
		
		if(agentId == 2) {
	//	System.out.println("DemandDeviation: " + DemandDeviation());
	//	System.out.println("X: " + getCurrentX());
		
		}
		this.iteration++;
	}

	// Gather: Get Information from other Agents
	public void Gather(Matrix matrix, int iteration) {
		int agentId = this.agentId;
		sumProduction = matrix.getProduction(agentId, iteration);
		double demandDeviation = DemandDeviation();
		
		if (periodScheduled() == true) {
			ElectricityPrice = getElectricityPriceForPeriod(currentPeriod);
			optimizationResult.addResult(currentPeriod, ElectricityPrice, stateStandby, stateIdle, stateProduction, getCurrentX(),
					calculatemLCOH(getCurrentX()), calculateProductionQuantity(getCurrentX()),
					getDemandForPeriod(currentPeriod));
			schedulingComplete();
			currentPeriod++;
		}

	}

	// ----- Optimization -----
	public void initialization() {
		// Setzen der Grenzen für x
		double lowerBound = minPower;
		double upperBound = maxPower;

		// Initialize Lagrange multipliers and penalty term
		lambda = 0;

		// Generieren eines zufälligen Werts für x innerhalb der Grenzen
		x = lowerBound + Math.random() * (upperBound - lowerBound); // Initial Value for x
		z = lowerBound + Math.random() * (upperBound - lowerBound); // Initial Value for z
		previousX = x;
	}

	// ----- Minimization of X -----

	public double minimizeLx() {
	    double min_x_value = minPower; // Initialisieren Sie min_x_value mit minPower
	    double min_mLCOH = Double.POSITIVE_INFINITY;
	    double toleranceMinPower = 0.01; // Toleranzschwelle, hier auf 0,01 (1%) eingestellt
	    ElectricityPrice = getElectricityPriceForPeriod(currentPeriod);

	    // Überprüfen Sie, ob der Elektrolyseur sich im Produktionsmodus befindet
	   if (stateProduction) {
	        // Loop über alle x-Werte
	       // for (double x = minPower; x <= maxPower; x += 0.003) {
	        	for (double x = 0; x <= maxPower; x += 0.003) {
	            mH2_nt = calculateProductionQuantity(x);
	            double mLCOH = (CapEX_nt + OMEx_nt) / mH2_nt + (PEL * x / 100 * ElectricityPrice) / mH2_nt
	                    + lambda * (x - z);

	            // Wenn der aktuelle mLCOH-Wert kleiner als das vorherige Minimum ist,
	            // aktualisieren Sie das Minimum und den X-Wert
	            if (mLCOH < min_mLCOH) {
	                min_mLCOH = mLCOH;
	                min_x_value = x;
	            }
	        }
	   } 
	   else {
	        // Der Elektrolyseur ist nicht im Produktionsmodus, daher ist die Produktion 0
	        min_x_value = 0.0;
	    }
	   
		
		if(Math.abs(min_x_value - minPower) < toleranceMinPower  && agentId == 2) {
			System.out.println("Minx:" + x);
			stateProduction = false;
			stateStandby = true;
		}

	    // Rückgabe des X-Werts, bei dem mLCOH minimal ist
	    setX(min_x_value);
	    return min_x_value;
	}

	// ----- Minimization of Z -----
	
	public double minimizeLz() {
		double increment = 0.0003;
		Demand = getDemandForPeriod(currentPeriod);

		// Überprüfen Sie, ob der Elektrolyseur sich im Produktionsmodus befindet
			double minDiffToZero = Double.POSITIVE_INFINITY; // Initialisieren Sie minDiffToZero mit einem hohen Wert
			double minZ = minPower; // Initialisieren Sie minZ mit minPower

			if(stateProduction) {
			// Schleife, um verschiedene Werte für Z auszuprobieren
			for (double z = minPower; z <= maxPower; z += increment) {

				double dzProduction = sumProduction + ProductionCoefficientA * Math.pow(z, 2)
						+ ProductionCoefficientB * z + ProductionCoefficientC - Demand + lambda * (x - z);
				double diffToZero = Math.abs(dzProduction - 0);

				// Wenn die aktuelle Differenz zu 0 kleiner als das bisherige Minimum ist,
				// aktualisieren Sie das Minimum und den Z-Wert
				if (diffToZero < minDiffToZero) {
					minDiffToZero = diffToZero;
					minZ = z;
				}
			}
			}
			else {
				minZ = 0;
			}

			setZ(minZ);


		return getCurrentZ();
	}
	

	public void DualUpdate() {
		x = getCurrentX();
		z = getCurrentZ();
		mH2_nt = calculateProductionQuantity(getCurrentX());

		// Calculate Demand Deviation
		double demandDeviation = DemandDeviation();
		double demandPercentage = Math.abs(demandDeviation / getDemandForPeriod(currentPeriod)) * 100; // Calculate demand deviation percentage
	//	System.out.println("DemandDeviation: " + demandDeviation);

		// Update Lambda
		penaltyFactor = 0.09;//0.0002 vorher
		lambda = lambda + (penaltyFactor * calculateGradientmLCOH(getCurrentX()) / demandPercentage) * (x - z);
	}

	// --- Functions ----

	public double calculateGradientmLCOH(double x) {
		// TODO:Lambda ergänzen?
		ElectricityPrice = getElectricityPriceForPeriod(currentPeriod);
		double gradientmLCOH = (ElectricityPrice * PEL) / (100
				* (ProductionCoefficientA * Math.pow(x, 2) + ProductionCoefficientB * x + ProductionCoefficientC))
				- (ElectricityPrice * PEL * x * (2 * ProductionCoefficientA * x + ProductionCoefficientB))
						/ (100 * (Math.pow(ProductionCoefficientA * Math.pow(x, 2) + ProductionCoefficientB * x
								+ ProductionCoefficientC, 2)));
		return gradientmLCOH;
	}

	public double calculateProductionQuantity(double x) {
		double  productionQuantity;
		
		if(stateProduction) {
		productionQuantity = ProductionCoefficientA * Math.pow(x, 2) + ProductionCoefficientB * x
				+ ProductionCoefficientC;}
		else {
			productionQuantity = 0;
		}
		return productionQuantity;
	}

	public double calculatemLCOH(double x) {
	    ElectricityPrice = getElectricityPriceForPeriod(currentPeriod);
	    
	    // Überprüfen Sie, ob der Elektrolyseur sich im Produktionsmodus befindet
	    if (stateProduction) {
	        double mLCOH = (CapEX_nt + OMEx_nt) / mH2_nt + (PEL * x / 100 * ElectricityPrice) / mH2_nt;
	        return mLCOH;
	    } else {
	        // Der Elektrolyseur ist nicht im Produktionsmodus, daher gibt es keine Kosten (Rückgabe von 0)
	        return 0.0;
	    }
	}

	
	public boolean periodScheduled() {
		
		boolean periodScheduled = false;
		double demandDeviation = DemandDeviation();
		
		if (Math.abs(demandDeviation) < epsilonProduction  && currentPeriod <= getLastPeriod()) {
			periodScheduled = true;
		}
		return periodScheduled;
	}
	
	public boolean schedulingComplete() {
		
		double demandDeviation = DemandDeviation();
		
		if (Math.abs(demandDeviation) < epsilonProduction  && currentPeriod == getLastPeriod()) {
			schedulingComplete = true;
		}
		
		return schedulingComplete;
	}
	

	public void getOptimizationResults() {
		optimizationResult.printOptimizationResults();
	}

	// Getter und Setter
	public double getCurrentX() {
		return x;
	}

	public double getCurrentZ() {
		return z;
	}

	public void setX(double newX) {
		x = newX;
	}

	public void setZ(double newZ) {
		z = newZ;
	}

	public double getPreviousX() {
		return previousX;
	}

	public void setPreviousX(double previousX) {
		this.previousX = previousX;
	}

	public double getPreviousZ() {
		return previousZ;
	}

	public void setPreviousZ(double previousZ) {
		this.previousZ = previousZ;
	}

	// Ausgabe Abweichung
	public double DemandDeviation() {
		mH2_nt = calculateProductionQuantity(getCurrentX());
		Demand = getDemandForPeriod(currentPeriod);
		double demandDeviation = mH2_nt + sumProduction - Demand;
		return demandDeviation;
	}

	// Method for getting the price of electricity for a given period
	public Double getElectricityPriceForPeriod(int period) {
	    int lastPeriod = getLastPeriod();

	    // If period is greater than lastPeriod, set period to lastPeriod
	    if (period > lastPeriod) {
	        period = lastPeriod;
	    }

	    if (externalInformation.containsKey(period)) {
	        Map<String, Double> DSMInfo = externalInformation.get(period);
	        return DSMInfo.get("ElectricityPrice");
	    } else {
	        // Period not found, optionally return null
	        System.out.println("Null");
	        return null;
	    }
	}


	// Method of getting the demand for a certain period
	public Double getDemandForPeriod(int period) {
		int lastPeriod = getLastPeriod();
		
	    // If period is greater than lastPeriod, set period to lastPeriod
	    if (period > lastPeriod) {
	        period = lastPeriod;
	    }
		
		if (externalInformation.containsKey(period)) {
			Map<String, Double> DSMInfo = externalInformation.get(period);
			return DSMInfo.get("Demand");
		} else {
			// Period not found, optionally return null
			return null;
		}
	}
	
	
	public int getLastPeriod() {
	    int lastPeriod = -1; // Initialisieren Sie lastPeriod mit einem ungültigen Wert

	    for (int period : externalInformation.keySet()) {
	        if (period > lastPeriod) {
	            lastPeriod = period;
	        }
	    }

	    return lastPeriod;
	}


}