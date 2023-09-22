public class Main {
	public static void main(String[] args) {

		// Erstellen der Matrix für die Datenübertragung zwischen den Agenten
		Matrix matrix = new Matrix(3);
		double Demand_t = 380 * 1.8; // 380 kg/h max pro Elektrolyseur

		// Parameter für beide Agenten
		double ProdctionCoefficientA = -0.0147;
		double ProductionCoefficientB = 5.1263;
		double ProductionCoefficientC = 10.763;
		double CapExkWPEM = 450;
		double CapExkWAEL = 898;

		// Parameter für den Agenten 0
		double PEL1 = 8000;
		double CapEx1 = PEL1 * CapExkWAEL;
		double OMFactor1 = 0.03;
		double minPower1 = 10;
		double maxPower1 = 100;

		// Parameter für den Agenten 1
		double OMFactor2 = 0.05;
		double minPower2 = 10;
		double maxPower2 = 100;
		double PEL2 = 8000;
		double CapEx2 = CapExkWAEL * PEL2;

		// Parameter für den Agenten 2
		double OMFactor3 = 0.09;
		double minPower3 = 10;
		double maxPower3 = 100;
		double PEL3 = 8000;
		double CapEx3 = CapExkWPEM * PEL3;

		// Erstellen Agent 1
		Agent agent1 = new Agent(CapEx1, OMFactor1, minPower1, maxPower1, PEL1, ProdctionCoefficientA,
				ProductionCoefficientB, ProductionCoefficientC, true);
		// Erstellen eines zweiten Agenten
		Agent agent2 = new Agent(CapEx2, OMFactor2, minPower2, maxPower2, PEL2, ProdctionCoefficientA,
				ProductionCoefficientB, ProductionCoefficientC, true);
		// Erstellen eines dritten Agenten
		Agent agent3 = new Agent(CapEx3, OMFactor3, minPower3, maxPower3, PEL3, ProdctionCoefficientA,
				ProductionCoefficientB, ProductionCoefficientC, true);
		
		// Fügen Sie DSM-Informationen für verschiedene Perioden hinzu
		agent1.addExternalDSMInformation(1, 380 * 1.88, 0.08); // Periode 1
		agent2.addExternalDSMInformation(1, 380 * 1.88, 0.08); // Periode 1
		agent3.addExternalDSMInformation(1, 380 * 1.88, 0.08); // Periode 1

		agent1.addExternalDSMInformation(2, 380 * 0.5, 0.09); // Periode 2
		agent2.addExternalDSMInformation(2, 380 * 0.5, 0.09); // Periode 2
		agent3.addExternalDSMInformation(2, 380 * 0.5, 0.09); // Periode 2

		/*
		agent1.addExternalDSMInformation(3, 380 * 1.4, 0.07); // Periode 3
		agent2.addExternalDSMInformation(3, 380 * 1.4, 0.07); // Periode 3
		agent3.addExternalDSMInformation(3, 380 * 1.4, 0.07); // Periode 3

		agent1.addExternalDSMInformation(4, 380 * 1.5, 0.04); // Periode 3
		agent2.addExternalDSMInformation(4, 380 * 1.5, 0.04); // Periode 3
		agent3.addExternalDSMInformation(4, 380 * 1.5, 0.04); // Periode 3
		*/
		
		
		// ----ADMM-----
		boolean converged = false;
		int iteration = 0;
		long startTime = System.currentTimeMillis();

		// Step 0: Initialization
		agent1.initialization();
		agent2.initialization();
		agent3.initialization();

		while (true) {
		        // Step 1: Minimization of x
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

		        agent1.DualUpdate();
		        agent2.DualUpdate();
		        agent3.DualUpdate();

		        // Check if termination criterion is reached
		        //converged = matrix.isConvergedProduction(Demand_t, epsilonProduction, iteration);
		        converged = agent3.schedulingComplete();
		        
		        if (converged == true) {
		            break; // Beendet die innere Schleife, wenn converged true ist
		        }

		        // Increase Iteration
		        iteration++;
		}
	
		System.out.println("------- Agent 1  ---------");
		agent1.getOptimizationResults();
		
		System.out.println("------- Agent 2  ---------");
		agent2.getOptimizationResults();
		
		System.out.println("------- Agent 3  ---------");
		agent3.getOptimizationResults();

		// Ausgabe der Ergebnisse
		matrix.printMatrix();
		matrix.writeMatrixToExcel();

		// Computation Time
		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		System.out.println("ADMM duration: " + duration + " milliseconds");
	}
}
