package simulator.objects;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import agents.SimpleUser;
import agents.SimpleUserI;

/**
 * Bid object. Comparator sorts list from lowest to highest.
 */
public class Bid implements Comparable<Bid> {
	
	private static final Logger logger = Logger.getLogger(Bid.class);
	
	private static final AtomicInteger bidId = new AtomicInteger(); // for allocating unique bid ids
	
	private final int id;
	private long time;
	private final SimpleUserI bidder;
	private final int price;
	
	public Bid(SimpleUserI bidder, int price) {
//		this.id = -1;
		this.id = bidId.getAndIncrement();
		this.bidder = bidder;
		this.price = price;
		this.time = -1;
		
		assert(bidder != null) : "Bidder is null";
		assert(price > 0) : "Price must be > 0, but is " + price + ".";
	}

	public int getId() {
		return id;
	}
	
//	public void setId(int id) {
//		if (this.id == -1) {
//			this.id = id;
//		} else {
//			logger.error("Bid id cannont be changed.");
//			assert false;
//		}
//	}
	
	public SimpleUserI getBidder() {
		return bidder;
	}

	public int getPrice() {
		return price;
	}

	public long getTime() {
		return this.time;
	}
	
	public void setTime(long time) {
		if (this.time == -1) {
			this.time = time;
		} else {
			logger.error("Bid time cannont be changed.");
			assert false;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(id:");
		sb.append(this.getId());
		sb.append(", bidder:");
		sb.append(this.getBidder());
		sb.append(", time:");
		sb.append(this.getTime());
		sb.append(", price:");
		sb.append(this.getPrice());
		sb.append(")");
		return sb.toString();
	}

	// sorts bids from lowest to highest
	@Override
	public int compareTo(Bid o) {
		return Integer.compare(this.getPrice(), o.getPrice());
	}
}
