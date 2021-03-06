package shillScore.evaluation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

import simulator.categories.ItemType;
import simulator.database.DBConnection;
import util.IncrementalMean;
import createUserFeatures.BuildUserFeatures.BidObject;
import createUserFeatures.BuildUserFeatures.SimAuction;
import createUserFeatures.BuildUserFeatures.UserObject;
import createUserFeatures.SimAuctionIterator;


public class ShillWinLossPrice {

	/**
	 * Calculates the average final sale price of auctions with and without shills, and the
	 * number of wins and losses by shills.
	 * Used to measure see whether the shills actually do their job of raising the final price,
	 * and see how often the shills win the auctions.
	 * 
	 * Written to <code>shillingResults/comparisons/shillWinLossCounts.csv<code>. 
	 * @param simAuctionIterator 
	 */
	public static void writeToFile(SimAuctionIterator simAuctionIterator, String label) {
		Map<Integer, ItemType> itemTypes = simAuctionIterator.itemTypes();
		
		IncrementalMean shillWinAvg = new IncrementalMean();
		IncrementalMean shillLossAvg = new IncrementalMean();
		IncrementalMean nonShillShillWinAvg = new IncrementalMean();
		IncrementalMean nonShillNonShillWinAvg = new IncrementalMean();

		Map<Integer, UserObject> users = simAuctionIterator.users();
		
		Iterator<Pair<SimAuction, List<BidObject>>> it = simAuctionIterator.iterator();
		while (it.hasNext()) {
			Pair<SimAuction, List<BidObject>> pair = it.next();
			SimAuction auction = pair.getKey();
			String sellerType = users.get(auction.sellerId).userType;
			String bidderType = users.get(auction.winnerId).userType;
			boolean sellerIsShill = sellerType.toLowerCase().contains("puppet");
			boolean winnerIsShill = bidderType.toLowerCase().contains("puppet");
			if (sellerIsShill && winnerIsShill) { // seller is shill && winner is shill
				shillWinAvg.add(ratio(pair, itemTypes));
			} else if (sellerIsShill && !winnerIsShill) { // seller is shill && winner is shill
				shillLossAvg.add(ratio(pair, itemTypes));
			} else if (!sellerIsShill && winnerIsShill) { // seller is shill && winner is shill
				nonShillShillWinAvg.add(ratio(pair, itemTypes));
			} else if (!sellerIsShill && !winnerIsShill) { // seller is shill && winner is shill
				nonShillNonShillWinAvg.add(ratio(pair, itemTypes));
			}
		}
		
		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults", "comparisons", "winLossCounts.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)){
			bw.append(new Date().toString());
			bw.append(",");
			bw.append(label);
			bw.append(",");
			
			// number of auctions won by the shill, and the price
			bw.append(shillWinAvg.numElements() + ",").append(shillWinAvg.average() + ",");
			
			// number of auctions won by a non-shill, and the price
			bw.append(shillLossAvg.numElements() + "," + shillLossAvg.average() + ",");
			
			// number of non-shill auctions won by the shill, and the price
			bw.append(nonShillShillWinAvg.numElements() + "," + nonShillShillWinAvg.average() + ",");
			
			// number of non-shill auctions won by a non-shill, and the price
			bw.append(nonShillNonShillWinAvg.numElements() + "," + nonShillNonShillWinAvg.average());
			
			bw.newLine();
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
//	public static void writeToFile(SimAuctionIterator simAuctionIterator, String label) {
//		try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("shillingResults", "comparisons", "winLossCounts.csv"), Charset.defaultCharset(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)){
//
//			bw.append(new Date().toString());
//			bw.append(",");
//			bw.append(label);
//			bw.append(",");
//			
//			try {
//				// number of auctions won by the shill, and the price
//				IncrementalMean shillWinAvg = findShillWinAuctions();
//				bw.append(shillWinAvg.getNumElements() + "," + shillWinAvg.getAverage() + ",");
//
//				// number of auctions lost by the shill, and the price
//				IncrementalMean shillLossAvg = findShillLossAuctions();
//				bw.append(shillLossAvg.getNumElements() + "," + shillLossAvg.getAverage() + ",");
//
//				// number of non-shill auctions lost by the shill, and the price
//				IncrementalMean nonShillShillWinAvg = findNonShillAuctionsWinByShills();
//				bw.append(nonShillShillWinAvg.getNumElements() + "," + nonShillShillWinAvg.getAverage() + ",");
//
//				// number of non-shill auctions won by a non-shill, and the price
//				IncrementalMean nonShillNonShillWinAvg = findNonShillAuctionsWinsByNonShills();
//				bw.append(nonShillNonShillWinAvg.getNumElements() + "," + nonShillNonShillWinAvg.getAverage());
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//			
//			
//			bw.newLine();
//			bw.flush();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	public static void main(String[] args) {
		
	}
	
	private static double ratio(Pair<SimAuction, List<BidObject>> pair, Map<Integer, ItemType> itemTypes) {
		return (double) pair.getValue().get(pair.getValue().size() - 1).amount / itemTypes.get(pair.getKey().itemTypeId).getTrueValuation();
	}
	
	private static IncrementalMean findShillWinAuctions() throws SQLException {
		IncrementalMean incAvg = new IncrementalMean();
		Connection conn = DBConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice " +  
				"FROM auctions as a " +
				"JOIN itemtypes as i ON a.itemTypeId=i.id " +
				"JOIN bids as b ON a.listingId=b.listingId " +
				"JOIN users as seller ON seller.userId=a.sellerId " +
				"JOIN users as bidder ON bidder.userId=a.winnerId " +  
				"WHERE seller.userType LIKE '%Puppet%' AND bidder.userType LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");
			
			double ratio = (double) finalPrice / trueValuation;
			incAvg.add(ratio);
		}
		return incAvg;
	}

	/**
	 * Find the number of shill auctions lost by shills, and the average finalPrice/trueValuation ratio
	 * for those auctions.
	 */
	private static IncrementalMean findShillLossAuctions() throws SQLException {
		IncrementalMean incAvg = new IncrementalMean();
		Connection conn = DBConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice " +  
				"FROM auctions as a " +
				"JOIN itemtypes as i ON a.itemTypeId=i.id " +
				"JOIN bids as b ON a.listingId=b.listingId " +
				"JOIN users as seller ON seller.userId=a.sellerId " +
				"JOIN users as bidder ON bidder.userId=a.winnerId " +  
				"WHERE seller.userType LIKE '%Puppet%' AND bidder.userType NOT LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");
			
			double ratio = (double) finalPrice / trueValuation;
			incAvg.add(ratio);
		}
		return incAvg;
	}
	
	private static IncrementalMean findNonShillAuctionsWinByShills() throws SQLException {
		IncrementalMean incAvg = new IncrementalMean();
		Connection conn = DBConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice, seller.userType as sellerType, bidder.userType as winnerType " + 
				"FROM auctions as a " +
				"JOIN itemtypes as i ON a.itemTypeId=i.id " +
				"JOIN bids as b ON a.listingId=b.listingId " +
				"JOIN users as seller ON seller.userId=a.sellerId " +
				"JOIN users as bidder ON bidder.userId=a.winnerId " +
				"WHERE seller.userType NOT LIKE '%Puppet%' AND bidder.userType LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");

			double ratio = (double) finalPrice / trueValuation;
			incAvg.add(ratio);
		}
		return incAvg;
	}
	
	/**
	 * Find the number of non-shill auctions, and the average finalPrice/trueValuation ratio
	 * for those auctions.
	 */
	private static IncrementalMean findNonShillAuctionsWinsByNonShills() throws SQLException {
		IncrementalMean incAvg = new IncrementalMean();
		Connection conn = DBConnection.getSimulationConnection();
		CallableStatement stmt = conn.prepareCall("SELECT a.winnerId, a.listingId, itemTypeId, trueValuation, MAX(b.amount) as winningPrice, seller.userType as sellerType, bidder.userType as winnerType " +
				"FROM auctions as a " +
				"JOIN itemtypes as i ON a.itemTypeId=i.id " +
				"JOIN bids as b ON a.listingId=b.listingId " +
				"JOIN users as seller ON seller.userId=a.sellerId " +
				"JOIN users as bidder ON bidder.userId=a.winnerId " +
				"WHERE seller.userType NOT LIKE '%Puppet%' AND bidder.userType NOT LIKE '%Puppet%' GROUP BY a.listingId;");
		ResultSet rs = stmt.executeQuery();
		while (rs.next()) {
//			int winnerId = rs.getInt("winnerId");
			int finalPrice = rs.getInt("winningPrice");
			int trueValuation = rs.getInt("trueValuation");

			double ratio = (double) finalPrice / trueValuation;
			incAvg.add(ratio);
		}
		return incAvg;
	}
	
}
