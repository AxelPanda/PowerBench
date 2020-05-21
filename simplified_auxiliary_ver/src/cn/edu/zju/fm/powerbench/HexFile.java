package cn.edu.zju.fm.powerbench;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

class MemBlock {
	private int baseAddress;
	private int address;
	private byte[] data;
	
	public MemBlock(int baseAddress, int length, int address, byte[] data) {
		this.baseAddress = baseAddress;
		this.address = address;
		this.data = new byte[length];
		for (int i = 0; i < length; i++)
			this.data[i] = data[i];
	}
	
	public int getAddress() {
		return (baseAddress << 16) | address;
	}
	
	public int getLength() {
		return data.length;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public String toString() {
		String str = "";
		str = str + String.format("%04X %04X--", baseAddress, address);
		str = str + String.format("%08X", getAddress()) + ":";
		for (int i = 0; i < data.length; i++) {
			str = str + String.format(" %02X", data[i]);
		}
		return str;
	}
}

class MemMap {
	private LinkedList<MemBlock> memBlocks = new LinkedList<MemBlock>();
	private int baseAddress = 0;
	
	public MemMap() {
		memBlocks = new LinkedList<MemBlock>();
		baseAddress = 0;
	}
	
	public void addMemBlock(int length, int address, byte[] data) {
		memBlocks.add(new MemBlock(baseAddress, length, address, data));
	}
	
	public void setBaseAddress(int baseAddress) {
		this.baseAddress = baseAddress;
	}
	
	public MemBlock[] getMemBlockArray() {
		MemBlock[] arr = new MemBlock[memBlocks.size()];
		memBlocks.toArray(arr);
		return arr;
	}
	
	public String toString() {
		String str = "";
		for (MemBlock memBlock : memBlocks) {
			str = str + memBlock.toString() + "\n";
		}
		return str;
	}
}

public class HexFile {
	
	
	public static MemMap parse(String name) {
		MemMap memMap = new MemMap();
		File file = new File(name);
		InputStream in;
		int length, address, type;
		@SuppressWarnings("unused")
		int checkSum;
		byte[] data;
		int newRead;
		
		try {
			in = new FileInputStream(file);
			newRead = in.read();
			while (newRead != -1) {
				if (newRead == 0x3A) {
					length = getNextByte(in);
					address = getNextShort(in)&0xFFFF;
					type = getNextByte(in);
					data = getBytes(in, length);
					checkSum = getNextByte(in);
//					System.out.printf("%02X %04X %02X %s %02X", length, address, type, data, (byte)checkSum);
//					System.out.println();
					if (type == 0x04) {
						memMap.setBaseAddress(data[0] * 256 + data[1]);
					}
					else if (type == 0x00) {
						memMap.addMemBlock(length, address, data);
					}
				}
				newRead = in.read();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return memMap;
	}
	
	public static int chr2num(int ch) throws IOException {
		if (ch >= 48 && ch < 58) {
			return ch - 48;
		}
		else if (ch >= 65 && ch < 71) {
			return ch - 65 + 10;
		}
		else {
			throw new IOException();
		}
	}
	
	public static byte getNextByte(InputStream in) throws IOException {
		int digitHigh, digitLow;
		digitHigh = chr2num(in.read());
		digitLow = chr2num(in.read());
		return (byte)((digitHigh << 4) | (digitLow & 0x0F));
	}
	
	public static short getNextShort(InputStream in) throws IOException {
		int byteHigh, byteLow;
		byteHigh = getNextByte(in);
		byteLow = getNextByte(in);
		return (short)((byteHigh << 8) | (byteLow & 0xFF)); 
	}

	public static byte[] getBytes(InputStream in, int length) throws IOException {
		byte[] arr = new byte[length];
		for (int i = 0; i < length; i++) {
			arr[i] = getNextByte(in);
		}
		return arr;
	}

	public static void main(String[] args) {
		MemMap memMap;
		memMap = parse("E:\\data\\test.hex");
		System.out.println(memMap.toString());
	}

}
