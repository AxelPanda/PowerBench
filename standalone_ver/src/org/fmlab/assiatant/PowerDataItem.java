package org.fmlab.assiatant;

public class PowerDataItem {
	public double current;
	public int time;
	
	public PowerDataItem(double current, int time) {
		this.current = current;
		this.time = time;
	}
	
	public String toString() {
		return String.format("%.3f, %d\n", current, time);
	}
	
	public static PowerDataItem parse(String str) {
		try {
			String[] spl = str.split(",");
			double current = Double.parseDouble(spl[0]);
			int time = Integer.parseInt(spl[1]);
			return new PowerDataItem(current, time);
		} catch (Exception e) {
			return null;
		}
	}
	
}
