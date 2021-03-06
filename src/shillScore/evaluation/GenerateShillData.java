package shillScore.evaluation;

import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multiset;

import createUserFeatures.BuildSimFeatures;
import createUserFeatures.Features;
import createUserFeatures.SimDBAuctionIterator;
import createUserFeatures.SimAuctionIterator;
import createUserFeatures.SimMemoryAuctionIterator;
import createUserFeatures.UserFeatures;
import agents.repFraud.MultipleRepFraud;
import agents.repFraud.SingleRepFraud;
import agents.repFraud.SingleRepFraud;
import agents.shills.Hybrid;
import agents.shills.HybridT;
import agents.shills.HybridTVaryCollusion;
import agents.shills.LowBidShillPair;
import agents.shills.HybridLowPrice;
import agents.shills.RandomHybrid;
import agents.shills.SimpleShillPair;
import agents.shills.puppets.PuppetClusterBidderCombined;
import agents.shills.puppets.PuppetFactoryI;
import agents.shills.strategies.LowPriceStrategy;
import agents.shills.strategies.LateStartTrevathanStrategy;
import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;
import agents.shills.strategies.WaitStartStrategy;
import shillScore.BuildCollusiveShillScore;
import shillScore.BuildShillScore;
import shillScore.CollusiveShillScore;
import shillScore.ShillScore;
import shillScore.WriteScores;
import shillScore.BuildShillScore.ShillScoreInfo;
import shillScore.CollusiveShillScore.ScoreType;
import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.Simulation;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.database.DBConnection;
import simulator.database.KeepObjectsInMemory;
import simulator.database.SaveToDatabase;
import simulator.database.SimulationCreateTableStmts;
import simulator.records.UserRecord;
import util.Util;

public class GenerateShillData {

	public static void main(String[] args) {
		// strategies
		Strategy travethan = new TrevathanStrategy(0.95, 0.85, 0.85);
		Strategy lateStart = new LateStartTrevathanStrategy(0.95, 0.85, 0.85);
		Strategy lowPrice = new LowPriceStrategy();
		Strategy waitStart = new WaitStartStrategy(0.95, 0.85, 0.85);

		// adders
		AgentAdder simplePairAdderA = SimpleShillPair.getAgentAdder(20, travethan); // can use 20, since each submits 10 auctions.
		AgentAdder simplePairAdderB = SimpleShillPair.getAgentAdder(20, lateStart);
		AgentAdder simplePairAdderC = LowBidShillPair.getAgentAdder(20, travethan, lowPrice);
		AgentAdder simplePairAdderD = SimpleShillPair.getAgentAdder(20, waitStart);
		AgentAdder hybridAdderA = Hybrid.getAgentAdder(5, travethan, 4); // use only 5 groups, since each group submits 40 auctions. if too many will affect normal auctions too much. 
		AgentAdder hybridAdderB = Hybrid.getAgentAdder(1, waitStart, 4);
		AgentAdder hybridAdderTVC = HybridTVaryCollusion.getAgentAdder(5, lateStart);
		AgentAdder hybridAdderC = HybridLowPrice.getAgentAdder(5, lateStart, lowPrice);
		AgentAdder randomHybridAdderA = RandomHybrid.getAgentAdder(5, travethan, 4);
		AgentAdder hybridAdderD = HybridT.getAgentAdder(10, lateStart, PuppetClusterBidderCombined.getFactory());
		
		AgentAdder repFraudA = SingleRepFraud.getAgentAdder(1, 20);
		AgentAdder repFraudB = MultipleRepFraud.getAgentAdder(1, 10, 20);
		
		int numberOfRuns = 1;
		
		for (int i = 1; i < 30; i++) {
			runWithName(simplePairAdderA, "gen_simple", i);
			runWithName(simplePairAdderB, "gen_lateStart", i);
			runWithName(simplePairAdderC, "gen_lowBid", i);
		}
//		writeSSandPercentiles(simplePairAdderA, numberOfRuns, new double[]{1,1,1,1,1,1});
//		run(simplePairAdderA, numberOfRuns);
//		run(repFraudA, 1);
//		run(doNothingAdder, numberOfRuns);
//		run(hybridAdderD, numberOfRuns);
//		run(simplePairAdderA, numberOfRuns, new double[]{0.0820,0.0049,-0.0319,0.5041,0.2407,0.2003});
//		writeSSandPercentiles(simplePairAdderB, numberOfRuns);
//		writeSSandPercentiles(simplePairAdderC, numberOfRuns);
		
//		buildFeaturesAndSSFromDB("shillingResults/trevathanALL", "auction_simulation_simple", simplePairAdderA, 30);

//		collusiveShillPairMultipleRuns(hybridAdderB, numberOfRuns);
//		buildFeaturesAndSSFromDB("shillingResults/hybridlp", "syn_hybridlp_", hybridAdderC, 30);
//		buildFeaturesAndSSFromDB("shillingResults/hybridtvc", "syn_hybridtvc_", hybridAdderTVC, 30);
//		buildFeaturesAndSSFromDB("shillingResults/hybridnormal", "syn_hybridLP_", hybridAdderD, 30);
//		collusiveShillPairMultipleRuns(randomHybridAdderA, numberOfRuns);
//		collusiveShillPairMultipleRuns(multisellerHybridAdderA, numberOfRuns);
//		collusiveShillPairMultipleRuns(hybridAdderC, numberOfRuns);
//		collusiveShillPairMultipleRuns(nonAltHybridA, numberOfRuns);11
	}
	
	public static final AgentAdder doNothingAdder = new AgentAdder() {
		@Override
		public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur,
				ArrayList<ItemType> types) {
		}
		
		@Override
		public String toString() {
			return "NoAdder";
		}
	};
	
	public static void runWithName(AgentAdder adder, String runName, int runNumber) {
		String databaseName = runName + "_"+ runNumber;
		System.out.println("running: " + databaseName);
		DBConnection.createDatabase(databaseName);
		SimulationCreateTableStmts.createSimulationTables(databaseName);
		Simulation.run(SaveToDatabase.instance(databaseName), adder);
	}
	
	public static void run(AgentAdder adder, int numberOfRuns, double[]... weightSets) {
		for (int runNumber = 0; runNumber < numberOfRuns; runNumber++) {
			System.out.println("starting run " + runNumber);
			
//			List<Features> featuresSelected = Features.defaultFeatures;
			List<Features> featuresSelected = Features.FEATURES_FOR_DT;
//			List<Features> featuresSelected = Features.ALL_FEATURES;

//			KeepObjectsInMemory objInMem = KeepObjectsInMemory.instance();
//			SimAuctionIterator simAuctionIterator = new SimMemoryAuctionIterator(objInMem, true);
//			Main.run(objInMem, adder); // run simulator
//			Map<Integer, UserFeatures> userFeatures = new BuildSimFeatures(true).build(simAuctionIterator); // build features
			
			SimAuctionIterator simAuctionIterator = new SimDBAuctionIterator(DBConnection.getSimulationConnection(), true);
			Simulation.run(SaveToDatabase.instance(), adder);
			Map<Integer, UserFeatures> userFeatures = new BuildSimFeatures(true).build(simAuctionIterator);
			
			// write results to file
//			BuildSimFeatures.writeToFile(userFeatures.values(), // write features
//					featuresSelected, 
//					Paths.get("single_feature_shillvsnormal", "syn_" + adder + "_" + Features.fileLabels(featuresSelected) + "_" + runNumber + ".csv")
//					);
			writeSSandPercentiles(simAuctionIterator, adder, runNumber, weightSets); // build and write shill scores
			
//			return;
		}
	}
	
	/**
	 * Reads from the database to build csv files with features and shill score ratings.
	 */
	public static void buildFeaturesAndSSFromDB(String outputDirectory, String databasePrefix, AgentAdder adder, int numberOfRuns, double[]... weightSets) {
		for (int runNumber = 0; runNumber < numberOfRuns; runNumber++) {
			System.out.println("Reading run " + runNumber);
			
			List<Features> featuresSelected = Features.ALL_FEATURES;

			Connection conn = DBConnection.getConnection(databasePrefix + runNumber);
			SimAuctionIterator simAuctionIterator = new SimDBAuctionIterator(conn, true);
			Map<Integer, UserFeatures> userFeatures = new BuildSimFeatures(true).build(simAuctionIterator); // build features
			
			// write results to file
			BuildSimFeatures.writeToFile(userFeatures.values(), // write features
					featuresSelected, 
//					Paths.get("single_feature_shillvsnormal", "syn_" + adder + "_" + Features.fileLabels(featuresSelected) + "_" + runNumber + ".csv")
					Paths.get(outputDirectory, "syn_" + adder + "_" + Features.fileLabels(featuresSelected) + "_" + runNumber + ".csv")
					);
			writeSSandPercentiles(outputDirectory, simAuctionIterator, adder, runNumber, weightSets); // build and write shill scores
		}
	}
	
	private static void writeSSandPercentiles(SimAuctionIterator simAuctionIterator, AgentAdder adder, int runNumber, double[]... weightSets) {
		writeSSandPercentiles("shillingResults/comparisons", simAuctionIterator, adder, runNumber, weightSets);
	}

	/**
	 * Calculate shill scores for synthetic data.
	 * Write those scores out to files, and also the ssPercentiles. 
	 * @param adder
	 * @param runNumber
	 * @param weightSets
	 */
	private static void writeSSandPercentiles(String outputFolder, SimAuctionIterator simAuctionIterator, AgentAdder adder, int runNumber, double[]... weightSets) {
		String runLabel = adder.toString() + "." + runNumber;

		// build shillScores
		ShillScoreInfo ssi = BuildShillScore.build(simAuctionIterator);
		
		// write out shill scores
		WriteScores.writeShillScores(outputFolder, ssi.shillScores, ssi.auctionCounts, runLabel, weightSets);
		
		// write out how many wins/losses by shills, and the normalised final price compared to non-shill auctions
		ShillWinLossPrice.writeToFile(simAuctionIterator, runLabel);
		
		List<List<Double>> ssPercentiless = new ArrayList<List<Double>>();
		ssPercentiless.add(splitAndCalculatePercentiles(ssi.shillScores.values(), ssi.auctionCounts, ShillScore.DEFAULT_WEIGHTS));
		for (double[] weights : weightSets) {// calculate the percentiles for the other weight sets
			ssPercentiless.add(splitAndCalculatePercentiles(ssi.shillScores.values(), ssi.auctionCounts, weights));
		}
		String ssPercentilesRunLabel = runLabel;
		for (double[] weights : weightSets) {
			ssPercentilesRunLabel += "." + Arrays.toString(weights).replaceAll(", " , "");
		}
		
		// write out percentiles
		ShillVsNormalSS.writePercentiles(Paths.get(outputFolder, "ssPercentiles-" + adder + ".csv"), ssPercentilesRunLabel, ssPercentiless);
		
		// write out the number of shill auctions for which the shill had the highest (or not highest) SS
		ShillVsNormalSS.ssRankForShills(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, simAuctionIterator.users(), Paths.get(outputFolder, "rank.csv"), runLabel, weightSets);
		
//		WriteScores.writeShillScoresForAuctions(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, runLabel);
	}
	
	public static List<Double> splitAndCalculatePercentiles(Collection<ShillScore> sss, Multiset<Integer> auctionCounts, double[] weights) {
		List<Double> shillSS = new ArrayList<>();
		List<Double> normalSS = new ArrayList<>();
//		int fraudCount = 0;
		for (ShillScore ss : sss) { // sort SS for shills and normals into different lists
			double shillScore = ss.getShillScore(auctionCounts, weights);
			if (Double.isNaN(shillScore)) // skip users who have an uncomputable shillscore
				continue;
			if (ss.userType.toLowerCase().contains("puppet")) { 
//				fraudCount++;
//				System.out.println("ss: " + ss.getId() + "-" + shillScore);
				shillSS.add(shillScore);
			} else {
				normalSS.add(shillScore);
			}
		}
//		System.out.println("fCount: " + fraudCount);
		return Util.percentiles(normalSS, shillSS);
	}
	
	private static void collusiveShillPairMultipleRuns(AgentAdder adder, int numberOfRuns) {
		for (int i = 0; i < numberOfRuns; i++) {
			System.out.println("starting run " + i);
			
			// run the simulator with the adder
			Simulation.run(SaveToDatabase.instance(), adder);
			
			String runLabel = adder.toString() + "." + i;

			Connection conn = DBConnection.getSimulationConnection();
			SimAuctionIterator simIt = new SimDBAuctionIterator(conn, true);

			// build shillScores
			ShillScoreInfo ssi = BuildShillScore.build(simIt);
			Map<Integer, CollusiveShillScore> css = BuildCollusiveShillScore.build(ssi);

			// write out shill scores
			WriteScores.writeShillScores(ssi.shillScores, ssi.auctionCounts, runLabel);
			WriteScores.writeCollusiveShillScore(ssi.shillScores, css, runLabel);
			
			// write out how many wins/losses by shills, and the normalised final price compared to non-shill auctions
			ShillWinLossPrice.writeToFile(simIt, runLabel);
			
			
			List<Double> ssPercentiles = BuildCollusiveShillScore.getPercentiles(ScoreType.Hybrid, ssi, css, runLabel);
			// write out percentiles
			ShillVsNormalSS.writePercentiles(Paths.get("shillingResults", "comparisons", "cssPercentiles.csv"), runLabel, Collections.singletonList(ssPercentiles));
			
//			WriteScores.writeShillScoresForAuctions(ssi.shillScores, ssi.auctionBidders, ssi.auctionCounts, runLabel);
		}
	}

}
