package edu.snu.reef.ALS_SVM;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SMSVector implements Serializable{
	private Boolean isSpam;
	private Map<String, Double> features; 
	
	public SMSVector() {
		features = new HashMap<String, Double>();
	}
	
	public void addWord(String keyword, Double TF) {
		features.put(keyword, TF);
	}
	
	public void setSpam(boolean isSpam) {
		this.isSpam = isSpam;
	}
	
	public Double getValue(String key) {
		return features.get(key);		
	}
	
	public Integer getSpam() {
		return isSpam ? 1 : 0;
	}
	
	public Set<String> getKeys() {
		return features.keySet();
	}
}
