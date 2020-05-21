package org.fmlab.assiatant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class AssistBoard extends Thread{
	
	private SerialPort serialPort;
	public static InputStream inputStream;
	public static BufferedReader reader;
	public static OutputStream outputStream;
	
	private AssistBoardData data;
	private int duration;

	public AssistBoard(String port, AssistBoardData data, int duration) {
		CommPortIdentifier portIdentifier = Serial.getIdentifier(port);
		this.data = data;
		this.duration = duration;
		try {
			serialPort = (SerialPort)portIdentifier.open("SerialPort", 2000);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			outputStream = serialPort.getOutputStream();
			inputStream = serialPort.getInputStream();
			reader = new BufferedReader(new InputStreamReader(inputStream));
		} catch (PortInUseException e) {
			System.err.println("Serial port is in use.");
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			System.err.println("Serial port parameters is not support.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private void sendStart() throws IOException {
		data.start();
		outputStream.write(new byte[]{'S', 't', 'a', 'r', 't', '\r', '\n'});
		outputStream.flush();
	}
	
	private void sendReset() throws IOException {
		outputStream.write(new byte[]{'R', 'e', 's', 'e', 't', '\r', '\n'});
		outputStream.flush();
		data.interrupt();
	}
	
	private void close() throws IOException {
		reader.close();
		outputStream.close();
		serialPort.close();
	}
	
	@Override
	public void run() {
		try {
			sendStart();
			long t = System.currentTimeMillis();
			while (isInterrupted() == false && System.currentTimeMillis()<t+duration) {
				if (reader.ready()) {
					String mesg = reader.readLine();
					if (mesg == null) {
						break;
					} else {
						data.enqueue(mesg);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				sendReset();
				close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
