package cn.edu.zju.fm.powerbench;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.console.MessageConsoleStream;

import com.google.gson.Gson;


import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class Stm32Download implements IWorkbenchWindowActionDelegate {
	
	private static final int STM32_ACK = 0x79;
	private static final int STM32_NACK = 0x1F;
	private static final int STM_ERR = -1;
	private static final int STM_OK = 0;
	private static final int MAX_TRY = 4;
	
	private CommPortIdentifier portIdentifier;
	private SerialPort serialPort;
	public static InputStream inputStream;
	public static OutputStream outputStream;
	
	private int boardVersion = 0;
	private byte[] cmd = new byte[16];
	private byte[] pid = new byte[8];
	
	public void setSerialPort(String port) {
		portIdentifier = Serial.getIdentifier(port);
		try {
			serialPort = (SerialPort)portIdentifier.open("SerialPort", 2000);
			serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_EVEN);
			outputStream = serialPort.getOutputStream();
			inputStream = serialPort.getInputStream();
			print2Console("Set Serial Port OK!");
		} catch (PortInUseException e) {
			print2Console("Serial port is in use.");
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			print2Console("Serial port parameters is not support.");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public void serialClose() {
		if (serialPort != null) {
			serialPort.close();	
		}
	}
	
	private byte checkSum(byte[] buffer) {
		byte sum = (byte)(buffer.length - 1);
		for (int i = 0; i < buffer.length; i++) {
			sum = (byte)(buffer[i] ^ sum);
		}
		return sum;
	}
	
	private byte[] addrPacket(int address) {
		byte[] buffer = new byte[5];
		buffer[0] = (byte)((address >> 24) & 0xFF);
		buffer[1] = (byte)((address >> 16) & 0xFF);
		buffer[2] = (byte)((address >> 8 ) & 0xFF);
		buffer[3] = (byte)((address >> 0 ) & 0xFF);
		buffer[4] = (byte)(buffer[0] ^ buffer[1] ^ buffer[2] ^ buffer[3]);
		return buffer;
	}
	
	private void sendCommand(byte c) throws IOException {
		byte xorByte = (byte)(c ^ 0xFF);
		outputStream.write(new byte[]{c, xorByte});
		outputStream.flush();
	}
	
	public int sync() {
		int ret = STM_ERR;
		final byte STM32_INIT = 0x7F;
		int boardRet = 0;
		
		print2Console("Sync");
//		System.out.flush();
		for (int i = 0; i < MAX_TRY; i++) {
//			System.out.print('.');
//			System.out.flush();
			try {
				outputStream.write(new byte[]{STM32_INIT});
				outputStream.flush();
				boardRet = inputStream.read();
				if (boardRet == STM32_ACK || boardRet == STM32_NACK) {
					ret = STM_OK;
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
//		System.out.println();
		
		if (ret == STM_OK) {
			print2Console("Connected to board");
		} else {
			print2Console("Can not connect to board, ");
			if (boardRet == STM32_NACK) {
				print2Console("failed.");
			} else {
				print2Console("timed-out.");
			}
		}
		
		return ret;
	}
	
	public int cmdGet() {
		final byte GET = (byte)0x00;
		int length = 0;
		int ret = STM_ERR;
		
		try {
			sendCommand(GET);
			if (inputStream.read() == STM32_ACK) {
				length = inputStream.read();
				boardVersion = inputStream.read();
				for (int i = 0; i < length; i++) {
					cmd[i] = (byte)inputStream.read();
				}
				if (inputStream.read() == STM32_ACK) {
					String str = String.format("Bootloader version: %x.%x Command set-[", boardVersion >> 4, 
							boardVersion & 0xF);
					for (int i = 0; i < length; i++) {
						str = str + String.format(" %02X", cmd[i]);
					}
					str = str + "]";
					print2Console(str);
					ret = STM_OK;
				} else {
					print2Console("STM32-Get: command error!");
				}		
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public void getID() {
		final byte GetID = (byte)0x02;
		int length = 0;
		
		try {
			sendCommand(GetID);
			assert(inputStream.read() == STM32_ACK);
			length = inputStream.read();
			//print2Console(length);
			for (int i = 0; i < length + 1; i++) {
				pid[i] = (byte)inputStream.read();
			}
			assert(inputStream.read() == STM32_ACK);
			
			String str = "Chip ID: ";
			for (int i = 0; i < length + 1; i++) {
				str = str + String.format("%02X", pid[i]);
			}
			print2Console(str);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] read(int address, int length) {
		final byte READ = (byte)0x11;
		byte[] buffer = new byte[length];
		
		try {
			sendCommand(READ);
			assert(inputStream.read() == STM32_ACK);
			outputStream.write(addrPacket(address));
			outputStream.flush();
			assert(inputStream.read() == STM32_ACK);

			assert(length > 0 && length <= 255);
			sendCommand((byte)(length - 1));
			assert(inputStream.read() == STM32_ACK);			
			for (int i = 0; i <length; i++) {
				buffer[i] = (byte)inputStream.read();
			}
			

			String str = "Read Buffer: ";
			for (int i = 0; i < length; i++) {
				if (i != 0)
					str = str + ";";
				str = str + String.format("%02X", buffer[i]);
			}
			print2Console(str);
//			System.out.println("Read Buffer");
//			for (int i = 0; i < length; i++) {
//				System.out.printf("%02X", buffer[i]);
//				if (i % 16 == 15) {
//					System.out.println();	
//				}
//				else {
//					System.out.print(";");
//				}
//			}
//			System.out.println();
//			System.out.flush();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void erase() {
		final byte ERASE = (byte)0x44;
		print2Console("Erase .");
		try {
			sendCommand(ERASE);
			assert(inputStream.read() == STM32_ACK);
			outputStream.write(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0x00});
			outputStream.flush();
			
			while (inputStream.read() != STM32_ACK) {
				System.out.print(".");
				System.out.flush();
				Thread.sleep(500);
			}
			System.out.println();
			System.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void writeBlock(int address, byte[] buffer) {
		final byte WRITE = (byte)0x31;
		int length = buffer.length;
		byte dataCheckSum = checkSum(buffer);
		
		assert(length % 4 == 0 && length > 0 && length <= 256);
		
		try {
			sendCommand(WRITE);
			assert(inputStream.read() == STM32_ACK);
			outputStream.write(addrPacket(address));
			outputStream.flush();
			assert(inputStream.read() == STM32_ACK);
			outputStream.write((byte)(length - 1));
			for (int i = 0; i < length; i++) {
				outputStream.write(buffer[i]);
			}
			outputStream.write(dataCheckSum);
			outputStream.flush();
//			inputStream.read();
			assert(inputStream.read() == STM32_ACK);
			
//			System.out.printf("WriteBlock - %08X", address);
//			System.out.println();
//			System.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(MemMap memMap) {
		print2Console("Start to download");
		
		MemBlock[] memBlockArr;
		memBlockArr = memMap.getMemBlockArray();
		//System.out.println(memBlockArr.length);
		for (MemBlock memBlock : memBlockArr) {
			//System.out.println(memBlock);
			writeBlock(memBlock.getAddress(), memBlock.getData());
		}
		
		print2Console("Download OK!");
	}
	
	public void reboot2isp() throws InterruptedException, IOException {
		//DTR NRST
		//RTS BOOT0
		
		print2Console("Reboot to isp....");
		serialPort.setRTS(false);
//		System.out.println("Set RTS(boot0) 1");
		Thread.sleep(500);
		/*PowerSource power = new PowerSource();
		power.start();
		Thread.sleep(500);
		power.powerOff();
		Thread.sleep(500);
		power.open();
		Thread.sleep(500);
		power.dispose();
		Thread.sleep(500);*/
		serialPort.setDTR(true);
//		System.out.println("Set DTR(NRST) 0");
		Thread.sleep(500);
		serialPort.setDTR(false);
//		System.out.println("Set DTR(NRST) 1");
		Thread.sleep(500);
		while (inputStream.available() != 0) {
			inputStream.read();
		}
	}
	
	public void reboot2run() throws InterruptedException {
		//DTR NRST
		//RTS BOOT0
		print2Console("Reboot to run......");
		serialPort.setRTS(true);
//		System.out.println("Set RTS(boot0) 0");
		Thread.sleep(500);
		/*PowerSource power = new PowerSource();
		power.start();
		Thread.sleep(500);
		power.powerOff();
		Thread.sleep(500);
		power.open();
		Thread.sleep(500);
		power.dispose();
		Thread.sleep(500);*/
		serialPort.setDTR(true);
//		System.out.println("Set DTR(NRST) 0");
		Thread.sleep(500);
		serialPort.setDTR(false);
//		System.out.println("Set DTR(NRST) 1");
		Thread.sleep(500);
	}
	
	/*public Stm32Download() {
		//power = new PowerSource();
		//power.start();
	}*/
	MessageConsoleStream printer = null;
	
	public void print2Console(String printStr) {
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
	
	public void exec(Stm32Download download) {
		
		try {
			download.reboot2isp();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		download.sync();
		download.cmdGet();
		download.getID();
		download.read(0x08000000, 16);
		download.erase();
		//memMap = HexFile.parse("E:/STM32/hex/F407PA1.hex");
		download.write(memMap);
		
		try {
			download.reboot2run();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		download.serialClose();
	}
	
	public static MemMap memMap;
	
	public static void main(String[] args) {
		final Stm32Download download = new Stm32Download();
		download.isPlugin = isPlugin;
		PowerSource power = new PowerSource();
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
		fileDialog.setText("选择要烧录的Hex文件");
		download.memMap = HexFile.parse(fileDialog.open());
		download.setSerialPort(power.config.serialPort);
		
		Runnable downloader = new Runnable() {
			public void run() {
				download.exec(download);
			}
		};
		new Thread(downloader).start();
	}
	
	public static boolean isPlugin = false;

	@Override
	public void run(IAction arg0) {
		// TODO Auto-generated method stub
		isPlugin = true;
		main(null);
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(IWorkbenchWindow arg0) {
		// TODO Auto-generated method stub
		
	}
}
