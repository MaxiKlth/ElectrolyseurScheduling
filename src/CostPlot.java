/*
 * import org.jfree.chart.ChartFactory; import org.jfree.chart.ChartFrame;
 * import org.jfree.chart.JFreeChart; import org.jfree.chart.plot.XYPlot; import
 * org.jfree.chart.renderer.xy.XYLineAndShapeRenderer; import
 * org.jfree.data.xy.DefaultXYDataset;
 * 
 * public class CostPlot { public static void createPlot(Matrix matrix) { //
 * Erstelle ein Dataset für den Plot DefaultXYDataset dataset = new
 * DefaultXYDataset();
 * 
 * // Sammle die Daten für den Plot List<Integer> iterations =
 * matrix.getIterations(); List<Double> costs = matrix.getCosts(); List<Integer>
 * agents = matrix.getAgents(); int numAgents = matrix.getNumAgents();
 * 
 * // Füge die Daten für jeden Agenten dem Dataset hinzu for (int i = 0; i <
 * numAgents; i++) { double[][] agentData = new double[2][iterations.size()];
 * for (int j = 0; j < iterations.size(); j++) { agentData[0][j] =
 * iterations.get(j); agentData[1][j] = costs.get(j); }
 * dataset.addSeries("Agent " + agents.get(i), agentData); }
 * 
 * // Erstelle den Plot JFreeChart chart = ChartFactory.createXYLineChart(
 * "Cost Plot", // Titel "Iteration", // x-Achse Beschriftung "Cost", // y-Achse
 * Beschriftung dataset // Dataset );
 * 
 * // Passe den Plot an XYPlot plot = chart.getXYPlot(); XYLineAndShapeRenderer
 * renderer = new XYLineAndShapeRenderer();
 * 
 * // Zeige die Linien für die Datenpunkte renderer.setBaseShapesVisible(true);
 * 
 * // Setze den Renderer für jeden Agenten separat for (int i = 0; i <
 * numAgents; i++) { renderer.setSeriesLinesVisible(i, true); }
 * 
 * plot.setRenderer(renderer);
 * 
 * // Erstelle das Fenster für den Plot ChartFrame frame = new
 * ChartFrame("Cost Plot", chart); frame.pack(); frame.setVisible(true); } }
 */