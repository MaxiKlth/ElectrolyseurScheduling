import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;

public class Matrix {
    private List<Integer> agents; // Agenten-Liste
    private List<Integer> iterations; // Iterations-Liste
    private List<Double> production; // Produktions-Liste
    private List<Double> power; // Leistungs-Liste
    private List<Double> costs; // Kosten-Liste
    private List<Double> lambdaValues; // Liste der Lagrange-Multiplikatoren lambda
    private List<Double> penaltyTerms; // Liste der Strafterme
    private List<Double> xValues; // Liste der x-Werte
    private List<Double> zValues; // Liste der z-Werte

    // Konstruktor
    public Matrix(int numAgents) {
        agents = new ArrayList<>();
        iterations = new ArrayList<>();
        production = new ArrayList<>();
        power = new ArrayList<>();
        costs = new ArrayList<>();
        lambdaValues = new ArrayList<>();
        penaltyTerms = new ArrayList<>();
        xValues = new ArrayList<>();
        zValues = new ArrayList<>();
    }

    // Methode zum Eintragen der Daten
    public void updateData(int agent, int iteration, double production, double power, double costs,
            double lambda, double penaltyTerm, double currentX, double currentZ) {
        agents.add(agent);
        iterations.add(iteration);
        this.production.add(production);
        this.power.add(power);
        this.costs.add(costs);
        lambdaValues.add(lambda);
       // muValues.add(mu);
       // gammaValues.add(gamma);
        penaltyTerms.add(penaltyTerm);
        xValues.add(currentX);
        zValues.add(currentZ);
        
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
        for (int i = 0; i < power.size(); i++) {
            if (agents.get(i) != agentId && iterations.get(i) == iteration) {
                sum += power.get(i);
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
    
    
    	public int getNumAgents() {
    	    return agents.size();
    	}

    
    	public void printMatrix() {
    		System.out.printf("%-7s%-10s%-15s%-15s%-15s%-15s%-15s%-15s%-15s\n",
    		        "Agent", "Iteration", "Produktion", "Energie", "LCOH", "Lambda", "Penalty", "X", "Z");
    	    for (int i = 0; i < agents.size(); i++) {
    	        // Begrenzen der Nachkommastellen auf 2 Stellen
    	        System.out.printf("%-7d%-10d%-15.2f%-15.2f%-15.2f%-15.2f%-15.2f%-15.2f%-15.2f\n",
    	                agents.get(i), iterations.get(i), production.get(i), power.get(i), costs.get(i),
    	                lambdaValues.get(i), penaltyTerms.get(i), xValues.get(i), zValues.get(i));
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
    	    headerRow.createCell(3).setCellValue("Energie");
    	    headerRow.createCell(4).setCellValue("Kosten");
    	    headerRow.createCell(5).setCellValue("Lambda");
    	    headerRow.createCell(6).setCellValue("Penalty");
    	    headerRow.createCell(7).setCellValue("X");
    	    headerRow.createCell(8).setCellValue("Z");

    	    // Schreibe die Ergebnisse in die Tabelle
    	    for (int i = 0; i < agents.size(); i++) {
    	        Row dataRow = sheet.createRow(i + 1);
    	        dataRow.createCell(0).setCellValue(agents.get(i));
    	        dataRow.createCell(1).setCellValue(iterations.get(i));
    	        dataRow.createCell(2).setCellValue(production.get(i));
    	        dataRow.createCell(3).setCellValue(power.get(i));
    	        dataRow.createCell(4).setCellValue(costs.get(i));
    	        dataRow.createCell(5).setCellValue(lambdaValues.get(i));
    	        dataRow.createCell(6).setCellValue(penaltyTerms.get(i));
    	        dataRow.createCell(7).setCellValue(xValues.get(i));
    	        dataRow.createCell(8).setCellValue(zValues.get(i));
    	    }

    	    // Speichere die Excel-Datei
    	    try (FileOutputStream fos = new FileOutputStream(filePath)) {
    	        workbook.write(fos);
    	    } catch (IOException e) {
    	        e.printStackTrace();
    	    }
    	}
    	

    	
    	public void printMatrixsorted() {
    	    // Erstelle eine Liste von Indizes für die Sortierung nach Iterationen
    	    List<Integer> sortedIndices = new ArrayList<>();
    	    for (int i = 0; i < agents.size(); i++) {
    	        sortedIndices.add(i);
    	    }
    	    
    	    // Sortiere die Indizes basierend auf der Iterationsnummer
    	    sortedIndices.sort(Comparator.comparingInt(iterations::get));
    	    
    	    System.out.println("Agent\tIteration\tProduktion\tEnergie\t\tKosten\t\tLambda\t\tMu\t\tGamma\t\tPenalty");
    	    
    	    // Gib die Agenteninformationen in sortierter Reihenfolge aus
    	    for (int index : sortedIndices) {
    	        int agent = agents.get(index);
    	        int iteration = iterations.get(index);
    	        double productionValue = production.get(index);
    	        double powerValue = power.get(index);
    	        double costValue = costs.get(index);
    	        double lambdaValue = lambdaValues.get(index);
   // 	        double muValue = muValues.get(index);
   // 	        double gammaValue = gammaValues.get(index);
    	        double penaltyValue = penaltyTerms.get(index);
    	        
    	        // Begrenzen der Nachkommastellen auf 2 Stellen
    	        System.out.printf("%d\t%d\t\t%.3f\t\t%.3f\t\t%.3f\t\t%.3f\t\t%.3f\t\t%.3f\t\t%.3f\n", agent, iteration,
    	                productionValue, powerValue, costValue, lambdaValue, penaltyValue);
    	    }
    	}



}
