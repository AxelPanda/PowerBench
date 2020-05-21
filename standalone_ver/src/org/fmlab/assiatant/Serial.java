package org.fmlab.assiatant;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
//import java.util.TooManyListenersException;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

public class Serial implements SerialPortEventListener {
	
	public static byte[] readBuffer = new byte[1024];
	
	public static CommPortIdentifier portId = null;
	public static SerialPort serialPort;
	
	public static InputStream inputStream;
	public static OutputStream outputStream;
	public static int cnt = 0;
	
	@SuppressWarnings("unchecked")
	public static String[] getAvailableSerialPortsName() {
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		ArrayList<String> serialPortsNameList = new ArrayList<String>();
		
        while (portList.hasMoreElements()) {
            CommPortIdentifier portIdentifier = portList.nextElement();
            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
            	serialPortsNameList.add(portIdentifier.getName());
            }
        }
        
        String[] serialPortsNameArray = new String[serialPortsNameList.size()];
        serialPortsNameList.toArray(serialPortsNameArray);
        
        return serialPortsNameArray;
	}
	
	public static CommPortIdentifier getIdentifier(String portName) {
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier portIdentifier = portList.nextElement();
			if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL &&
					portName.equals(portIdentifier.getName())) {
				portId = portIdentifier;
				return portId;
			}
		}
		return null;
	}
	
	public static boolean open(String portName) {
		if (getIdentifier(portName) == null) {
			return false;
		}
		
		try {
			serialPort = (SerialPort)portId.open("SerialPort", 2000);
			//serialPort.addEventListener(new Serial());
			//serialPort.notifyOnDataAvailable(true);
			serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			outputStream = serialPort.getOutputStream();
			inputStream = serialPort.getInputStream();
		} catch (PortInUseException e) {
			e.printStackTrace();
//		} catch (TooManyListenersException e) {
//			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			while (true) {
				if (inputStream.available() > 0) {
					int len = inputStream.read(readBuffer);
					String str = "";
					for (int i = 0; i < len; i++) {
						str =  str + (char)readBuffer[i];
					}
					System.out.println(str);	
				}
				outputStream.write(new byte[]{65,66,67});
				outputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	public void serialEvent(SerialPortEvent arg0) {
		// TODO Auto-generated method stub
		cnt = cnt + 1;
		try {
			int len = inputStream.read(readBuffer);
			String str = "";
			for (int i = 0; i < len; i++) {
				str =  str + (char)readBuffer[i];
			}
			System.out.println("cnt = " + cnt);
			System.out.println(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
	public static void main(String[] args) {
		//print all available 
		for (String name: getAvailableSerialPortsName()) {
			System.out.println(name);
		}
		
		System.out.println(open("COM4"));
	}
}
