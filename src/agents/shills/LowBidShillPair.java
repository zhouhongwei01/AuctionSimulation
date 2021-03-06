package agents.shills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.ArrayListMultimap;

import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.buffers.ItemSender.ItemSold;
import simulator.buffers.PaymentSender.Payment;
import simulator.categories.ItemType;
import simulator.objects.Auction;
import simulator.objects.Feedback;
import simulator.objects.ItemCondition;
import simulator.objects.Feedback.Val;
import simulator.records.UserRecord;
import agents.EventListener;
import agents.SimpleUserI;
import agents.shills.puppets.Puppet;
import agents.shills.puppets.PuppetI;
import agents.shills.strategies.Strategy;

public class LowBidShillPair extends EventListener implements Controller {
	
	private static final Logger logger = Logger.getLogger(LowBidShillPair.class); 
	
	protected BufferHolder bh;
	protected PaymentSender ps;
	protected ItemSender is;
	protected AuctionHouse ah;
	
	private final Puppet ss;
	private final Puppet sb;
	// Map<Auction, Registered>
	private final Map<Auction, Boolean> shillAuctions;
	private final Set<Auction> expiredShillAuctions;
	List<ItemType> types;

	private final Strategy strategy1; // strategy for shill auctions
	private final Strategy strategy2; // strategy for auction sniping
	
//	private final Random r;
	
	private int winCount = 0;
	private int shillWinCount = 0;
	private int lossCount = 0;
	private int shillLossCount = 0;
	
	public LowBidShillPair(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, List<ItemType> types, 
			Strategy strategy1, Strategy strategy2) {
		super(bh);
		this.bh = bh;
		this.ps = ps;
		this.is = is;
		this.ah = ah;
		this.types = types;
		
		// set up the shill seller
		Puppet ss = new Puppet(bh, ps, is, ah, this, types);
//		SimpleUser ss = new SimpleUser(bh, ps, is, ah);
		ur.addUser(ss);
		this.ss = ss;
		
		// set up the shill bidder
//		SimpleUser sb = new SimpleUser(bh, ps, is, ah);
		Puppet sb = new Puppet(bh, ps, is, ah, this, types);
		ur.addUser(sb);
		this.sb = sb;
	
		shillAuctions = new HashMap<>();
		expiredShillAuctions = new HashSet<>();
		
		setNumberOfAuctions(10);

		this.strategy1 = strategy1;
		this.strategy2 = strategy2;
		
//		r = new Random();
		
		ah.registerForSniping(this);
	}
	
//	private final Set<Auction> shillAuctionsFirstNo = new HashSet<>();
	private final Set<Auction> waiting = new HashSet<>();
	private final ArrayListMultimap<Long, Auction> futureBid = ArrayListMultimap.create();
	@Override
	public void run() {
		super.run();
		
		long currentTime = bh.getTime();
		
		// TODO: can make another set "toCheckShillAuctions" which only needs to be checked
		// after that auction receives a message about a new bid.
		
		// look through the shill auctions to see if any require action
		for (Auction shillAuction : shillAuctions.keySet()) {
			if (shillAuctions.get(shillAuction) == false || shillAuction.getWinner() == sb)
				continue;

			if (waiting.contains(shillAuction))
				continue;
			
//			if (someoneJustBid == true) {
//				System.out.println("are you bidding?");
//				someoneJustBid = false;
//			}

			if (this.strategy1.shouldBid(shillAuction, currentTime)) {
				long wait = strategy1.wait(shillAuction); 
				if (wait > 0) {
					// record when to bid in the future
					waiting.add(shillAuction);
					futureBid.put(currentTime + wait, shillAuction);
				} else {
					sb.makeBid(shillAuction, this.strategy1.bidAmount(shillAuction));
				}
			}
		}
		
		// submit a bid for auctions that have finished waiting
		List<Auction> finishedWaiting = futureBid.removeAll(currentTime);
		if (finishedWaiting != null) {
			for (Auction shillAuction : finishedWaiting) {
				sb.makeBid(shillAuction, this.strategy1.bidAmount(shillAuction));
			}
			this.waiting.removeAll(finishedWaiting);
		}
		
		// submit a new auction
		if (!auctionTimes.isEmpty() && auctionTimes.get(auctionTimes.size() - 1) == currentTime) {
			Auction newShillAuction = ss.submitAuction();
			shillAuctions.put(newShillAuction, false);
			auctionTimes.remove(auctionTimes.size() - 1);
		}
	}
	
	private List<Integer> auctionTimes;
	public final Random r = new Random();
	private void setNumberOfAuctions(int numberOfAuctions) {
		auctionTimes = new ArrayList<>();
		int latest = (AuctionHouse.HUNDRED_DAYS - AuctionHouse.SEVEN_DAYS) / AuctionHouse.UNIT_LENGTH;;
		for (int i = 0; i < numberOfAuctions; i++) {
			auctionTimes.add(r.nextInt(latest));
		}
		Collections.sort(auctionTimes, Collections.reverseOrder());
	}
	
	public Set<Auction> getShillAuctions() {
		return this.shillAuctions.keySet();
	}

	@Override
	public void newAction(Auction auction, int time) {
		super.newAction(auction, time);
		if (shillAuctions.containsKey(auction)) {
			ah.registerForAuction(this, auction);
			shillAuctions.put(auction, true);
		}
	}

	boolean someoneJustBid; 
	@Override
	public void priceChangeAction(Auction auction, int time) {
		super.priceChangeAction(auction, time);
		if (auction.getWinner() != sb)
			someoneJustBid = true;
	}

	@Override
	public void lossAction(Auction auction, int time) {
		logger.debug("Shill auction " + auction + " has expired. Removing.");
		boolean removed = shillAuctions.remove(auction);
		assert removed;
		boolean isNew = expiredShillAuctions.add(auction);
		assert isNew;
	}

	
	/**
	 * This class is a controller and can never bid, and so, can never win.
	 * This method will never be called.
	 */
	@Override
	public void winAction(Auction auction, int time) {
		super.winAction(auction, time);
		assert(false) : "This method should never be called, since this class can never bid/win.";
	}

	@Override
	public void endSoonAction(Auction auction, int time) {
		super.endSoonAction(auction, time);
		
		if (!shouldSnipe(auction))
			return;
		
		if (this.strategy2.shouldBid(auction, bh.getTime())) {
//			System.out.println(sb + " is making bid on " + auction + " at " + bh.getTime() + ".");
			this.sb.makeBid(auction, strategy2.bidAmount(auction));
		}
	}

	protected boolean shouldSnipe(Auction auction) {
		if (!shillAuctions.containsKey(auction)) {
//			System.out.println(winCount + ", " +  shillWinCount + ", " + lossCount + ", " + shillLossCount + " " + (winCount + shillWinCount + 3 < lossCount + shillLossCount));
//			if (winCount + shillWinCount + 3 < shillLossCount) 
//				System.out.println("pause");
//			System.out.println(winCount + " " + shillLossCount);
//			if (winCount < shillLossCount)
//				System.out.println("shouldSnipe: " + (winCount < shillLossCount));
//			if (winCount + shillWinCount < shillLossCount)
//				System.out.println("shouldSnipe2: " + (winCount + shillWinCount < shillLossCount));
			return winCount < shillLossCount;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return super.toString() + ":" + strategy1.toString(); 
	}

	public static AgentAdder getAgentAdder(final int numberOfGroups, final Strategy strategy1, final Strategy strategy2) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					LowBidShillPair sc = new LowBidShillPair(bh, ps, is, ah, ur, types, strategy1, strategy2);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "ModifiedShillPair." + numberOfGroups + "." + strategy1.toString() + "." + strategy2.toString();
			}
		};
	}

	@Override
	public void winAction(SimpleUserI agent, Auction auction) {
//		System.out.println(agent + " won the auction : " + auction);
		assert agent == sb;
		if (shillAuctions.containsKey(auction)) {
			shillWinCount++;
		} else {
			winCount++;
		}
	}

	@Override
	public void lossAction(SimpleUserI agent, Auction auction) {
//		System.out.println(agent + " lost the auction : " + auction);
		assert agent == sb;
		if (auction.getSeller() == ss)
			shillLossCount++;
		else
			lossCount++;
	}

	@Override
	public boolean isFraud(Auction auction) {
		return shillAuctions.containsKey(auction);
	}

	@Override
	public void soldAction(SimpleUserI agent, Auction auction) {
		if (isFraud(auction)) {
//			bh.getFeedbackToAh().put(new Feedback(Val.POS, agent, auction));
		} else {
			this.awaitingPayment.add(auction);
		}
	}

	@Override
	public void expiredAction(SimpleUserI agent, Auction auction) {
	}

	@Override
	public void gotPaidAction(SimpleUserI agent, Collection<Payment> paymentSet) {
	}

	@Override
	public void itemReceivedAction(PuppetI agent, Set<ItemSold> itemSet) {
	}

	@Override
	public void endSoonAction(PuppetI agent, Auction auction) {
	}
	
}
