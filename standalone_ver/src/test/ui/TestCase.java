package test.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import Serial.Chart;
import Serial.HexFile;
import Serial.Stm32Download;

class DownloadException extends Exception {	
	private static final long serialVersionUID = 943905075020089618L;
	}

class ExecuteException extends Exception {
	private static final long serialVersionUID = -7787192127881619341L;}

public class TestCase {
	public String name = "DEFAULT";
	public String target = "";
	public String serialPort = "";
	public String data = "";
	public String signalPin = "";
	public boolean useSignal = false;
	public int times = 1;
	public int duration = 1;
	
	public boolean canCalc = false;
	
	private void download() throws DownloadException {
		Stm32Download download = new Stm32Download();
		
		try {
			download.setSerialPort(serialPort);
			
			download.reboot2isp();
		
			download.sync();
			download.cmdGet();
			download.getID();
			download.read(0x08000000, 32);
			download.erase();
			download.write(HexFile.parse(target));
			
			download.reboot2run();		
		} catch (Exception e) {
			e.printStackTrace();
			throw new DownloadException();
		} finally {
			download.serialClose();	
		}	
	}
	
	private void exec() throws ExecuteException{
		try {
			
			do {
				Chart chart = new Chart();
				chart.exec(new File(data), duration, useSignal, serialPort, signalPin);
			} while (!dataCheck());
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExecuteException();
		}
	}
	
	class Point {
		double voltage;
		double current;
		double time;
		
		public Point(String str) {
			str = str.substring(1);
			String[] list = str.split(",");
			voltage = Double.parseDouble(list[0]);
			current = Double.parseDouble(list[1]);
			time = Double.parseDouble(list[2]);
		}
		
		public String toString() {
			return String.format("%f,%f,%f", voltage, current, time);
		}
	}
	
	class Signal {
		int pin;
		int type;
		double time;
		
		public Signal(String str) {
			str = str.substring(1);
			String[] list = str.split(",");
			pin = Integer.parseInt(list[0]);
			type = Integer.parseInt(list[1]);
			time = Double.parseDouble(list[2]);
		}
		
		public String toString() {
			return String.format("PIN_%d,%s,%f", pin, type==1?"UP":"Down", time);
		}
	}
	
	class Result {
		double voltage = 0;
		double current = 0;
		double power = 0;
		double interval = 0;
		
		public String toString() {
			return String.format("interval = %f, power = %fJ", interval, power);
		}
	}
	
	public String calc() throws IOException {
		//testName, averageCurrent, times, totalTimes, each-time-data, average-power, average-time
		
		ArrayList<Result> list = new ArrayList<>();
		ArrayList<Point> points = new ArrayList<>();
		ArrayList<Signal> signals = new ArrayList<>();
		File dataFile = new File(data);
		BufferedReader br = new BufferedReader(new FileReader(dataFile));
		
		String str = br.readLine();
		while (str != null) {
			if (str.charAt(0) == 'p') {
				points.add(new Point(str));
			} else if (str.charAt(0) == 's') {
				signals.add(new Signal(str));
			}
			str = br.readLine();
		}
		
		br.close();
		
		int i = 0;
		while (i + 1 < signals.size()) {
			Signal s0, s1;
			Result r = new Result();
			s0 = signals.get(i++);
			// if (s0.type == 2)
			if (s0.type == 1) {
				continue;
			}
			s1 = signals.get(i++);
			if (s1.time == s0.time) {
				System.err.println("PIN UP and DOWN at same time");
				continue;
			}
			
			r.interval = s1.time - s0.time;
			for (int k = 0; k + 1 < points.size(); k++) {
				if (points.get(k).time > s0.time && points.get(k).time < s1.time) {
					double t = points.get(k + 1).time - points.get(k).time;
					r.voltage += points.get(k).voltage * t;
					r.current += points.get(k).current * t;
					r.power += points.get(k).voltage * points.get(k).current * t;
				}
			}
			r.voltage /= r.interval;
			r.current /= r.interval;
			list.add(r);
		}
		
		Result average = new Result();
		for (Result re: list) {
			average.current += re.current;
			average.voltage += re.current;
			average.power += re.power;
			average.interval += re.interval;
		}
		
		String ret = "ERR";
		int cnt = list.size();
		
		if (cnt != 0) {
			average.current /= cnt;
			average.voltage /= cnt;
			average.power /= cnt;
			average.interval /= cnt;
			
			ret = String.format("%s,%.3f,%.3f,%d,%.4f,%.4f,%d", name, average.current, average.interval, times, average.power, average.power/times, cnt);
			for (Result re: list) {
				ret += "\n" + String.format("%.3f", re.power);
			}
		}
		
		return ret;
	}
	
	private boolean dataCheck() throws IOException {
		//ArrayList<Point> points = new ArrayList<>();
		File dataFile = new File(data);
		BufferedReader br = new BufferedReader(new FileReader(dataFile));
		String str = br.readLine();
		double current = -1;
		while (str != null) {
			if (str.charAt(0) == 'p') {
				Point p = new Point(str);
				if (p.time < current) {
					br.close();
					return false;
				}
				else {
					current = p.time;
				}
			}
			str = br.readLine();
		}
		
		br.close();
		return true;
	}
	
	public void test() {
		try {
			download();
			exec();
			canCalc = true;
		} catch (DownloadException e) {
			System.err.println("Exception in downloading.");
		} catch (ExecuteException e) {
			System.err.println("Exception in executing");
		}
	}
	
	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("name: " + name + "\n");
		strbuf.append("target: " + target + "\n");
		strbuf.append("serialPort: ");
		strbuf.append(serialPort);
		strbuf.append("\n");
		strbuf.append("data: ");
		strbuf.append(data);
		strbuf.append("\n");
		strbuf.append("useSignal: ");
		strbuf.append(useSignal);
		strbuf.append("\n");
		strbuf.append("signalPin: ");
		strbuf.append(signalPin);
		strbuf.append("\n");
		strbuf.append("times: ");
		strbuf.append(times);
		strbuf.append("\n");
		strbuf.append("duration: ");
		strbuf.append(duration);
		return strbuf.toString();
	}
	
	public static void main(String[] args) throws IOException {
		TestCase testCase = new TestCase();
		testCase.data = "E:/ARMGCC/data/20180108/TestTaskGetName.txt";
		System.out.println(testCase.calc());
	}
}
