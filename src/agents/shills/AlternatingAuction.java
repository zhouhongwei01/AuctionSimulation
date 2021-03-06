package agents.shills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import agents.SimpleUserI;
import agents.shills.puppets.Puppet;
import agents.shills.puppets.PuppetFactoryI;
import agents.shills.puppets.PuppetI;
import agents.shills.strategies.Strategy;
import agents.shills.strategies.TrevathanStrategy;


import simulator.AgentAdder;
import simulator.AuctionHouse;
import simulator.Simulation;
import simulator.buffers.BufferHolder;
import simulator.buffers.ItemSender;
import simulator.buffers.PaymentSender;
import simulator.categories.ItemType;
import simulator.database.SaveToDatabase;
import simulator.objects.Auction;
import simulator.records.UserRecord;

public class AlternatingAuction extends CollusiveShillController {

	public AlternatingAuction(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur,
			List<ItemType> types, Strategy strategy, PuppetFactoryI factory, int numBidder) {
		super(bh, ps, is, ah, ur, types, strategy, factory, 1, numBidder, 40);
	}

	Map<Auction, PuppetI> AuctionsAssigned = new HashMap<>(); // Map<auction, bidder assigned to that auction> 
	private int bidderIndex = 0;
	/**
	 * Assign auctions to shills according to the alternating auction strategy
	 */
	@Override
	protected PuppetI pickBidder(Auction auction) {
		if (!AuctionsAssigned.containsKey(auction)) {
			PuppetI chosen = cbs.get(bidderIndex % cbs.size());
			AuctionsAssigned.put(auction, chosen);
//			System.out.println("new: picked " + chosen);
			bidderIndex++;
			return chosen;
		} else {
//			System.out.println("old: picked " + alternatingAuctionsAssigned.get(auction));
			return AuctionsAssigned.get(auction);
		}
	}
	
	public static AgentAdder getAgentAdder(final int numberOfGroups, final Strategy strategy, final int numBidder) {
		return new AgentAdder() {
			@Override
			public void add(BufferHolder bh, PaymentSender ps, ItemSender is, AuctionHouse ah, UserRecord ur, ArrayList<ItemType> types) {
				for (int i = 0; i < numberOfGroups; i++) {
					AlternatingAuction sc = new AlternatingAuction(bh, ps, is, ah, ur, types, strategy, Puppet.getFactory(), numBidder);
					ah.addEventListener(sc);
				}
			}
			
			@Override
			public String toString() {
				return "AlternatingAuction:" + numberOfGroups;
			}
		};
	}
	
	public static void main(String[] args) {
		final int numberOfGroups = 1;
		Simulation.run(SaveToDatabase.instance(), getAgentAdder(numberOfGroups, new TrevathanStrategy(0.85, 0.85, 0.85), 4));
	}

	@Override
	protected PuppetI pickSeller() {
		return css.get(0);
	}

}
