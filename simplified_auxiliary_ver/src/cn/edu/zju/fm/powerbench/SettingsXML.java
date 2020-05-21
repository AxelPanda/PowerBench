package cn.edu.zju.fm.powerbench;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class XMLFormatException extends Exception {
	private static final long serialVersionUID = 1L;
}

/*
class TestCase {
	
	private String name;
	private String code;
	private String buildTool;
	private String target;
	
	public TestCase(Node testCaseNode) {
		read(testCaseNode);
	}
	
	public TestCase(String code, String buildTool, String target) {
		this.code = code;
		this.buildTool = buildTool;
		this.target = target;
	}
	
	public void read(Node testCaseNode) {
		NodeList list = testCaseNode.getChildNodes();
		name = testCaseNode.getAttributes().getNamedItem("name").getNodeValue();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node != null) {
				switch(node.getNodeName()) {
				case "code":
					code = node.getTextContent();
					break;
				case "buildTool":
					buildTool = node.getTextContent();
					break;
				case "target":
					target = node.getTextContent();
					break;
				}
			}
		}
	}
	
	public void download() throws InterruptedException, IOException {
		Stm32Download download = new Stm32Download();
		MemMap memMap;
		download.setSerialPort(SettingsXML.getSerialPortName());
		memMap = HexFile.parse(target);
		//reboot to isp
		download.reboot2isp();
		
		download.sync();
		download.cmdGet();
		download.getID();
		download.read(0x08000000, 64); //test read enable
		download.erase();
		download.write(memMap);
		
		//reboot to run, serial close
		download.reboot2run();
		download.serialClose();
	}
	
	public String toString() {
		String str = "\n" + "case-" + name + "\n";
		str = str + "code:" + code + "\n";
		str = str + "buildTool:" + buildTool + "\n";
		str = str + "target:" + target;
		return str;
	}
	
}


public class SettingsXML {
	
	private static String name;
	private static String serialPort;
	private static ArrayList<TestCase> testCases = new ArrayList<TestCase>();
	
	public static void read(String xmlFile) throws XMLFormatException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = dbf.newDocumentBuilder();
			InputStream in = new FileInputStream(new File(xmlFile));
			Document doc = builder.parse(in);
			Element root = doc.getDocumentElement();
			if (root == null) {
				throw new XMLFormatException();
			}
			NodeList nodes= root.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node != null) {
					switch (node.getNodeName()) {
					case "name":
						name = node.getTextContent();
						break;
					case "serial":
						serialPort = node.getTextContent();
						break;
					case "testCases":
						NodeList child = node.getChildNodes();
						for (int j = 0; j < child.getLength(); j++) {
							Node testCaseNode = child.item(j);
							if (testCaseNode!=null && testCaseNode.getNodeName().equals("testCase")) {
								TestCase testCase = new TestCase(testCaseNode);
								testCases.add(testCase);
							}
						}
						break;
					}
					
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static String getSerialPortName() {
		return serialPort;
	}
	
	public static int getCaseNum() {
		return testCases.size();
	}
	
	public static TestCase testCaseItem(int i) {
		return testCases.get(i);
	}
	
	public static String print() {
		String info = "";
		info = info + "Project Name:" + name + "\n";
		info = info + "Serial Port:" + serialPort;
		for (TestCase testCase : testCases) {
			info = info + "\n" + testCase;
		}
		return info;
	}

	public static void main(String[] args) {
		try {
			SettingsXML.read("E:/stm32_test/settings.xml");
			System.out.println(SettingsXML.print());
			for (int i = 0; i < SettingsXML.getCaseNum(); i++) {
				SettingsXML.testCaseItem(i).download();
				//test
				ConsoleLog.print("Do the test");
				
				//start UI thread to display current
				DisplayCurrent displayCurrent = new DisplayCurrent();
				displayCurrent.start();
				
				Thread.sleep(20000);
				
				displayCurrent.setFinish();
			}
		} catch (XMLFormatException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
*/