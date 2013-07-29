package gov.nasa.jpf.cv;

import java.util.Map;


public interface Visitor {
	public void sameState(String s1, String s2);

	public void setTransition(String start, String action, String end);
	
	public Map<String,String> getSameStates();
	
	public Map<Integer, Map<String, Integer>> getTransitions();
	
	public String idToName(int id);
	
	public int nameToId(String name);
}