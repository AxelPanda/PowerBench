package org.fmlab.assiatant;

public class EventItem {
	public int id;
	public int type;
	public int time;
	
	public EventItem(int id, int type, int time) {
		this.id = id;
		this.type = type;
		this.time = time;
	}
	
	public String toString() {
		return String.format("event:%d,type:%d,time:%d\n", id, type, time);
	}
	
	public static EventItem parse(String str) {
		try {
			if (!str.startsWith("event:")){
				return null;
			}
			String[] spl = str.substring(6).split(",time:|,type:");
			int id = Integer.parseInt(spl[0]);
			int type = Integer.parseInt(spl[1]);
			int time = Integer.parseInt(spl[2]);
			return new EventItem(id, type, time); 
		} catch (Exception e) {
			return null;
		}
	}	
}
