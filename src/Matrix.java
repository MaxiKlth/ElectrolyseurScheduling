import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;

public class Matrix {
	private List<Integer> agents; // Agenten-Liste
	private List<Integer> iterations; // Iterations-Liste
	private List<Double> production; // Produktions-Liste
	private List<Double> costs; // Kosten-Liste
	private List<Double> lambdaValues; // Liste der Lagrange-Multiplikatoren lambda
	private List<Double> penaltyTerms; // Liste der Strafterme
	private List<Double> xValues; // Liste der x-Werte (Power)
	private List<Double> zValues; // Liste der z-Werte
	private List<Double> demandDeviation; // Nachfrageabweichung
	private List<Double> mLCOHGradient; // Gradient LCOH
	
	//Anzahl an Agenten
	private int numAgents;

	// Konstruktor
	public Matrix(int numAgents) {
		this.numAgents = numAgents;
		agents = new ArrayList<>();
		iterations = new ArrayList<>();
		production = new ArrayList<>();
		costs = new ArrayList<>();
		lambdaValues = new ArrayList<>();
		penaltyTerms = new ArrayList<>();
		xValues = new ArrayList<>();
		zValues = new ArrayList<>();
		demandDeviation = new ArrayList<>();
		mLCOHGradient = new ArrayList<>();
	}

	// Methode zum Eintragen der Daten
	public void updateData(int agent, int iteration, double production, double costs, double lambda, double penaltyTerm,
			double currentX, double currentZ, double demandDeviation, double mLCOHGradient) {
		this.agents.add(agent);
		this.iterations.add(iteration);
		this.production.add(production);
		this.costs.add(costs);
		this.lambdaValues.add(lambda);
		this.penaltyTerms.add(penaltyTerm);
		this.xValues.add(currentX);
		this.zValues.add(currentZ);
		this.demandDeviation.add(demandDeviation);
		this.mLCOHGradient.add(mLCOHGradient);
	}
	

	public double getCosts(int agentId, int iteration) {
		double sum = 0;
		for (int i = 0; i < costs.size(); i++) {
			if (agents.get(i) != agentId && iterations.get(i) == iteration) {
				sum += costs.get(i);
			}
		}
		return sum;
	}

	public double getPower(int agentId, int iteration) {
		double sum = 0;
		for (int i = 0; i < xValues.size(); i++) {
			if (agents.get(i) != agentId && iterations.get(i) == iteration) {
				sum += xValues.get(i);
			}
		}
		return sum;
	}

	public double getProduction(int agentId, int iteration) {
		double sum = 0;
		for (int i = 0; i < production.size(); i++) {
			if (agents.get(i) != agentId && iterations.get(i) == iteration) {
				sum += production.get(i);
			}
		}
		return sum;
	}

	public double getMinLCOH(int agentId, int iteration) {
		double minLCOH = Double.MAX_VALUE;
		for (int i = 0; i < agents.size(); i++) {
			int currentAgent = agents.get(i);
			int currentIteration = iterations.get(i);
			double currentCosts = this.costs.get(i);

			// Betrachtung aktueller Iteration
			if (currentIteration == iteration) {
				minLCOH = Math.min(minLCOH, currentCosts);
			}
		}
		return minLCOH;
	}

	public double getOwnLCOH(int agentId, int iteration) {
		for (int i = 0; i < agents.size(); i++) {
			if (agents.get(i) == agentId && iterations.get(i) == iteration) {
				return costs.get(i);
			}
		}
		return 0.0; // Agent mit der gegebenen ID und Iteration wurde nicht gefunden
	}

	public int getNumAgents() {
		return numAgents;
	}

	// Methode zur Überprüfung des Abbruchkriteriums
	public boolean isConvergedProduction(double targetProduction, double tolerance, int currentIteration) {
		double totalProduction = 0;
		double totalCost = 0;

		for (int i = 0; i < agents.size(); i++) {
			if (iterations.get(i) == currentIteration) {
				totalProduction += production.get(i);
				totalCost += production.get(i) * costs.get(i); // Gesamtkosten berechnen
			}
		}

		double deviation = Math.abs(totalProduction - targetProduction);

		// Überprüfen, ob die Abweichung kleiner als die Toleranz ist
		return deviation <= tolerance;
	}
	
    // Methode zur Überprüfung des Abbruchkriteriums
	public boolean isConvergedLCOH(double targetProduction, double tolerance, int currentIteration) {
	    double minWeightedLCOHGradient = Double.MAX_VALUE;
	    double maxWeightedLCOHGradient = Double.MIN_VALUE;

	    // Ermittle den minimalen und maximalen Wert von weightedLCOHGradient über alle Agenten
	    for (int i = 0; i < agents.size(); i++) {
	        if (iterations.get(i) == currentIteration) {
	            double currentGradient = mLCOHGradient.get(i);
	            minWeightedLCOHGradient = Math.min(minWeightedLCOHGradient, currentGradient);
	            maxWeightedLCOHGradient = Math.max(maxWeightedLCOHGradient, currentGradient);
	        }
	    }

	    double deviation = Math.max(Math.abs(maxWeightedLCOHGradient), Math.abs(minWeightedLCOHGradient)) - Math.min(Math.abs(maxWeightedLCOHGradient), Math.abs(minWeightedLCOHGradient));
	    
	    // Ausgabe der Agenten-ID und aktuellen Iteration, wenn das Abbruchkriterium erfüllt ist
	    if (deviation < tolerance) {
	        for (int i = 0; i < agents.size(); i++) {
	            int agentId = agents.get(i); // Agenten-ID abrufen
	           // System.out.printf("%-7d%-10d%-15.7f\n", agentId, currentIteration, deviation);
	        }
	    }

	    return deviation < tolerance;
	}
	
	// Methode zur Überprüfung des Abbruchkriteriums
	public boolean isConvergedtotal(double targetProduction, double toleranceProduction, double toleranceLCOHGradient, int currentIteration) {
	    double totalProduction = 0;
	    double totalCost = 0;
	    double minWeightedLCOHGradient = Double.MAX_VALUE;
	    double maxWeightedLCOHGradient = Double.MIN_VALUE;

	    // Ermittle den minimalen und maximalen Wert von weightedLCOHGradient über alle Agenten
	    for (int i = 0; i < agents.size(); i++) {
	        if (iterations.get(i) == currentIteration) {
	            double currentGradient = mLCOHGradient.get(i);
	            minWeightedLCOHGradient = Math.min(minWeightedLCOHGradient, currentGradient);
	            maxWeightedLCOHGradient = Math.max(maxWeightedLCOHGradient, currentGradient);
	            totalProduction += production.get(i);
	            totalCost += production.get(i) * costs.get(i); // Gesamtkosten berechnen
	        }
	    }

	    double deviationProduction = Math.abs(totalProduction - targetProduction);
	    double deviationGradient = Math.max(Math.abs(maxWeightedLCOHGradient), Math.abs(minWeightedLCOHGradient)) - Math.min(Math.abs(maxWeightedLCOHGradient), Math.abs(minWeightedLCOHGradient));

	    // Überprüfen, ob beide Abbruchkriterien erfüllt sind
	    boolean isProductionConverged = deviationProduction <= toleranceProduction;
	    boolean isGradientConverged = deviationGradient <= toleranceLCOHGradient;

	    if (isProductionConverged && isGradientConverged) {
	        // Ausgabe der Agenten-ID und aktuellen Iteration, wenn das Abbruchkriterium erfüllt ist
	        for (int i = 0; i < agents.size(); i++) {
	            int agentId = agents.get(i); // Agenten-ID abrufen
	           // System.out.printf("%-7d%-10d%-15.7f\n", agentId, currentIteration, deviationGradient);
	        }
	        return true; // Beide Kriterien erfüllt, Konvergenz erreicht
	    }

	    return false; // Konvergenz noch nicht erreicht
	}



	public void printMatrix() {
	    System.out.printf("%-7s%-10s%-15s%-15s%-15s%-15s%-15s%-15s%-15s\n", "Agent", "Iteration",
	            "Produktion", "LCOH", "Lambda", "Penalty", "X", "Z", "mLCOHGradient");
	    
	    for (int i = 0; i < agents.size(); i++) {
	        // Begrenzen der Nachkommastellen auf 2 Stellen
	        System.out.printf("%-7d%-10d%-15.2f%-15.2f%-15.2f%-15.2f%-15.2f%-15.2f%-15.7f\n",
	                agents.get(i), iterations.get(i), production.get(i), costs.get(i), lambdaValues.get(i),
	                penaltyTerms.get(i), xValues.get(i), zValues.get(i), mLCOHGradient.get(i));
	    }
	}


	public void writeMatrixToExcel() {
		String filePath = "D:\\Dokumente\\OneDrive - Helmut-Schmidt-Universität\\02_eModule\\AP4 - Integration\\Agentensystem\\ADMM\\Konvergenz.xlsx";
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Results");

		// Erzeuge eine Überschriftsreihe
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("Agent");
		headerRow.createCell(1).setCellValue("Iteration");
		headerRow.createCell(2).setCellValue("Produktion");
		headerRow.createCell(3).setCellValue("Kosten");
		headerRow.createCell(4).setCellValue("Lambda");
		headerRow.createCell(5).setCellValue("Penalty");
		headerRow.createCell(6).setCellValue("X");
		headerRow.createCell(7).setCellValue("Z");
		headerRow.createCell(8).setCellValue("dZProduction");
		headerRow.createCell(9).setCellValue("mLCOHGradient");

		// Schreibe die Ergebnisse in die Tabelle
		for (int i = 0; i < agents.size(); i++) {
			Row dataRow = sheet.createRow(i + 1);
			dataRow.createCell(0).setCellValue(agents.get(i));
			dataRow.createCell(1).setCellValue(iterations.get(i));
			dataRow.createCell(2).setCellValue(production.get(i));
			dataRow.createCell(3).setCellValue(costs.get(i));
			dataRow.createCell(4).setCellValue(lambdaValues.get(i));
			dataRow.createCell(5).setCellValue(penaltyTerms.get(i));
			dataRow.createCell(6).setCellValue(xValues.get(i));
			dataRow.createCell(7).setCellValue(zValues.get(i));
			dataRow.createCell(8).setCellValue(demandDeviation.get(i));
			dataRow.createCell(9).setCellValue(mLCOHGradient.get(i));
		}

		// Speichere die Excel-Datei
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			workbook.write(fos);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
