                                                                                                                                       package cn.edu.zju.fm.powerbench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.ui.console.MessageConsoleStream;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

class SerialPortNotExist extends Exception {
	private static final long serialVersionUID = 1L;
	public SerialPortNotExist() {}
	public SerialPortNotExist(String message) {
		super(message);
	}
}

class SignalChangedEvent {
	public int pin;
	public double time;
	public int action;
	public static final int RISING_EDGE = 1;
	public static final int FALLING_EDGE = 2;
	public static final int PIN_CTS = 0;
	public static final int PIN_DSR = 1;
	
	public SignalChangedEvent(int pin, int action, double time) {
		this.pin = pin;
		this.action = action;
		this.time = time;
		System.out.println(toString());
		//print2Console(toString());
	}
	
	public String toString() {
		return String.format("%s, %.3fs, %s", 
				pin == PIN_CTS? "CTS" : "DSR",
				time, 
				action == RISING_EDGE? "RISING_EDGE" : "FALLING_EDGE");
	}
	
	public static SignalChangedEvent parse(String str) {
		str = str.substring(1);
		String[] data = str.split(",");
		int pin = Integer.parseInt(data[0]);
		int action = Integer.parseInt(data[1]);
		double time = Double.parseDouble(data[2]);
		return new SignalChangedEvent(pin, action, time);
	}
	
	public String save() {
		return String.format("s%d,%d,%.3f", pin, action, time);
	}
}

public class Signal extends Thread {
	
	private static final int POLLING_INTERVAL = 1;
	
	private SerialPort serialPort;
	private boolean threadStatus; //Running - true, Stable - false
	private long startSystemTime;
	private double startPowerTime;
	
	private boolean CTS;
	private boolean DSR;
	
//	private PowerSource power;
	private ArrayList<SignalChangedEvent> data;

	public Signal(SerialPort port, long startSystemTime, double startPowerTime, PowerSource power) 
			throws SerialPortNotExist, PortInUseException, UnsupportedCommOperationException {
		/*CommPortIdentifier identifier = Serial.getIdentifier(port);
		if (identifier == null) {
			throw new SerialPortNotExist();
		}
		serialPort = (SerialPort)identifier.open("SerialPort", 0);
		serialPort.setSerialPortParams(9600, 
				SerialPort.DATABITS_8, 
				SerialPort.STOPBITS_1, 
				SerialPort.PARITY_NONE);*/
		serialPort = port;
		data = new ArrayList<SignalChangedEvent>();
		this.startSystemTime = startSystemTime;
		this.startPowerTime = startPowerTime;
		this.power = power;
		System.out.println(this.startSystemTime + ", " + this.startPowerTime);
	}
	
	public void close() {
		threadStatus = false; //try to close the thread
		try {
			Thread.sleep(POLLING_INTERVAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/*if (serialPort != null) {
			serialPort.close();
		}*/
	}
	
	/* get signalChangedEvents which are occurred after t */
	public SignalChangedEvent[] getEvents(double time) {
		ArrayList<SignalChangedEvent> events = new ArrayList<>();
		synchronized (data) {
			int index = data.size();
			while (index > 0) {
				index--;
				SignalChangedEvent e = data.get(index);
				if (e.time > time) {
					events.add(e);
				} else {
					break;
				}
			}
		}
		SignalChangedEvent[] eventArray = new SignalChangedEvent[events.size()];
		events.toArray(eventArray);
		return eventArray;
	}
	
	public void save(File file) throws IOException {
		if (!file.exists()) {
			File parent = file.getParentFile();
			if (!parent.exists()) {
				parent.mkdirs();
			}
			file.createNewFile();
		}
		
		BufferedWriter bwr = new BufferedWriter(new FileWriter(file, true));
		
		synchronized (data) {
			for (SignalChangedEvent e: data) {
				bwr.write(e.save());
				bwr.newLine();
			}
		}
		
		bwr.close();
	}
	
	private PowerSource power;
	
	public void run() {
		boolean newCTS; //PINA - Channel 1
		boolean newDSR; //PINB - Channel 2
		long systemTime;
		double powerTime;
		
		serialPort.setRTS(true); //boot0 = 0
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		serialPort.setDTR(true); //NRST = 0
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		serialPort.setDTR(false); //NRST = 1
		/*
		//PowerSource power = new PowerSource();
		try {
			//power.start();
			//Thread.sleep(500);
			power.powerOff();
			Thread.sleep(500);
			power.open();
			Thread.sleep(500);
			//power.dispose();
			//Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		CTS = serialPort.isCTS();
		DSR = serialPort.isDSR();
		threadStatus = true;
		
		while (threadStatus) {
			newCTS = serialPort.isCTS();
			newDSR = serialPort.isDSR();
			systemTime = System.currentTimeMillis();
			powerTime = (systemTime - startSystemTime) * 1.0 / 1000 + startPowerTime;
			if (newCTS != CTS) {
				System.out.print("CTS changed!");
				synchronized (data) {
					int event = !CTS ? SignalChangedEvent.FALLING_EDGE : SignalChangedEvent.RISING_EDGE;
					data.add(new SignalChangedEvent(SignalChangedEvent.PIN_CTS, event, powerTime));
				}
				CTS = newCTS;
			}
			if (newDSR != DSR) {
				synchronized (data) {
					int event = !DSR ? SignalChangedEvent.FALLING_EDGE : SignalChangedEvent.RISING_EDGE;
					data.add(new SignalChangedEvent(SignalChangedEvent.PIN_DSR, event, powerTime));
				}
				DSR = newDSR;
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			PowerSource power = new PowerSource();
			power.start();
			Thread.sleep(200);

			SerialPort portSerial;
			CommPortIdentifier identifier = Serial.getIdentifier("COM3");
			portSerial = (SerialPort)identifier.open("SerialPort", 0);
			portSerial.setSerialPortParams(115200, 
					SerialPort.DATABITS_8, 
					SerialPort.STOPBITS_1, 
					SerialPort.PARITY_NONE);
			
			Signal s = new Signal(portSerial, System.currentTimeMillis(), power.getTime(), power);
			//power.dispose();
			s.start();
			
			Thread.sleep(30000);
			s.close();
			
			/*
			 * File file = new File("E://data//data.txt");
			 * try {
			 * 	power.save(file);
			 * 	s.save(file);
			 * } catch (IOException e) {
			 * 	e.printstacktrace();
			 * }
			 */
		} catch (SerialPortNotExist e) {
			e.printStackTrace();
		} catch (PortInUseException e) {
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
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
