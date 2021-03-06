package createUserFeatures;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;

import createUserFeatures.BuildUserFeatures.BidObject;
import simulator.AuctionHouse;
import simulator.categories.ItemType;
import simulator.database.DBConnection;

/**
 *	Builds UserFeature object using scraped TradeMe data.
 */
public class BuildSimFeatures extends BuildUserFeatures{

	private static final Logger logger = Logger.getLogger(BuildSimFeatures.class);
	
	public static void main(String[] args) {
//		String features = "-0-1-1ln-2-2ln-3-3ln-10-4-4ln-5-6-6ln-11-9-12-8-13-14-15"; // all
//		String features = "-0-1ln-2ln-3ln-10-5-6-11-7-9-8";
//		String features = "-0-1ln-2ln-3ln-10-4ln-5-6-11-7-9-8";
//		String features = "-3ln-10-5-6ln-11";
		
//		List<Features> features = Features.DEFAULT_FEATURES;
		List<Features> features = Features.ALL_FEATURES;
		
		boolean trim = true;
		BuildSimFeatures bf = new BuildSimFeatures(trim);
//		writeToFile(bf.build().values(), features, Paths.get("BuildTrimmedSimFeatures20000_" + Features.fileLabels(features) + ".csv"));
		writeToFile(bf.build(new SimDBAuctionIterator(DBConnection.getConnection("auction_simulation"), true)).values(), features, Paths.get("aucSim_10k" + Features.fileLabels(features) + ".csv"));
//		writeToFile(bf.build(new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_20000"), true)).values(), features, Paths.get("BuildTrimmedSimFeatures20000_" + Features.fileLabels(features) + ".csv"));
//		writeToFile(bf.build(new SimDBAuctionIterator(DBConnection.getConnection("syn_normal_4000"), true)).values(), features, Paths.get("BuildTrimmedSimFeatures4000_" + Features.fileLabels(features) + ".csv"));
//		String features = "-0-1ln-2ln-3ln-10-5-6-11-7-9-8";
		System.out.println("Finished.");
	}
	
	public BuildSimFeatures(boolean trim) {
		this.trim = trim;
	}
	
	private static final long zeroTime = (long) 946684800 * 1000; // time since epoch at year 1/1/2000
	private static final long timeUnitMillis = AuctionHouse.UNIT_LENGTH * 60 * 1000;
	public static Date convertTimeunitToTimestamp(long timeUnit) {
		return new Date(zeroTime + timeUnit * timeUnitMillis);
	}
	
	public Map<Integer, UserFeatures> build(SimAuctionIterator simAuctionIterator) {
		Iterator<Pair<SimAuction, List<BidObject>>> it = simAuctionIterator.iterator();
		Map<Integer, ItemType> itemTypes = simAuctionIterator.itemTypes();
		while (it.hasNext()) {
			Pair<SimAuction, List<BidObject>> pair = it.next();
//			System.out.println("auction: " + pair.getKey() + ", bids: " + pair.getValue());
			processAuction(pair.getKey(), pair.getValue(), itemTypes);
		}
		
		for (UserObject user : simAuctionIterator.users().values()) {
			UserFeatures uf = this.userFeaturesMap.get(user.userId);
			if (uf == null)
				continue;
			uf.pos = user.posUnique;
			uf.neg = user.negUnique;
			uf.userType = user.userType;
		}
		
		return this.userFeaturesMap;
	}
//	private void userRep(Connection conn) throws SQLException {
//		PreparedStatement usersQuery = conn.prepareStatement(
//				"SELECT DISTINCT userId, posUnique, negUnique " +
//				"FROM users as u " +
//				";"
//				); 
//		ResultSet usersResultSet = usersQuery.executeQuery();
//		while (usersResultSet.next()) {
//			if (userFeaturesMap.containsKey(usersResultSet.getInt("userId")))
//				userFeaturesMap.get(usersResultSet.getInt("userId")).setRep(usersResultSet.getInt("posUnique"), usersResultSet.getInt("negUnique"));
//		}
//	}

	/**
	 * Uses information from the database to construct a set of user features for each user.
	 * Information is stored in class variable "userFeaturesMap".  Map is in ascending key
	 * order. Key is the userId.
	 */
	public Map<Integer, UserFeatures> build() {
		try {
			Connection conn = DBConnection.getSimulationConnection();
			Map<Integer, UserFeatures> result = build(new SimDBAuctionIterator(conn, trim));
			conn.close();
			return result;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void processAuction(SimAuction auction, List<BidObject> list, Map<Integer, ItemType> itemtypes) {
		super.processAuction(auction, list);
		
		// for recording bid amounts as a proportion of true valuation
		// this value can only be calculated for SIM-Auctions, since true valuation is not known for TM-Auctions.
		for (BidObject bid : list) {
			double proportionOfValuation = (double) bid.amount/itemtypes.get(auction.itemTypeId).getTrueValuation();
			userFeaturesMap.get(bid.bidderId).getBidAmountComparedToValuation().add(proportionOfValuation);
		}
	}
}
