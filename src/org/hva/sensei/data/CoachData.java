package org.hva.sensei.data;

public class CoachData {
	
	int id;
	int run_id; 
	int answer; //stimulus type in int
	long datetime;
	int stimulus; // type in int
	long stimulus_length; //long in ms
	int pleasure;// value between -4 tot  4
	int arousal; // value between -4 tot  4
	int instructions; // 0 no 1 is yes
	
	
	
	
	public CoachData(int run_id, int answer, long datetime, int stimulus,
			long stimulus_length, int pleasure, int arousal, int instructions) {
		super();
		this.run_id = run_id;
		this.answer = answer;
		this.datetime = datetime;
		this.stimulus = stimulus;
		this.stimulus_length = stimulus_length;
		this.pleasure = pleasure;
		this.arousal = arousal;
		this.instructions = instructions;
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getRun_id() {
		return run_id;
	}
	public void setRun_id(int run_id) {
		this.run_id = run_id;
	}
	public int getAnswer() {
		return answer;
	}
	public void setAnswer(int answer) {
		this.answer = answer;
	}
	public long getDatetime() {
		return datetime;
	}
	public void setDatetime(long datetime) {
		this.datetime = datetime;
	}
	public int getStimulus() {
		return stimulus;
	}
	public void setStimulus(int stimulus) {
		this.stimulus = stimulus;
	}
	public long getStimulus_length() {
		return stimulus_length;
	}
	public void setStimulus_length(long stimulus_length) {
		this.stimulus_length = stimulus_length;
	}
	public int getPleasure() {
		return pleasure;
	}
	public void setPleasure(int pleasure) {
		this.pleasure = pleasure;
	}
	public int getArousal() {
		return arousal;
	}
	public void setArousal(int arousal) {
		this.arousal = arousal;
	}
	public int getInstructions() {
		return instructions;
	}
	public void setInstructions(int instructions) {
		this.instructions = instructions;
	}
	
	
	
	
}
