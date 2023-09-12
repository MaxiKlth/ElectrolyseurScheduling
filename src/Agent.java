import org.apache.poi.util.IdentifierManager;

public class Agent {
	// Agent-Classification
	private int agentId; // Agenten-ID
	private static int agentCounter = 0; // Statische Zählervariable

	// External Parameters
	private double CapEx; // Capital-Costs
	private double OMFactor; // Factor for Operation & Maintenance
	private double IOpR; // Nennstrom
	private double VOPR; // Nennspannung
	private int n = 10; // Lifetime of Electrolyzer
	private double minPower; // Minimum Power from Electrolyzer
	private double maxPower; // Maximum Power from Electrolyzer
	private double CapEX_nt; // CapEx bezogen auf Lebensdauer
	private double OMEx_nt; // O&M-Kosten bezogen auf Lebensdauer
	private double PEL; // Elektrische Leistung des Elektrolyseur
	private double Ae; // Electrode Area
	private double ProductionCoefficientA; // Produktion = A*x^3 + B*X^2 + C*x + D
	private double ProductionCoefficientB;
	private double ProductionCoefficientC;
	private double ProductionCoefficientD;
	private double Demand_t; // Demand in Period t
	private double maxCostChange; //Agent mit der größten gewichtet Kostenänderungsrate GradientLCOH*LCOH
	private double ownCostChange; // Eigene Kostenänderungsrate 

	// Testparameter
	private double ElectricityPrice = 0.04; // Current Electricity Price (€/kWh)
	double MaxLeistung = 14.051 / 100; // aus ADMM-Excel Tabelle abgeleitet (IOp*VOp), durch 100 um Prozent zu entfernen
										
	// Lagrange Multiplicators
	private double lambda; // Lagrange-Multiplicator for Demand Constraint
	private double penaltyFactor; // Penalty-Term
	private int iteration = 0; // Iteration der Verhandlung

	// Gather-Information
	private double sumProduction;
	private double sumPowerDemand;
	private double sumCost;
	private double minLCOH;
	private double ownLCOH;
	private double dzCost;
	private double dzProduction;
	private int N; //Number of Agents 

	// Equations
	private double mH2_nt;

	// Constructor
	public Agent(double CapEx, double OMFactor, double minPower, double maxPower, double Pel, double IOpR, double VOpR,
			double ProductionCoefficientA, double ProductionCoefficientB, double ProductionCoefficientC,
			double ProductionCoefficientD, double Demand_t) {
		agentId = agentCounter++; // Agenten-ID erhöhen und zuweisen
		this.CapEx = CapEx;
		this.OMFactor = OMFactor;
		this.minPower = minPower;
		this.maxPower = maxPower;
		this.PEL = Pel;
		this.IOpR = IOpR;
		this.VOPR = VOpR;
		this.Ae = PEL / (IOpR * VOpR);
		this.ProductionCoefficientA = ProductionCoefficientA;
		this.ProductionCoefficientB = ProductionCoefficientB;
		this.ProductionCoefficientC = ProductionCoefficientC;
		this.ProductionCoefficientD = ProductionCoefficientD;
		this.Demand_t = Demand_t;

		// Calculate Constant Parameters
		this.CapEX_nt = CapEx / (n * 8760);
		this.OMEx_nt = CapEX_nt * OMFactor;
		System.out.println("Agent (ID" + agentId + ") erfolgreich erstellt");
	}

	// --------- OPTIMIZATION ---------

	// Variables
	private double x; // Leistung von Elektrolyseur/Agent
	private double z; // Hilfsvariable für z
	private double previousX;
	private double previousZ;

	// Broadcast information to other Agents
	public void BrodcastData(Matrix matrix) {
		x = getCurrentX();
		z = getCurrentZ();
		mH2_nt = ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
				+ ProductionCoefficientC * x + ProductionCoefficientD;
		double CapExPerKg = (CapEX_nt + OMEx_nt) / mH2_nt;
		double OpExPerKg = (MaxLeistung * x * Ae * ElectricityPrice) / mH2_nt;
		double cost = OpExPerKg + CapExPerKg;
		double lambda = this.lambda;
		double penaltyfactor = this.penaltyFactor;
		double currentX = x;
		double currentZ = z;
		double LCOHGradientValue;
		double objectiveFunctionGradientValue; 
		
		//Gradient ObjectiveFunction
		double term1 = (Ae*ElectricityPrice*MaxLeistung)/(N*(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD));
		
		double term2 = (Ae*ElectricityPrice*MaxLeistung)/(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD);
		
		double term3 = ((Ae*ElectricityPrice*x*MaxLeistung+OMEx_nt+CapEX_nt)*(3 * ProductionCoefficientA * Math.pow(x, 2) + 2 * ProductionCoefficientB * x
				+ ProductionCoefficientC))/(N*(Math.pow(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD, 2)));
		
		double term4 = ((Ae*ElectricityPrice*x*MaxLeistung+OMEx_nt+CapEX_nt)*(3 * ProductionCoefficientA * Math.pow(x, 2) + 2 * ProductionCoefficientB * x
				+ ProductionCoefficientC))/(Math.pow(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD, 2));
	
		objectiveFunctionGradientValue  = term1 + term2 - term3 - term4 + lambda;
		
		//Gradient LCOH
		LCOHGradientValue = term2 - term4;		

		//Update Matrix
		matrix.updateData(this.agentId, this.iteration, mH2_nt, cost, lambda, penaltyfactor, currentX, currentZ,
				dzProduction, dzCost, objectiveFunctionGradientValue, LCOHGradientValue);
		
		//Save own Cost Change Value
		ownCostChange = LCOHGradientValue*cost;
		
		this.iteration++;
	}

	// Gather: Get Information from other Agents
	public void Gather(Matrix matrix, int iteration) {
		int agentId = this.agentId;
		sumCost = matrix.getCosts(agentId, iteration);
		sumPowerDemand = matrix.getPower(agentId, iteration);
		sumProduction = matrix.getProduction(agentId, iteration);
		minLCOH = matrix.getMinLCOH(agentId, iteration); //gibt die kleinsten LCOH aus, ausgenommen des eigenen Agenten --> vielleicht noch anpassen und eigenen Agenten inkludieren
		ownLCOH = matrix.getOwnLCOH(agentId, iteration); //Gibt die eigenen Kosten eines Agenten wider
		N = matrix.getNumAgents(); // get Number of Agents
		maxCostChange = matrix.getMaxCostChange();	
	}

	// ----- Optimization -----
	public void initialization() {
		// Setzen der Grenzen für x
		double lowerBound = minPower;
		double upperBound = maxPower;

		// Initialize Lagrange multipliers and penalty term
		lambda = 0.0065;
				
		// Generieren eines zufälligen Werts für x innerhalb der Grenzen
		x = lowerBound + Math.random() * (upperBound - lowerBound); // Initial Value for x
		z = lowerBound + Math.random() * (upperBound - lowerBound); // Initial Value for z
		previousX = x;
	}

	// ----- Minimization of X -----
	
	public double minimizeLx() {
		double learningRate = 0.003; // Lernrate für die Optimierung
		double epsilon = 1e-6; // Genauigkeit oder Abbruchkriterium
		double previousX = getPreviousX();
		double minX = minPower; // Mindestwert für x
		double maxX = maxPower; // Maximalwert für x
		double currentX = getCurrentX(); // Startwert für x
		do {
			previousX = currentX;
			// Berechnen der Ableitung von L nach x
			double dLdx = computeDerivativeX(previousX);

			// Aktualisieren von x mit der Gradientenabstiegsformel
			currentX = previousX - learningRate * dLdx;

			// Überprüfen, ob der neue Wert von x außerhalb des Bereichs liegt
			if (currentX < minX) {
				currentX = minX;
			} else if (currentX > maxX) {
				currentX = maxX;
			}

			// Prüfen auf Konvergenz
			if (Math.abs(currentX - previousX) < epsilon) {
				break;
			}
		} while (true);

		setX(currentX);
		previousX = currentX;
		return currentX;
	}

	private double computeDerivativeX(double x) {
		//TODO
		//Testparameter, noch verbessern!
		N = 3;
		
		double term1 = (Ae*ElectricityPrice*MaxLeistung)/(N*(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD));
		
		double term2 = (Ae*ElectricityPrice*MaxLeistung)/(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD);
		
		double term3 = ((Ae*ElectricityPrice*x*MaxLeistung+OMEx_nt+CapEX_nt)*(3 * ProductionCoefficientA * Math.pow(x, 2) + 2 * ProductionCoefficientB * x
				+ ProductionCoefficientC))/(N*(Math.pow(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD, 2)));
		
		double term4 = ((Ae*ElectricityPrice*x*MaxLeistung+OMEx_nt+CapEX_nt)*(3 * ProductionCoefficientA * Math.pow(x, 2) + 2 * ProductionCoefficientB * x
				+ ProductionCoefficientC))/(Math.pow(ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
		+ ProductionCoefficientC * x + ProductionCoefficientD, 2));
	
		double GradientLCOH = term1 + term2 - term3 - term4 + lambda;

		double dLdx = GradientLCOH;

		return dLdx;
	}

	// ----- Minimization of Z (Produktionsmenge) -----
	
	public double minimizeLz() {
		double z = getCurrentZ(); // Startwert für z
		setPreviousZ(z);
		double epsilon = 1e-6; // Genauigkeit oder Abbruchkriterium

		do {
			// --- g(z)---
			// Produktionsabweichung in g(z)
			dzProduction = sumProduction + ProductionCoefficientA * Math.pow(z, 3)
					+ ProductionCoefficientB * Math.pow(z, 2) + ProductionCoefficientC * z + ProductionCoefficientD
					- Demand_t;
			// --- Ende g(z) ----

			// --- Ableitung von g(z) [g'(z)]---
			// Ableitung Produktionsabweichung in g(z)
			double dLdzProduction = (3 * ProductionCoefficientA * Math.pow(z, 2) + 2 * ProductionCoefficientB * z
					+ ProductionCoefficientC - lambda);

			// --- Ende Ableitung g(z) [g'(z)] ----

			// Anwendung des Newton-Verfahrens zur Annäherung an die Nullstelle
			 double newZ = z - (dzProduction / dLdzProduction);
			 /*
			 System.out.println("dzProduction: " + dzProduction);
			 System.out.println("dLzProduction: " + dLdzProduction);
			 System.out.println("z: " + z);
			 System.out.println("NewZ1: " + newZ);*/

			// Begrenzen von newZ auf den Bereich von minPower bis maxPower
			if (newZ < minPower) {
				newZ = minPower;
			} else if (newZ > maxPower) {
				newZ = maxPower;
			}
			
			// Überprüfen auf Konvergenz
			if (Math.abs(newZ - z) < epsilon) {
				break;
			}

			z = newZ;
			setZ(z);

		} while (true);

		return z;
	}
	
	public void DualUpdate() {
	    x = getCurrentX();
	    z = getCurrentZ(); 
	    ownCostChange = Math.abs(ownCostChange);
	    
	    mH2_nt = ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
        + ProductionCoefficientC * x + ProductionCoefficientD;

		double demandDeviation = mH2_nt + sumProduction - Demand_t;

	 //Calculate Demand Deviation 
	   double demandPercentage = Math.abs(demandDeviation / Demand_t) * 100; // Calculate demand deviation percentage
	   
	   if (demandDeviation > 0) {
	     //  System.out.printf("Demand Deviation (Ueberproduktion): %.3f\n", demandDeviation);
	       penaltyFactor = ((x-z)/z)+(maxCostChange - ownCostChange)/maxCostChange;
	   } else if (demandDeviation < 0) {
	   //    System.out.printf("Demand Deviation (Unterproduktion): %.3f\n", demandDeviation);
	     //  System.out.println("(maxCostChange - ownCostChange)/maxCostChange)" + (maxCostChange - ownCostChange)/maxCostChange);
	       penaltyFactor = ((x-z)/z - (maxCostChange - ownCostChange)/maxCostChange);
	   }
	   
	   //Update Lambda 
	   lambda = lambda + penaltyFactor*0.004;

	   if (demandPercentage > 40) {
	       System.out.printf("Iteration" + iteration +  " Demand Deviation: %.3f%% (>40)\n", demandPercentage);
	       lambda = lambda * 1.0004; 
	   } else if (demandPercentage > 30) {
	      System.out.printf("Iteration" + iteration + "Demand Deviation: %.3f%% (>30)\n", demandPercentage);
	       lambda = lambda * 1.0003; 
	   } else if (demandPercentage > 20) {
	       System.out.printf("Iteration" + iteration + "Demand Deviation: %.3f%% (>20)\n", demandPercentage);
	       lambda = lambda * 1.0002;
	   } else if (demandPercentage > 10) {
	       System.out.printf("Iteration" + iteration + "Demand Deviation: %.3f%% (>10)\n", demandPercentage);
	       lambda = lambda * 1.0001;
	   } else if (demandPercentage > 5) {
	      System.out.printf("Iteration" + iteration + "Demand Deviation: %.3f%% (>5)\n", demandPercentage);
	       lambda = lambda * 1.0000001;
	   } else if (demandPercentage > 2) {
	       System.out.printf("Itera0tion" + iteration + "Demand Deviation: %.3f%% (>2)\n", demandPercentage);
	       lambda = lambda * 1.00000001;
	   } else if (demandPercentage > 1.5) {
	       System.out.printf("Iteration" + iteration + "Demand Deviation: %.3f%% (>1.5)\n", demandPercentage);
	       lambda = lambda * 1.0000000001;
	   } else if (demandPercentage > 1.2) {
	      System.out.printf("Iteration" + iteration + "Demand Deviation: %.3f%% (>1.2)\n", demandPercentage);
	       lambda = lambda * 1.00000000001;
	   } else if (demandPercentage > 1.1) {
	       System.out.printf("Iteration" + iteration + "Demand Deviation: %.3f%% (>1.1)\n", demandPercentage);
	       lambda = lambda * 1.000000000001;
	   } else {
	     //  System.out.printf("Demand Deviation: %.3f%% (Minimal deviation)\n", demandPercentage);
	       lambda = lambda * 0.99999;
	   }	   
	   
	}


	// ---- Support Functions ----

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
	public void DemandDeviation() {
		x = getCurrentX();
		mH2_nt = ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
				+ ProductionCoefficientC * x + ProductionCoefficientD;
		double demandDeviation = mH2_nt + sumProduction - Demand_t;
		System.out.printf("Die Abweichung zur Zielmenge betraegt: %.3f\n", demandDeviation);
	}

}