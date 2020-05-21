package org.fmlab.assiatant;

public class ThermalDataItem {
	public double[] data;
	public final static int size = 64;
	
	private ThermalDataItem() {
		data = new double[size];
	}
	
	
	public String toString() {
		StringBuilder builder = new StringBuilder("{\"thermal\":[");
		for (int i = 0; i < size; i++) {
			if (i != 0) {
				builder.append(',');
			}
			builder.append(String.format("%.2f", data[i]));
		}
		builder.append("]}\n");
		return builder.toString();
	}
	
	public static ThermalDataItem parse(String str) {
		try {
			if (!str.startsWith("{\"thermal\":[")) {
				return null;
			}
			ThermalDataItem t = new ThermalDataItem();
			String[] spl = str.substring(12).split(",|]}");
			for (int i = 0; i < size; i++) {
				t.data[i] = Double.parseDouble(spl[i]);
			}
			return t;
		} catch (Exception e) {
			return null;
		}
	}
	
}
