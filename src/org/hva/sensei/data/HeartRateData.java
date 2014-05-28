package org.hva.sensei.data;

public class HeartRateData {
	private long timestamp;
	private long heart_rate;
	private long id;
	private long run_id;
	
	
	public HeartRateData(long heart_rate, long timestamp, long run_id) {
		this.timestamp = timestamp;
		this.heart_rate = heart_rate;
		this.run_id = run_id;
	}
	
	public long getRun_id() {
		return run_id;
	}
	public void setRun_id(long run_id) {
		this.run_id = run_id;
	}
	public HeartRateData() {
		// TODO Auto-generated constructor stub
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}

	public long getHeart_rate() {
		return heart_rate;
	}

	public void setHeart_rate(long heart_rate) {
		this.heart_rate = heart_rate;
	}
	

}
