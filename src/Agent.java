
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

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
	private double ProductionCoefficientA; // Produktion = A*x^2 + B*X + C*x + D
	private double ProductionCoefficientB;
	private double ProductionCoefficientC;
	private double ProductionCoefficientD;
	private double Demand_t; //Demand in Period t 

	// Testparameter
	private double ElectricityPrice = 0.04; // Current Electricity Price (€/kWh)
	private double PowerAvailable_t = 60; // Available Power for Period t //40
	double MaxLeistung = 14.051 / 100; // aus ADMM-Excel Tabelle abgeleitet (IOp*VOp), durch 100 um Prozent zu
										// entfernen

	// Lagrange Multiplicators
	private double lambda; // Lagrange-Multiplicator for Demand Constraint
	private double penaltyFactor; // Penalty-Term
	private int iteration = 0; // Iteration der Verhandlung

	// Gather-Information
	private double sumProduction;
	private double sumPowerDemand;
	private double sumCost;

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
		double power = x;
		double lambda = this.lambda;
		double penaltyfactor = this.penaltyFactor;
		double currentX = x;
		double currentZ = z;

		matrix.updateData(this.agentId, this.iteration, mH2_nt, power, cost, lambda, penaltyfactor, currentX, currentZ);

		this.iteration++;
	}

	// Gather: Get Information from other Agents
	public void Gather(Matrix matrix, int iteration) {
		int agentId = this.agentId;
		sumCost = matrix.getCosts(agentId, iteration);
		sumPowerDemand = matrix.getPower(agentId, iteration);
		sumProduction = matrix.getProduction(agentId, iteration);
	}

	// ----- Optimization -----
	public void initialization() {
		// Setzen der Grenzen für x
		double lowerBound = minPower;
		double upperBound = maxPower;

		// Initialize Lagrange multipliers and penalty term
		lambda = 0.0005;
		penaltyFactor = 0.000000036; // für 3 Agenten

		// Generieren eines zufälligen Werts für x innerhalb der Grenzen
		x = lowerBound + Math.random() * (upperBound - lowerBound); // Initial Value for x
		z = lowerBound + Math.random() * (upperBound - lowerBound); // Initial Value for z
		previousX = x;
		System.out.printf("Startwert x: %.3f\n ", x);
		System.out.printf("Startwert z: %.3f\n ", z);
	}

	public double minimizeLx() {
		double learningRate = 0.0005; // Lernrate für die Optimierung
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

		double NennerStrafterm = ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
				+ ProductionCoefficientC * x + ProductionCoefficientD;
		double GradientLCOH = ((Ae * ElectricityPrice * MaxLeistung) / NennerStrafterm
				- ((Ae * ElectricityPrice * MaxLeistung * x + OMEx_nt + CapEX_nt)
						* (3 * ProductionCoefficientA * Math.pow(x, 2) + 2 * ProductionCoefficientB * x
								+ ProductionCoefficientC))
						/ Math.pow(NennerStrafterm, 2))
				+ lambda;

		// Erstmal noch unberücksichtigt
		double GradientStraftermProduktionsmenge = penaltyFactor
				* (3 * ProductionCoefficientA * Math.pow(x, 2) + 2 * ProductionCoefficientB * x
						+ ProductionCoefficientC)
				* (ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
						+ ProductionCoefficientC * x + ProductionCoefficientD - Demand_t + sumProduction);
		// double dLdx = GradientLCOH + GradientStraftermProduktionsmenge;
		double dLdx = GradientLCOH;

		return dLdx;
	}

	public double minimizeLz() {
		double z = getCurrentZ(); // Startwert für z
		double epsilon = 1e-6; // Genauigkeit oder Abbruchkriterium

		do {
			double dz = sumProduction + ProductionCoefficientA * Math.pow(z, 3)
					+ ProductionCoefficientB * Math.pow(z, 2) + ProductionCoefficientC * z + ProductionCoefficientD
					- Demand_t; // g(z)
			double dLdz = (3 * ProductionCoefficientA * Math.pow(z, 2) + 2 * ProductionCoefficientB * z
					+ ProductionCoefficientC - lambda); // Erste Ableitung von g(z)

			// Anwendung des Newton-Verfahrens zur Annäherung an die Nullstelle
			double newZ = z - (dz / dLdz);

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

		} while (true);

		return z;
	}

	public void DualUpdate() {
		x = getCurrentX();
		z = getCurrentZ();
		lambda = lambda + penaltyFactor * (x + z);
		mH2_nt = ProductionCoefficientA * Math.pow(x, 3) + ProductionCoefficientB * Math.pow(x, 2)
				+ ProductionCoefficientC * x + ProductionCoefficientD;

		double demandDeviation = mH2_nt + sumProduction - Demand_t;
		double energyDeviation = (PowerAvailable_t - x - sumPowerDemand);
		System.out.printf("Agent %d\n", agentId);

		if (demandDeviation > 0) {
			System.out.printf("Demand Deviation (Ueberproduktion): %.3f\n", demandDeviation);
		} else if (demandDeviation < 0) {
			System.out.printf("Demand Deviation (Unterproduktion): %.3f\n", demandDeviation);
		}
		// System.out.printf("Energy Deviation: %.3f\n", energyDeviation);

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

	public void writeLCOHToExcel() {
		double minX = 5;
		double maxX = 100;
		String filePath = "D:\\Dokumente\\OneDrive - Helmut-Schmidt-Universität\\02_eModule\\AP4 - Integration\\Agentensystem\\ADMM\\LCOH.xlsx";

		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Data");

		// Write column headers
		Row headerRow = sheet.createRow(0);
		Cell headerCellX = headerRow.createCell(0);
		headerCellX.setCellValue("Power [%]");
		Cell headerCellCost = headerRow.createCell(1);
		headerCellCost.setCellValue("LCOH per kg");
		Cell headerCellMH2_nt = headerRow.createCell(2);
		headerCellMH2_nt.setCellValue("Current MH2");
		Cell headerCellCapExPerKg = headerRow.createCell(3);
		headerCellCapExPerKg.setCellValue("CapEx per kg");
		Cell headerCellOpExPerKg = headerRow.createCell(4);
		headerCellOpExPerKg.setCellValue("OpEx per kg");

		int rowIndex = 1;
		for (double currentX = minX; currentX <= maxX; currentX++) {
			Row dataRow = sheet.createRow(rowIndex);
			Cell dataCellX = dataRow.createCell(0);
			dataCellX.setCellValue(currentX);
			Cell dataCellCost = dataRow.createCell(1);
			double currentMH2_nt = ProductionCoefficientA * Math.pow(currentX, 3)
					+ ProductionCoefficientB * Math.pow(currentX, 2) + ProductionCoefficientC * currentX
					+ ProductionCoefficientD;
			double CapExPerKg = (CapEX_nt + OMEx_nt) / currentMH2_nt;
			double OpExPerKg = (MaxLeistung * currentX * Ae * ElectricityPrice) / currentMH2_nt;
			double cost = OpExPerKg + CapExPerKg;
			dataCellCost.setCellValue(cost);
			Cell dataCellMH2_nt = dataRow.createCell(2);
			dataCellMH2_nt.setCellValue(currentMH2_nt);
			Cell dataCellCapExPerKg = dataRow.createCell(3);
			dataCellCapExPerKg.setCellValue(CapExPerKg);
			Cell dataCellOpExPerKg = dataRow.createCell(4);
			dataCellOpExPerKg.setCellValue(OpExPerKg);
			rowIndex++;
		}

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			workbook.write(fos);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
