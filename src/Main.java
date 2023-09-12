public class Main {
    public static void main(String[] args) {

        // Erstellen der Matrix für die Datenübertragung zwischen den Agenten
        Matrix matrix = new Matrix(3);
    	double Demand_t = 571638*0.9; // 373902
    	
        // Parameter für beide Agenten 
        double ProdctionCoefficientA = 0.2919;
        double ProductionCoefficientB = -68.565;
        double ProductionCoefficientC = 7649.2;
        double ProductionCoefficientD = 12623;
        double CapExPerKW = 898;

        // Parameter für den Agenten 0
        double PEL1 = 8000000;
        double CapEx1 = PEL1*CapExPerKW;
        double OMFactor1 = 0.008;
        double minPower1 = 10;
        double maxPower1 = 100;
        double IOpR = 1.7;
        double VOPR = 1.75; 

        // Parameter für den Agenten 1
        double OMFactor2 = 2;
        double minPower2 = 10;
        double maxPower2 = 100;
        double PEL2 = 8000000;
        double CapEx2 = CapExPerKW*PEL2;
        
        // Parameter für den Agenten 2
        double OMFactor3 = 5;
        double minPower3 = 10;
        double maxPower3 = 100;
        double PEL3 = 8000000;
        double CapEx3 = CapExPerKW*PEL3;

  
        // Erstellen Agent 1
        Agent agent1 = new Agent(CapEx1, OMFactor1, minPower1, maxPower1,PEL1, IOpR, VOPR, ProdctionCoefficientA, ProductionCoefficientB, ProductionCoefficientC, ProductionCoefficientD, Demand_t);
        // Erstellen eines zweiten Agenten
        Agent agent2 = new Agent(CapEx2, OMFactor2, minPower2, maxPower2, PEL2, IOpR, VOPR, ProdctionCoefficientA, ProductionCoefficientB, ProductionCoefficientC, ProductionCoefficientD, Demand_t);
        // Erstellen eines dritten Agenten
        Agent agent3 = new Agent(CapEx3, OMFactor3, minPower3, maxPower3, PEL3, IOpR, VOPR, ProdctionCoefficientA, ProductionCoefficientB, ProductionCoefficientC, ProductionCoefficientD, Demand_t);
     
        // ----ADMM-----
        boolean converged = false;
        double epsilonProduction = 20;
        double epsilonLCOH = 0.0095;
        int iteration = 0;
        long startTime = System.currentTimeMillis();
        
        // Step 0: Initialization 
        agent1.initialization();
        agent2.initialization();
        agent3.initialization();
        
		int numRuns = 100;
		while (!converged)
			for (int run = 1; run <= numRuns; run++) {
			// Step 1: Minimization of X
			agent1.minimizeLx();
			agent2.minimizeLx();
			agent3.minimizeLx();

			// Step 2: Broadcast Information:
			agent1.BrodcastData(matrix);
			agent2.BrodcastData(matrix);
			agent3.BrodcastData(matrix);

			// Step 3: Gather Information:
			agent1.Gather(matrix, iteration);
			agent2.Gather(matrix, iteration);
			agent3.Gather(matrix, iteration);
				
			// Step 4: Minimization of Z
			agent1.minimizeLz();
			agent2.minimizeLz();
			agent3.minimizeLz();

			// Step 5: Dual Update
			agent1.DualUpdate();
			agent2.DualUpdate();
			agent3.DualUpdate();

			// Check if termination criterion is reached
			converged = matrix.isConvergedProduction(Demand_t, epsilonProduction, iteration);
			//converged = matrix.isConvergedLCOH(Demand_t, epsilonLCOH, iteration);
			//converged = matrix.isConvergedtotal(Demand_t,epsilonProduction,epsilonLCOH, iteration);
			
			// Increase Iteration
			iteration++;

		}

        // Ausgabe der Ergebnisse
        matrix.printMatrix();
       // matrix.writeMatrixToExcel();
        agent1.DemandDeviation();
        
     // Computation Time
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("ADMM duration: " + duration + " milliseconds");
    }
}
