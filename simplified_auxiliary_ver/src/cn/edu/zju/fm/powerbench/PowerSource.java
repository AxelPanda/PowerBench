package cn.edu.zju.fm.powerbench;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.eclipse.ui.console.MessageConsoleStream;

import com.google.gson.Gson;


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
		voltage = Double.parseDouble(data[1].substring(0, data[1].length() - 1));
		String[] timestamp = data[2].split(":");
		time = Double.parseDouble(timestamp[0])*3600 + Double.parseDouble(timestamp[1])*60 + Double.parseDouble(timestamp[2]);
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
		return String.format("p%f,%f,%f", voltage, current, time);
	}
	
	@Override
	public String toString() {
		String str = String.format("VOLTAGE = %fV, CURRENT = %fmA, TIME = %fs", voltage, current, time);
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
	public static Config config;
	
	private LinkedList<PowerSourceMessage> data;
	
	public void init(String ip, int i) throws UnknownHostException, IOException {
		available = true;
		/*socket = new Socket(ip, i);
		writer = new PrintWriter(socket.getOutputStream());
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));*/
		data = new LinkedList<PowerSourceMessage>();
		//prevent the list is empty
		data.add(new PowerSourceMessage(0, 0, 0));
		print2Console("Connected to Power Source.");
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
			print2Console("Disconnect.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setVoltage(double u) {
		send(":VOLTage " + u);
		print2Console("SET VOLTAGE = " + u);
	}
	
	public void setMaxCurrent(double i) {
		send(":CURRent " + i);
		print2Console("SET MAXCURRETN = " + i);
	}
	
	public void open() {
		send(":OUTP ON");
		print2Console("OUTPUT ON");
	}
	
	public void powerOff() {
		send(":OUTP OFF");
		print2Console("OUTPUT OFF");
	}
	
	public PowerSourceMessage fetch() throws IOException {
		/*//send(":FETCh?");
//		latestData = new PowerSourceMessage(receive());
		PowerSourceMessage latestData = new PowerSourceMessage(receive());*/
		// -- new begin
		time = System.currentTimeMillis() * 1.0 / 1000;
		PowerSourceMessage latestData = new PowerSourceMessage(5.0, 100, time);
		// -- new end
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
				print2Console(mesg.toString());
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
	
	public PowerSource() {
		try {
			FileReader configJson = new FileReader(new File("config.json"));
			Gson gson = new Gson();
			try {
				config = gson.fromJson(configJson, Config.class);
			} catch (Exception e) {
				print2Console("Configuration file parse failed");
			} finally {
				try {
					configJson.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			print2Console("SerialPort: " + config.serialPort + ", BaudRate: " + config.baudRate);
			print2Console("Voltage: " + config.voltage + "V, Current: " + config.current + "A");
			print2Console("PowerController: " + config.ipAddr + ":" + config.ipPort);
			print2Console("TestcaseJsonFile: " + config.workingDirectory + config.testcaseJson);
		} catch (FileNotFoundException e) {
			print2Console("Open configuration file failed");
		}
	}

	public static void main(String[] args) {
		PowerSource power = new PowerSource();
		try {
			power.init(config.ipAddr, config.ipPort);

			/*
			power.send(":SENS:CONC:RES 4");
			power.send(":SENS:CONC:NPLC 0.002");
			power.send(":SENS:CONC:AVER OFF");
			power.send(":FORMat:ELEMents \"READ,SOUR,UNIT,RST\"");
			
			power.setVoltage(config.voltage);
			power.setMaxCurrent(config.current);
			power.open();
			*/
//			power.close();
			while (true) {
				print2Console(power.fetch().toString());
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
			init(config.ipAddr, config.ipPort);
			/*
			send(":SENS:CONC:RES 4");
			send(":SENS:CONC:NPLC 0.002");
			send(":SENS:CONC:AVER OFF");
			send(":FORMat:ELEMents \"READ,SOUR,UNIT,RST\"");
			
			setVoltage(config.voltage);
			setMaxCurrent(config.current);
			open();
			*/
			
			print2Console("Start to Fetch");
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
	
	public static boolean isPlugin = false;
	static MessageConsoleStream printer = null;
	
	public static void print2Console(String printStr) {
		if (isPlugin) {
			if (printer == null) {
				printer = ConsoleFactory.getConsole()
						.newMessageStream();
			}
			printer.setActivateOnWrite(true);
			printer.println(printStr);
		} else {
			System.out.println(printStr);
		}
	}

}
