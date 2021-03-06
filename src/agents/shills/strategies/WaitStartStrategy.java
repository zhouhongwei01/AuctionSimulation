package agents.shills.strategies;

import java.util.List;
import java.util.Random;

import com.sun.istack.internal.logging.Logger;

import simulator.objects.Auction;
import simulator.objects.Bid;

/**
 *	Similar to TrevathanStrategy, except starts bidding later, and makes bids that are not always the minimum required.
 *	Does not start bidding of no one else has bid. Therefore, may not bid at all in an auction (by a collaborating seller).
 */
public class WaitStartStrategy implements Strategy {

	private static final Logger logger = Logger.getLogger(WaitStartStrategy.class);
	
	private final double theta;
	private final double alpha;
	private final double mu;
	
	private final Random r = new Random();
	
	public WaitStartStrategy(double theta, double alpha, double mu) {
		this.theta = theta;
		this.alpha = alpha;
		this.mu = mu;
	}
	
	@Override
	public long wait(Auction auction) {
		return r.nextInt(50) + 50;
	}

	@Override
	public boolean shouldBid(Auction shillAuction, long currentTime) {
		if (shillAuction.hasNoBids()) { // wait if no one else has bid yet, don't bid until 70% of auction has elapsed
			return false;
		}
		
		boolean d3success = directive3(theta, shillAuction, currentTime);
		boolean d4success = directive4(alpha, shillAuction);
		boolean d5success = directive5(mu, theta, currentTime, shillAuction);
		return d3success && d4success && d5success;
	}

	@Override
	/**
	 * Bid the minimum amount possible.
	 */
	public int bidAmount(Auction auction) {
		int amount = auction.minimumBid();
		if (r.nextDouble() > 0.98) {
			int change = Math.max(0, (int) ((1 - auction.proportionOfTrueValuation()) * 0.1 * auction.trueValue()));
			amount += change;
		}
		return amount;
	}
	
	/**
	 * D3 of Simple Shilling Agent by Trevathan; don't bid too close to auction end
	 */
	private static boolean directive3(double theta, Auction auction, long time) {
		double proportionRemaining = proportionRemaining(auction.getStartTime(), auction.getEndTime(), time);
//		System.out.println("proportionRemaining: " + proportionRemaining);
		return proportionRemaining <= theta;
	}
	
	/**
	 * D4 of Simple Shilling Agent by Trevathan; bid until target price is reached
	 */
	private static boolean directive4(double alpha, Auction auction) {
		double proportionPrice = auction.getCurrentPrice() / auction.trueValue();
//		System.out.println("proportionPrice: " + proportionPrice);
		return alpha > proportionPrice;
	}
	
	/**
	 * D5 of Simple Shilling Agent by Trevathan; bid when bid volume is high
	 * @param mu
	 * @param auction
	 * @return true if should bid, else false
	 */
	private static boolean directive5(double mu, double theta, long currentTime, Auction auction) {
		if (auction.getBidCount() == 0)
			return true;
		
		double muTime = proportionToTime(1 - mu, auction.getStartTime(), currentTime); // D5, how far back to look when counting bids
		double thetaTime = proportionToTime(theta, auction.getStartTime(), auction.getEndTime()); // D3, don't bid too close to end
		int numberOfBids = numberOfBids(auction.getBidHistory(), (long) (muTime + 0.5));
//		System.out.println("numberOfBids: " + numberOfBids);
		if (numberOfBids > 1) {
			return true;
		} else if (numberOfBids == 1) {
			// find the proportion of time time left available for the shill bidder to act
			double normalisedTime = proportionRemaining(auction.getStartTime(), thetaTime, currentTime);
//			System.out.println("normalisedTime: " + normalisedTime);
			if (normalisedTime < 0.85)
				return true;
		}	
		return false;
	}

	/**
	 * Counts the number of bids that were made after the timeLimit by bidders
	 * who are not shill bidders from this controller.
	 * @param bidHistory
	 * @param time
	 */
	private static int numberOfBids(List<Bid> bidHistory, long timeLimit) {
		int count = 0;
		for (int i = bidHistory.size() - 1; i >= 0 && bidHistory.get(i).getTime() >= timeLimit; i--) {
			count++;
		}
		return count;
	}
	
	private static double proportionRemaining(long start, double end, long current) {
		return ((double) current - start)/(end - start);
	}
	
	private static double proportionToTime(double proportion, long start, long end) {
		return proportion * (end - start) + start;  
	}
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "." + theta + "." + alpha + "." + mu;
	}

}
