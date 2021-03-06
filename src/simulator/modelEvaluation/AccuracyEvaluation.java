package simulator.modelEvaluation;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.google.common.base.Joiner;

import util.CsvManipulation;
import util.HungarianAlgorithm;
import util.Sample;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import createUserFeatures.BuildSimFeatures;
import createUserFeatures.BuildTMFeatures;
import createUserFeatures.ClusterAnalysis;
import createUserFeatures.Features;
import createUserFeatures.UserFeatures;

/**
 * Runs a modified version of the evaluation described in Stability-based Clustering Evaluation by Lange et al.
 * Clusters both TradeMe and Synthetic data, maximises matching with Hungarian algorithm, then
 * measures accuracy. 
 */
public class AccuracyEvaluation implements Runnable {

	private static final Logger logger = Logger.getLogger(AccuracyEvaluation.class); 
	
	private static BufferedWriter bw_full, bw_short;
	
//	public static void main(String[] args) throws Exception {
//		go();
//	}
//	
//	private static void go() throws Exception {
//		makeBws();
//		
//		String[] featureArray = {"-3ln-10-5-6ln-11-12","-3ln-10-5-6ln-12","-3ln-10-5-6ln-11","-3ln-10-6ln-11-12","-3ln-10-6ln-12","-3ln-10-6ln-11",};
//		int numClusters = 4;
//
//		ExecutorService es = Executors.newFixedThreadPool(3);
//		Random r = new Random();
//		for (int sampleCount = 0; sampleCount < 10; sampleCount++) {
//			List<Callable<Object>> tasks = new ArrayList<>(); 
//			for (String featureString : featureArray) {
//				tasks.add(Executors.callable(new AccuracyEvaluation(featureString, numClusters, r.nextInt(), r.nextInt())));
//			}
//			es.invokeAll(tasks);
//		}
//
////		String featuresForClustering = "-1ln-2ln-3ln-10-5-11-9";
////		String featuresToPrint = "-1ln-2ln-3ln-10-5-11-9";
////		String simClusteredFile = ClusterAnalysis.clusterToFile(new BuildSimFeatures(featuresForClustering), featuresForClustering, featuresToPrint, 4);
////		buildDT(simClusteredFile, 20);
//		
//		es.shutdown();
//		
//		flushBws();
//		System.out.println("Finished.");
//	}
//	
	static {
		try {
			bw_full = Files.newBufferedWriter(new File("Evaluation_full_results_inv_1nn_f6").toPath(), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			bw_short = Files.newBufferedWriter(new File("Evaluation_short_results_inv_1nn_f6").toPath(), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
//
//	private static void flushBws() throws IOException {
//		bw_full.flush();
//		bw_short.flush();
//	}
//
	public synchronized static void writeResults(BufferedWriter bw_full, BufferedWriter bw_short, int numberOfClusters, 
			List<Features> featuresForClustering, double[][] correctMatrix, int[][] assignment, int correct, int total, 
			int tmSeed, int simSeed, int runNumber) throws IOException {
		long currentTime = System.currentTimeMillis();
		write(bw_full, "Clusters:" + numberOfClusters);
		write(bw_full, "Clustering Features:" + Features.fileLabels(featuresForClustering));
		write(bw_full, "Timestamp:" + new Date(currentTime).toString());
		write(bw_full, "Run:" + runNumber);
		write(bw_full, AccuracyEvaluation.multiArrayString(correctMatrix).toString());
		write(bw_full, AccuracyEvaluation.multiArrayString(assignment).toString());
		write(bw_full, correct + ":" + total);
		write(bw_full, "tmSeed: " + tmSeed);
		write(bw_full, "simSeed: " + simSeed);
		bw_full.flush();
		write(bw_short, numberOfClusters + "," + Features.fileLabels(featuresForClustering) + "," + currentTime + "," + correct + "," + tmSeed + "," + simSeed + "," + runNumber);
		bw_short.flush();
	}

	private static void write(BufferedWriter bw, String s) throws IOException {
		bw.append(s);
		bw.newLine();
	}

//	private enum ClassifyWith {TM, SIM};
//	private final ClassifyWith classifyWith;
	private final List<Features> featuresForClustering;
	private final List<Features>  featuresToPrint;
	private final int numberOfClusters;

	private final int tmSeed, simSeed;
	
	public AccuracyEvaluation(List<Features> featuresForClustering, int numberOfClusters
//			, ClassifyWith classifyWith
			, int tmSeed, int simSeed) {
//		this.classifyWith = classifyWith;
		this.featuresForClustering = featuresForClustering;
		this.featuresToPrint = featuresForClustering;
		this.numberOfClusters = numberOfClusters;
		this.tmSeed = tmSeed;
		this.simSeed = simSeed;
	}
	
	public void run() {
		try {
//		String featuresForClustering = "-1ln-2ln-3ln-10-5-11-9";
//		String featuresToPrint = "-1ln-2ln-3ln-10-5-11-9";
//		String featuresForClustering = "-3ln-10-5-6ln-11-12";
//		String featuresToPrint = "-3ln-10-5-6ln-11-12";
//		int numberOfClusters = 4;

		String tmClusteredFile = ClusterAnalysis.clusterToFile(new BuildTMFeatures(), tmSeed, featuresForClustering, featuresToPrint, numberOfClusters, "");
		logger.warn("TM clustered instances in file: " + tmClusteredFile + ".");
		
		String simClusteredFile = ClusterAnalysis.clusterToFile(new BuildSimFeatures(true), simSeed, featuresForClustering, featuresToPrint, numberOfClusters, "");
		logger.warn("Sim clustered instances in file: " + simClusteredFile + ".");
		
		evaluate(tmClusteredFile, simClusteredFile, numberOfClusters, bw_full, bw_short, featuresForClustering, tmSeed, simSeed, 0);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		Random r = new Random(12345678);
		for (int seed = 0; seed < 30; seed++) {
			for (int clusters = 2; clusters <= 10; clusters++) {
				BuildTMFeatures tmFeatureBuilder = new BuildTMFeatures();
				Map<Integer, UserFeatures> userFeatures = tmFeatureBuilder.build();
				
				HashSet<Integer> half = new HashSet<>(Sample.randomSample(userFeatures.keySet().iterator(), userFeatures.size() / 2, r));
				Map<Integer, UserFeatures> aHalf = new HashMap<>();
				Map<Integer, UserFeatures> bHalf = new HashMap<>();
				userFeatures.forEach((id, features) -> {
					if (half.contains(id)) 
						aHalf.put(id, features); 
					else 
						bHalf.put(id, features);
					});
				
				
				int aSeed = seed * 123 + 13579;
				int bSeed = seed * 123 + 24680;
				String aHalfFilename = ClusterAnalysis.clusterToFile(tmFeatureBuilder.getClass(), tmFeatureBuilder.trim(), aHalf, aSeed, Features.DEFAULT_FEATURES_NOID, Features.DEFAULT_FEATURES_NOID, clusters, false, false, -1, "a");
				String bHalfFilename = ClusterAnalysis.clusterToFile(tmFeatureBuilder.getClass(), tmFeatureBuilder.trim(), bHalf, bSeed, Features.DEFAULT_FEATURES_NOID, Features.DEFAULT_FEATURES_NOID, clusters, false, false, -1, "b");
				logger.warn("TM clustered instances in file: " + aHalfFilename + " and " + bHalfFilename);
				
//				partition(dir, tmClusteredFile);
				String dir = "C:/Users/Sidney Tsang/Desktop/Auction Simulation/";
				
				evaluate(dir + aHalfFilename, dir + bHalfFilename, clusters, bw_full, bw_short, Features.DEFAULT_FEATURES_NOID, seed * 123 + 13579, seed * 123 + 24680, seed);
			}
		}
	}
	
//	private static void partition(String dir, String filename) {
//		List<String[]> rows = CsvManipulation.readWholeFile(Paths.get(dir, filename), false);
//		try {
//			BufferedWriter bwA = Files.newBufferedWriter(Paths.get(dir, "a_" + filename), Charset.defaultCharset());
//			BufferedWriter bwB = Files.newBufferedWriter(Paths.get(dir, "b_" + filename), Charset.defaultCharset());
//
//			Joiner joiner = Joiner.on(",");
//			
//			bwA.append(joiner.join(rows.get(0)));
//			bwA.newLine();
//			bwB.append(joiner.join(rows.get(0)));
//			bwB.newLine();
//			
//			for (int i = 1; i < rows.size(); i++) {
//				if (Math.random() < 0.5) {
//					bwA.append(joiner.join(rows.get(i)));
//					bwA.newLine();
//				} else {
//					bwB.append(joiner.join(rows.get(i)));
//					bwB.newLine();
//				}
//			}
//			bwA.flush();
//			bwB.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		
//		
//	}
	
	/**
	 * Calculates the number users with matching cluster assignments between users from tmFile and simFile.
	 * 
	 * @param tmFile string path of the file containing user features from users in Trade Me data; includes cluster assignments.
	 * @param simFile string path of the file containing user features from users in synthetic data; includes cluster assignments.
	 * @param numberOfClusters 
	 * @param bw_full
	 * @param bw_short
	 * @param featuresForClustering
	 * @param tmSeed
	 * @param simSeed
	 * @param runNumber
	 * @throws Exception
	 */
	public static void evaluate(String tmFile, String simFile, int numberOfClusters, BufferedWriter bw_full, BufferedWriter bw_short, 
			List<Features> featuresForClustering, int tmSeed, int simSeed, int runNumber) throws Exception {
		Instances tmIs = new DataSource(tmFile).getDataSet();
		System.out.println("files: " + tmFile + "," + simFile);
//		tmIs.randomize(new Random());
		tmIs.setClassIndex(tmIs.numAttributes() - 1); // the last attribute is the cluster assignment 
		
		Instances simIs = new DataSource(simFile).getDataSet();
//		simIs.randomize(new Random());
		simIs.setClassIndex(simIs.numAttributes() - 1);  // the last attribute is the cluster assignment

		// =====================================================
		// 				Build classifier with TM data
		// =====================================================
		Classifier classifier = buildIBk(tmIs, 1);
		double[][] correctMatrix = calculateCorrectMatrix(numberOfClusters, classifier, simIs);
		// =====================================================
		// 				Build classifier with Sim data
		// =====================================================
//		Classifier classifier = buildIBk(simIs, 1);
//		double[][] correctMatrix = calculateCorrectMatrix(numberOfClusters, classifier, tmIs);
		
//		double[][] correctMatrix = new double[numberOfClusters][numberOfClusters];
//		for (int i = 0; i < simIs.numInstances(); i++) {
//			Instance simInstance = simIs.instance(i);
//			double classification = classifier.classifyInstance(simInstance);
//			double cluster = simInstance.classValue();
//
//			// matrix before
////			if (classification != cluster) {
////				printMultiArray(correctMatrix);
////			}
//
//			// fill the matrix
//			correctMatrix[(int) cluster][(int) classification]++;
//
//			// matrix after
////			if (classification != cluster) {
////				System.out.println(i + ":" + classification + "," + instance.classValue());
////				printMultiArray(correctMatrix);
////			}
//		}
		
		// run the hungarian algorithm
		int[][] assignment = HungarianAlgorithm.hgAlgorithm(correctMatrix, "max");
		
		// print out class assignments
//		for (int i = 0; i < assignment.length; i++) {
//			int classification = assignment[i][0];
//			int cluster = assignment[i][1];
//			logger.warn("class " + classification + " should be paired with cluster " + cluster + " for highest accuracy.");
//		}
		
		// calculate number correct
		int correct = 0;
		for (int i = 0; i < assignment.length; i++) {
			correct += correctMatrix[assignment[i][0]][assignment[i][1]];
		}
		
//		logger.warn(multiArrayString(correctMatrix).toString());
		
//		logger.warn("Number of instances that had matching cluster and class: " + correct + " out of " + simIs.numInstances() + ".");
		writeResults(bw_full, bw_short, numberOfClusters, featuresForClustering, correctMatrix, assignment, correct, simIs.numInstances(), tmSeed, simSeed, runNumber);

//		logger.warn("Number of instances that had matching cluster and class: " + correct + " out of " + tmIs.numInstances() + ".");
//		writeResults(bw_full, bw_short, numberOfClusters, featuresForClustering, correctMatrix, assignment, correct, tmIs.numInstances());
		
	}

	public static double[][] calculateCorrectMatrix(int numberOfClusters, Classifier classifier, Instances instancesWithClass) throws Exception {
		double[][] correctMatrix = new double[numberOfClusters][numberOfClusters];
		for (int i = 0; i < instancesWithClass.numInstances(); i++) {
			Instance instance = instancesWithClass.instance(i);
			double classification = classifier.classifyInstance(instance);
			correctMatrix[(int) instance.classValue()][(int) classification]++;
		}
		return correctMatrix;
	}
	
	public static StringBuilder multiArrayString(double[][] matrix) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < matrix.length; i++) {
			sb.append(Arrays.toString(matrix[i]));
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		sb.append("]");
		return sb;
	}
	public static <T> StringBuilder multiArrayString(int[][] matrix) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < matrix.length; i++) {
			sb.append(Arrays.toString(matrix[i]));
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		sb.append("]");
		return sb;
	}
	
	public static J48 buildJ48(Instances is) throws Exception {
		return buildJ48(is, (int) (is.numInstances() * 0.01));
	}
	public static J48 buildJ48(Instances is, int minNumObject) throws Exception {
		J48 j48 = new J48();
		j48.setMinNumObj(minNumObject);
		j48.buildClassifier(is);
		System.out.println(j48.toString());

//		for (int i = 0; i < is.numInstances(); i++) {
//			Instance instance = is.instance(i);
//			double classification = j48.classifyInstance(instance);
//			System.out.println(i + ":" + classification + ", " + instance.classValue());
//			
//		}
		
		return j48;
	}
	
//	public static double[] classifyAll(Classifier classifier, Instances is) throws Exception {
//		double[] classifications = new double[is.numInstances()];
//		for (int i = 0; i < is.numInstances(); i++) {
//			Instance instance = is.instance(i);
//			double classification = classifier.classifyInstance(instance);
//			classifications[i] = classification;
//		}
//		return classifications;
//	}
	
	public static IBk buildIBk(Instances is, int numNearestNeighbours) throws Exception {
		IBk ibk = new IBk(numNearestNeighbours);
		ibk.buildClassifier(is);
		return ibk;
	}
	
}
