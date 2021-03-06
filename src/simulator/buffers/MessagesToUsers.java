package simulator.buffers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MessagesToUsers {

	private HashMap<Long, List<Message>> map;
	
	public MessagesToUsers() {
		this.map = new HashMap<Long, List<Message>>();
	}
	
	public void startUsersTurn() {
	}
	
	public void startAhTurn() {
		// make a new map to store new messages to users from the AuctionHouse
		this.map.clear();
	}
	
	/**
	 * NOT TO BE CALLED BY AuctionHouse
	 */
	public List<Message> getMessages(long userId) {
		// get messages
		if (this.map.containsKey(userId))
			return this.map.get(userId);
		else
			return Collections.emptyList();
	}
	
	/**
	 * ONLY TO BE CALLED BY AuctionHouse
	 * 
	 * synchronized to make sure all values written are seen immediately...
	 * (is this necessary?) IT IS NOT.
	 */
	public void putMessages(long userId, Message message) {
		List<Message> auctionBids = this.map.get(userId);
		if (auctionBids == null) {
			auctionBids = new ArrayList<Message>();
			this.map.put(userId, auctionBids);
		}
		auctionBids.add(message);
	}
	
}