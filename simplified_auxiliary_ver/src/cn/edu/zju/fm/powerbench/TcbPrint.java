package cn.edu.zju.fm.powerbench;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
/*
class TcbPrintEvent {
	public double time;
	public int TcbPrintValue;
	
	public TcbPrintEvent(double time, int TcbPrintValue) {
		this.time = time;
		this.TcbPrintValue = TcbPrintValue;
		System.out.println(toString());
	}
	
	public String toString() {
		return String.format("Tasks: %.3fs, %s", 
				time,
				TcbPrintValue);
	}
	
	public static TcbPrintEvent parse(String str) {
//		System.out.println(str);
		str = str.substring(1);
		String[] data = str.split(",");
		double time = Double.parseDouble(data[0]);
		int TcbPrintValue = Integer.parseInt(data[1]);
		return new TcbPrintEvent(time, TcbPrintValue);
	}
	
	public String save() {
		return String.format("t%.3f,%d", time, TcbPrintValue);
	}
}*/

public class TcbPrint extends Thread {
	
	private SerialPort serialPort;
	private boolean threadStatus; //Running - true, Stable - false
	private long startSystemTime;
	private double startPowerTime;
	private PowerSource power;
	private static final int POLLING_INTERVAL = 100;
	public static InputStream inputStream;
	public static OutputStream outputStream;
	private ArrayList<TcbPrintEvent> data;
	
	public TcbPrint(SerialPort port, long startSystemTime, double startPowerTime, PowerSource power)
			throws PortInUseException, UnsupportedCommOperationException, SerialPortNotExist, IOException {
		/*CommPortIdentifier identifier = Serial.getIdentifier(port);
		if (identifier == null) {
			throw new SerialPortNotExist();
		}
		serialPort = (SerialPort)identifier.open("SerialPort", 0);
		serialPort.setSerialPortParams(115200, 
				SerialPort.DATABITS_8, 
				SerialPort.STOPBITS_1, 
				SerialPort.PARITY_NONE);*/
		serialPort = port;
		data = new ArrayList<TcbPrintEvent>();
		inputStream = serialPort.getInputStream();
		outputStream = serialPort.getOutputStream();
		this.startSystemTime = startSystemTime;
		this.startPowerTime = startPowerTime;
		this.power = power;
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
	
	public void sendQuery() {
		byte[] printTCBCmd = {0x54,0x43,0x42,0x50,0x72,0x69,0x6E,0x74,0x0D,0x0A};
		try {
			outputStream.write(printTCBCmd);
			outputStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			for (TcbPrintEvent e: data) {
				bwr.write(e.save());
				bwr.newLine();
			}
		}
		
		bwr.close();
	}
	
	/* get signalChangedEvents which are occurred after t */
	public TcbPrintEvent[] getEvents(double time) {
		ArrayList<TcbPrintEvent> events = new ArrayList<>();
		synchronized (data) {
			int index = data.size();
			while (index > 0) {
				index--;
				TcbPrintEvent e = data.get(index);
				if (e.time > time) {
					events.add(e);
				} else {
					break;
				}
			}
		}
		TcbPrintEvent[] eventArray = new TcbPrintEvent[events.size()];
		events.toArray(eventArray);
		return eventArray;
	}
	
	public void run() {
		long systemTime;
		double powerTime;
		String serialString;
		threadStatus = true;
		int length;
		byte[] bytes;
		
		while (threadStatus) {
			try {
				length = inputStream.available();
				systemTime = System.currentTimeMillis();
				powerTime = (systemTime - startSystemTime) * 1.0 / 1000 + startPowerTime;
				if (length > 0) {
					BufferedReader inBuffer = new BufferedReader(new InputStreamReader(inputStream));
					serialString = inBuffer.readLine();
					System.out.println(serialString);
					//bytes = new byte[length];
					//inputStream.read(bytes);
					//serialString = new String (bytes);
					//System.out.println(serialString);
					if (serialString.startsWith("T|")) {
						System.out.println(serialString);
						/*synchronized (data) {
							int TcbPrintValue = Integer.parseInt(serialString.substring(9, length-2));
							data.add(new TcbPrintEvent(powerTime, TcbPrintValue));
						}*/
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PowerSource power = new PowerSource();
		power.start();
		try {
			Thread.sleep(200);
			
			SerialPort portSerial;
			CommPortIdentifier identifier = Serial.getIdentifier("COM5");
			portSerial = (SerialPort)identifier.open("SerialPort", 0);
			portSerial.setSerialPortParams(115200, 
					SerialPort.DATABITS_8, 
					SerialPort.STOPBITS_1, 
					SerialPort.PARITY_NONE);
			
			TcbPrint tp = new TcbPrint(portSerial, System.currentTimeMillis(), power.getTime(), power);
			//power.dispose();
			tp.start();
			Thread.sleep(1000);
			for(int i = 0; i < 30; i++) {
				tp.sendQuery();
				Thread.sleep(1000);
			}
			//Thread.sleep(30000);
			tp.close();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SerialPortNotExist e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
