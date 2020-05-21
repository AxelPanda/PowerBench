package org.fmlab.assiatant;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.fmlab.evaluation.views.PowerView;
import org.fmlab.evaluation.views.ThermalView;

public class AssistBoardData extends Thread {

	private ArrayList<PowerDataItem> powerData;
	private ArrayList<EventItem> eventData;
	private ArrayList<ThermalDataItem> thermalData;
	
	private LinkedBlockingQueue<String> messages;
	private double now;
	
	public static ThermalView thermalView;
	public static PowerView powerView;
	
	public AssistBoardData() {
		powerData = new ArrayList<>();
		eventData = new ArrayList<>();
		thermalData = new ArrayList<>();
		now = 0;
		messages = new LinkedBlockingQueue<>();
	}
	
	private void sendData() {
		System.out.println("In function");
		ArrayList<PowerDataItem> pData = new ArrayList<>();
		ArrayList<EventItem> eData = new ArrayList<>();
		for (PowerDataItem item : powerData) {
			if (item.time > now) {
				pData.add(item);
			}
		}
		for (EventItem item : eventData) {
			if (item.time > now) {
				eData.add(item);
			}
		}
		PowerDataItem[] powerA = new PowerDataItem[pData.size()];
		EventItem[] eventArray = new EventItem[eData.size()];
		pData.toArray(powerA);
		eData.toArray(eventArray);
		int time = powerA[0].time;
		for (EventItem item : eventArray) {
			item.time = item.time - time;
		}
		
		now = now + 500;
		
		powerView.setData(powerA, eventArray);
		System.out.println("len1: " + powerA.length + " " + powerA[0].time);
	}
	
	public void add(String str) {
		if (str.startsWith("+") || str.startsWith("-")) {
			PowerDataItem item = PowerDataItem.parse(str);
			if (item != null) {
				powerData.add(item);
				if (item.time - now > PowerView.timeBound) {
					sendData();
				}
			}
		} else if (str.startsWith("event")) {
			EventItem item = EventItem.parse(str);
			if (item != null) {
				eventData.add(item);
			}
		} else if (str.startsWith("{")) {
			ThermalDataItem item = ThermalDataItem.parse(str);
			if (item != null) {
				thermalData.add(item);
				if (thermalView != null) {
					thermalView.setData(item.data);
				}
				System.out.println("the" + item);
			}
		}
	}
	
	public void enqueue(String message) {
		try {
			messages.put(message);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		// infinity loop
		while (isInterrupted() == false) {
			String mesStr = messages.poll();
			if (mesStr != null) {
				add(mesStr);
			}
		}
		thermalView.stopDisplay();
		powerView.stop();
	}
	
	public static void test() {
		PowerDataItem[] data = new PowerDataItem[3];
		data[0] = new PowerDataItem(1.234, 500);
		data[1] = new PowerDataItem(2.345, 1000);
		data[2] = new PowerDataItem(3.45, 2000);
		powerView.setData(data, null);
	}
	
}
