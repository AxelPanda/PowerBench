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

import org.eclipse.ui.console.MessageConsoleStream;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

class CpuUsageEvent {
	public double time;
	public int CpuUsageValue;
	
	public CpuUsageEvent(double time, int CpuUsageValue) {
		this.time = time;
		this.CpuUsageValue = CpuUsageValue;
		//print2Console(toString());
	}
	
	public String toString() {
		return String.format("CPUUsage: %.3fs, %d", 
				time,
				CpuUsageValue);
	}
	
	public static CpuUsageEvent parse(String str) {
		str = str.substring(1);
		String[] data = str.split(",");
		double time = Double.parseDouble(data[0]);
		int CpuUsageValue = Integer.parseInt(data[1]);
		return new CpuUsageEvent(time, CpuUsageValue);
	}
	
	public String save() {
		return String.format("c%.3f,%d", time, CpuUsageValue);
	}
}

class TcbPrintEvent {
	public double time;
	public String taskString;
	
	public TcbPrintEvent(double time, String taskString) {
		this.time = time;
		this.taskString = taskString;
		//print2Console(toString());
	}
	
	public String toString() {
		return String.format("Tasks Running @ %.3fs\nPRIO\tID\tSTAT\n%s", 
				time,
				taskString.replace('|', '\n').replace(':', '\t'));
	}
	
	public static TcbPrintEvent parse(String str) {
		str = str.substring(1);
		String[] data = str.split(",");
		double time = Double.parseDouble(data[0]);
		String taskString = data[1];
		return new TcbPrintEvent(time, taskString);
	}
	
	public String save() {
		return String.format("t%.3f,%s", time, taskString);
	}
}

class CpuTmprtEvent {
	public double time;
	public double CpuTmprtValue;
	
	public CpuTmprtEvent(double time, double CpuTmprtValue) {
		this.time = time;
		this.CpuTmprtValue = CpuTmprtValue;
		//print2Console(toString());
	}
	
	public String toString() {
		return String.format("CPUTmprt: %.3fs, %.2f", 
				time,
				CpuTmprtValue);
	}
	
	public static CpuTmprtEvent parse(String str) {
		str = str.substring(1);
		String[] data = str.split(",");
		double time = Double.parseDouble(data[0]);
		double CpuTmprtValue = Double.parseDouble(data[1]);
		return new CpuTmprtEvent(time, CpuTmprtValue);
	}
	
	public String save() {
		return String.format("T%.3f,%.2f", time, CpuTmprtValue);
	}
}

public class CpuUsage extends Thread {
	
	private SerialPort serialPort;
	private boolean threadStatus; //Running - true, Stable - false
	private long startSystemTime;
	private double startPowerTime;
	private PowerSource power;
	private static final int POLLING_INTERVAL = 100;
	public static InputStream inputStream;
	public static OutputStream outputStream;
	private ArrayList<CpuUsageEvent> data;
	private ArrayList<TcbPrintEvent> tcbData;
	private ArrayList<CpuTmprtEvent> tmpData;
	
	public CpuUsage(SerialPort port, long startSystemTime, double startPowerTime, PowerSource power)
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
		data = new ArrayList<CpuUsageEvent>();
		tcbData = new ArrayList<TcbPrintEvent>();
		tmpData = new ArrayList<CpuTmprtEvent>();
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
			for (CpuUsageEvent e: data) {
				bwr.write(e.save());
				bwr.newLine();
			}
		}
		synchronized (tcbData) {
			for (TcbPrintEvent e: tcbData) {
				bwr.write(e.save());
				bwr.newLine();
			}
		}
		synchronized (tmpData) {
			for (CpuTmprtEvent e: tmpData) {
				bwr.write(e.save());
				bwr.newLine();
			}
		}
		
		bwr.close();
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
	
	/* get signalChangedEvents which are occurred after t */
	public CpuUsageEvent[] getEvents(double time) {
		ArrayList<CpuUsageEvent> events = new ArrayList<>();
		synchronized (data) {
			int index = data.size();
			while (index > 0) {
				index--;
				CpuUsageEvent e = data.get(index);
				if (e.time > time) {
					events.add(e);
				} else {
					break;
				}
			}
		}
		CpuUsageEvent[] eventArray = new CpuUsageEvent[events.size()];
		events.toArray(eventArray);
		return eventArray;
	}
	
	/* get signalChangedEvents which are occurred after t */
	public CpuTmprtEvent[] getTmprtEvents(double time) {
		ArrayList<CpuTmprtEvent> events = new ArrayList<>();
		synchronized (tmpData) {
			int index = tmpData.size();
			while (index > 0) {
				index--;
				CpuTmprtEvent e = tmpData.get(index);
				if (e.time > time) {
					events.add(e);
				} else {
					break;
				}
			}
		}
		CpuTmprtEvent[] eventArray = new CpuTmprtEvent[events.size()];
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
					/*bytes = new byte[length];
					inputStream.read(bytes);
					serialString = new String (bytes);*/
					BufferedReader inBuffer = new BufferedReader(new InputStreamReader(inputStream));
					while ((serialString = inBuffer.readLine()) != null) {
						System.out.println(serialString);
						if (serialString.startsWith("CPUUsage:") && serialString.length() > 9) {
							synchronized (data) {
								int CpuUsageValue = Integer.parseInt(serialString.substring(9));
								data.add(new CpuUsageEvent(powerTime, CpuUsageValue));
							}
						} else if (serialString.startsWith("T|") && serialString.length() > 2) {
							synchronized (tcbData) {
								tcbData.add(new TcbPrintEvent(powerTime, serialString.substring(2)));
								//String taskString = serialString.substring(2).replace(':', '\t');
								//String[] tasks = taskString.split("\\|");
								//data.add(new CpuUsageEvent(powerTime, CpuUsageValue));
							}
						} else if (serialString.startsWith("iTemp:") && serialString.length() > 6) {
							synchronized (tmpData) {
								int CpuTmprtValue = Integer.parseInt(serialString.substring(6));
								tmpData.add(new CpuTmprtEvent(powerTime, CpuTmprtValue/100.0));
							}
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
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
			CommPortIdentifier identifier = Serial.getIdentifier("COM3");
			portSerial = (SerialPort)identifier.open("SerialPort", 0);
			portSerial.setSerialPortParams(115200, 
					SerialPort.DATABITS_8, 
					SerialPort.STOPBITS_1, 
					SerialPort.PARITY_NONE);
			
			CpuUsage c = new CpuUsage(portSerial, System.currentTimeMillis(), power.getTime(), power);
			//power.dispose();
			c.start();
			Thread.sleep(1000);
			for(int i = 0; i < 30; i++) {
				c.sendQuery();
				Thread.sleep(1000);
			}
			//Thread.sleep(30000);
			c.close();
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
