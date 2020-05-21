package Serial;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

//class PowerSourceNotAvailableException extends Exception {
//	public PowerSourceNotAvailableException() {}
//	public PowerSourceNotAvailableException(String message) {
//		super(message);
//	}
//}

class PowerSourceMessage {
	
	private double voltage;
	private double current;
	private double time;
	
	public PowerSourceMessage(String str) {
		//parseDouble - NullPointerException, NumberFormatException, 
		//IndexOutOfBoundException
		String[] data = str.split(",", 3);
		current = Double.parseDouble(data[0].substring(0, data[0].length() - 1)) * 1000;
		voltage = Double.parseDouble(data[1].substring(1, data[1].length() - 1));
		time = Double.parseDouble(data[2].substring(2, data[2].length() - 1));
	}
	
	public PowerSourceMessage(double voltage, double current, double time) {
		this.voltage = voltage;
		this.current = current;
		this.time = time;
	}
	
	public static PowerSourceMessage parse(String str) {
		str = str.substring(1); //remove first char 'p'
		String[] data = str.split(",");
		double voltage = Double.parseDouble(data[0]);
		double current = Double.parseDouble(data[1]);
		double time = Double.parseDouble(data[2]);
		return new PowerSourceMessage(voltage, current, time);
	}
	
	public double getCurrent() {
		return current;
	}
	
	public double getVoltage() {
		return voltage;
	}
	
	public double getTime() {
		return time;
	}
	
	public String save() {
		return String.format("p%.3f,%.3f,%.3f", voltage, current, time);
	}
	
	@Override
	public String toString() {
		String str = String.format("VOLTAGE = %.2fV, CURRENT = %.2fmA, TIME = %.4fs", voltage, current, time);
		return str;
	}
	
	public boolean equals(PowerSourceMessage mesg) {
		if (this.current == mesg.current && this.voltage == mesg.voltage && this.time == mesg.time) {
			return true;
		} else {
			return false;
		}
	}
	
}

public class PowerSource extends Thread{

	private Socket socket;
	private BufferedReader reader;
	private PrintWriter writer;
//	private	PowerSourceMessage latestData;
	private boolean available = false;
	private double time = 0;
	
	private LinkedList<PowerSourceMessage> data;
	
	public void init(String ip, int i) throws UnknownHostException, IOException {
		available = true;
		socket = new Socket(ip, i);
		writer = new PrintWriter(socket.getOutputStream());
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		data = new LinkedList<PowerSourceMessage>();
		//prevent the list is empty
		data.add(new PowerSourceMessage(0, 0, 0));
		ConsoleLog.print("Connected to Power Source.");
	}
	
	public void dispose() {
		available = false;
	}
	
	public void close() {
		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
			ConsoleLog.print("Disconnect.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setVoltage(double u) {
		send(":VOLTage " + u);
		ConsoleLog.print("SET VOLTAGE = " + u);
	}
	
	public void setMaxCurrent(double i) {
		send(":CURRent " + i);
		ConsoleLog.print("SET MAXCURRETN = " + i);
	}
	
	public void open() {
		send(":OUTP ON");
		ConsoleLog.print("OUTPUT ON");
	}
	
	public PowerSourceMessage fetch() throws IOException {
		send(":FETCh?");
//		latestData = new PowerSourceMessage(receive());
		PowerSourceMessage latestData = new PowerSourceMessage(receive());
		synchronized (data) {
			if (latestData.equals(data.getLast()) == false) {
				data.add(latestData);
			}	
		}
		time = latestData.getTime();
		return latestData;
	}
	
	public PowerSourceMessage[] getLatestSecondsData(double seconds) {
		double time;
		int index;
		ArrayList<PowerSourceMessage> list = new ArrayList<>();
		
		synchronized (data) {
			time = data.getLast().getTime();
			index = data.size();
			while (index - 1 >= 0) {
//				index--;
				index--;
				if (time - data.get(index).getTime() < seconds) {
					list.add(data.get(index));
				} else {
					break;
				}
			}
		}
		
		PowerSourceMessage[] array = new PowerSourceMessage[list.size()];
		list.toArray(array);
		return array;
	}
	
	public void dump() {
		synchronized (data) {
			for (PowerSourceMessage mesg : data) {
				System.out.println(mesg);
			}	
		}
	}
	
	public double getCurrent() {
		synchronized (data) {
			return data.getLast().getCurrent();	
		}
	}
	
	public double getVoltage() {
		synchronized (data) {
			return data.getLast().getVoltage();	
		}
	}
	
	public double getTime() {
		return time;
	}
	
	public void save(File file) throws IOException {
		if (!file.exists()) {
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			file.createNewFile();
		}
		
		BufferedWriter bwr = new BufferedWriter(new FileWriter(file, false));
		
		synchronized (data) {
			for (PowerSourceMessage psm: data) {
				bwr.write(psm.save());
				bwr.newLine();
			}	
		}
		
		bwr.close();
	}
	
	private void send(String command) {
		writer.println(command);
		writer.flush();
	}
	
	private String receive() throws IOException {
		String recv;
		recv = reader.readLine();
		return recv;
	}

	public static void main(String[] args) {
		PowerSource power = new PowerSource();
		try {
			power.init("10.214.9.89", 5025);
			
			power.send(":SENS:CONC:RES 4");
			power.send(":SENS:CONC:NPLC 0.002");
			power.send(":SENS:CONC:AVER OFF");
			
			power.setVoltage(3.3);
			power.setMaxCurrent(0.1);
			power.open();
//			power.close();
			while (true) {
				System.out.println(power.fetch());
//				Thread.sleep(5);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			init("10.214.9.89", 5025);
			send(":SENS:CONC:RES 4");
			send(":SENS:CONC:NPLC 0.002");
			send(":SENS:CONC:AVER OFF");
			
			setVoltage(3.3);
			setMaxCurrent(0.5);
			open();
			
			ConsoleLog.print("Start to Fetch");
			while (available) {
				fetch();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
		} finally {
			close();
		}
	}

}
