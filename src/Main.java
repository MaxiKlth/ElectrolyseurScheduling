public class Main {
    public static void main(String[] args) {

        // Erstellen der Matrix für die Datenübertragung zwischen den Agenten
        Matrix matrix = new Matrix(3);
    	double Demand_t = 373902; // Demand in Period t 249268 für 2 Elektrolyseure
        
        // Parameter für beide Agenten 
        double ProdctionCoefficientA = 0.2919;
        double ProductionCoefficientB = -68.565;
        double ProductionCoefficientC = 7649.2;
        double ProductionCoefficientD = 12623;
        double CapExPerKW = 898;

        // Parameter für den Agenten 1
        double PEL1 = 8000000;
        double CapEx1 = PEL1*CapExPerKW;
        double OMFactor1 = 0.05;
        double minPower1 = 10;
        double maxPower1 = 100;
        double IOpR = 1.7;
        double VOPR = 1.75; 

        // Parameter für den Agenten 2
        double OMFactor2 = 0.5;
        double minPower2 = 10;
        double maxPower2 = 100;
        double PEL2 = 8000000;
        double CapEx2 = CapExPerKW*PEL2;
        
        // Parameter für den Agenten 3
        double OMFactor3 = 0.5;
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

        // Anzahl der Iterationen für das ADMM
        int numIterations = 10000;
        

        // ----ADMM-----

        // Step 0: Initialization 
        agent1.initialization();
        agent2.initialization();
        agent3.initialization();

        for (int iteration = 0; iteration < numIterations; iteration++) {
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
            double newZ1 = agent1.minimizeLz(); 
            agent1.setZ(newZ1);
            double newZ2 = agent2.minimizeLz(); 
            agent2.setZ(newZ2);
            double newZ3 = agent3.minimizeLz(); 
            agent3.setZ(newZ3);
            
            // Step 5: Dual Update 
            agent1.DualUpdate();
            agent2.DualUpdate();
            agent3.DualUpdate();


        }

        // Ausgabe der Ergebnisse
        matrix.printMatrix();
        //matrix.writeMatrixToExcel();
        agent1.DemandDeviation();

    }
}
