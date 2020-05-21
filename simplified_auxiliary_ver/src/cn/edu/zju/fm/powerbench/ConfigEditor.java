package cn.edu.zju.fm.powerbench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.console.MessageConsoleStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigEditor implements IWorkbenchWindowActionDelegate {
	private Text text_serialPort;
	private Text text_baudRate;
	private Text text_ipAddr;
	private Text text_ipPort;
	private Text text_voltage;
	private Text text_current;
	private Text text_workingDirectory;
	private Text text_testcaseJson;
	private Config config;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ConfigEditor window = new ConfigEditor();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	Display display;
	Shell shell;

	/**
	 * Open the window.
	 */
	public void open() {
		Gson gson = new Gson();
		//config = new Config();
		FileReader configJson;
		try {
			configJson = new FileReader(new File("config.json"));
			config = gson.fromJson(configJson, Config.class);
			configJson.close();
		} catch (FileNotFoundException e) {
			print2Console("Configuration file doesn't exist");
			//e.printStackTrace();
		} catch (IOException e) {
			print2Console("File close failed.");
			//e.printStackTrace();
		}
		display = Display.getDefault();
		shell = new Shell();
		shell.setText("\u914D\u7F6E\u4FEE\u6539");
		shell.setSize(480,320);
		
		Group grpConfig = new Group(shell, SWT.NONE);
		grpConfig.setText("\u914D\u7F6E\u4FE1\u606F");
		grpConfig.setBounds(10, 10, 442, 253);
		
		Label label = new Label(grpConfig, SWT.NONE);
		label.setBounds(10, 33, 76, 20);
		label.setText("\u4E32\u53E3");
		
		text_serialPort = new Text(grpConfig, SWT.BORDER);
		text_serialPort.setBounds(92, 30, 73, 26);
		text_serialPort.setText(config.serialPort);
		
		text_baudRate = new Text(grpConfig, SWT.BORDER);
		text_baudRate.setBounds(253, 30, 73, 26);
		text_baudRate.setText(String.valueOf(config.baudRate));
		
		text_ipAddr = new Text(grpConfig, SWT.BORDER);
		text_ipAddr.setBounds(92, 70, 73, 26);
		text_ipAddr.setText(config.ipAddr);
		
		text_ipPort = new Text(grpConfig, SWT.BORDER);
		text_ipPort.setBounds(253, 70, 73, 26);
		text_ipPort.setText(String.valueOf(config.ipPort));
		
		Label label_1 = new Label(grpConfig, SWT.NONE);
		label_1.setBounds(171, 33, 76, 20);
		label_1.setText("\u6CE2\u7279\u7387");
		
		Label lblip = new Label(grpConfig, SWT.NONE);
		lblip.setBounds(10, 73, 76, 20);
		lblip.setText("\u7535\u6E90IP");
		
		Label label_3 = new Label(grpConfig, SWT.NONE);
		label_3.setBounds(171, 73, 76, 20);
		label_3.setText("\u7535\u6E90\u7AEF\u53E3");
		
		Label label_4 = new Label(grpConfig, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_4.setBounds(10, 62, 422, 2);
		
		text_voltage = new Text(grpConfig, SWT.BORDER);
		text_voltage.setBounds(92, 102, 73, 26);
		text_voltage.setText(String.valueOf(config.voltage));
		
		text_current = new Text(grpConfig, SWT.BORDER);
		text_current.setBounds(253, 102, 73, 26);
		text_current.setText(String.valueOf(config.current));
		
		Label lblv = new Label(grpConfig, SWT.NONE);
		lblv.setBounds(10, 105, 76, 20);
		lblv.setText("\u7535\u538B(V)");
		
		Label lbla = new Label(grpConfig, SWT.NONE);
		lbla.setBounds(171, 105, 76, 20);
		lbla.setText("\u7535\u6D41(A)");
		
		Label label_2 = new Label(grpConfig, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_2.setBounds(10, 134, 422, 2);
		
		text_workingDirectory = new Text(grpConfig, SWT.BORDER);
		text_workingDirectory.setBounds(92, 142, 234, 26);
		text_workingDirectory.setText(config.workingDirectory);
		
		text_testcaseJson = new Text(grpConfig, SWT.BORDER);
		text_testcaseJson.setBounds(92, 174, 234, 26);
		text_testcaseJson.setText(config.testcaseJson);
		
		Label label_5 = new Label(grpConfig, SWT.NONE);
		label_5.setBounds(10, 145, 76, 20);
		label_5.setText("\u5DE5\u4F5C\u76EE\u5F55");
		
		Label label_6 = new Label(grpConfig, SWT.NONE);
		label_6.setBounds(10, 177, 76, 20);
		label_6.setText("\u7528\u4F8B\u5217\u8868");
		
		Button button_save = new Button(grpConfig, SWT.NONE);
		button_save.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				save();
			}
		});
		button_save.setBounds(67, 206, 98, 30);
		button_save.setText("\u4FDD\u5B58");
		
		Button button_cancel = new Button(grpConfig, SWT.NONE);
		button_cancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				shell.dispose();
			}
		});
		button_cancel.setBounds(171, 206, 98, 30);
		button_cancel.setText("\u53D6\u6D88");
		
		Button button = new Button(grpConfig, SWT.NONE);
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				DirectoryDialog fileDialog = new DirectoryDialog(shell, SWT.OPEN);
				fileDialog.setText("选择工作目录");
				text_workingDirectory.setText(fileDialog.open());
			}
		});
		button.setBounds(332, 140, 98, 30);
		button.setText("\u9009\u62E9\u76EE\u5F55");
		
		Button button_1 = new Button(grpConfig, SWT.NONE);
		button_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
				fileDialog.setText("选择测试用例配置文件");
				fileDialog.open();
				text_testcaseJson.setText(fileDialog.getFileName());
			}
		});
		button_1.setBounds(332, 172, 98, 30);
		button_1.setText("\u9009\u62E9\u6587\u4EF6");

		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	private void save() {
		config.serialPort = text_serialPort.getText();
		config.baudRate = Integer.parseInt(text_baudRate.getText());
		config.ipAddr = text_ipAddr.getText();
		config.ipPort = Integer.parseInt(text_ipPort.getText());
		config.voltage = Float.parseFloat(text_voltage.getText());
		config.current = Float.parseFloat(text_current.getText());
		config.workingDirectory = text_workingDirectory.getText();
		if (!config.workingDirectory.endsWith("\\")) {
			config.workingDirectory += "\\";
		}
		config.testcaseJson = text_testcaseJson.getText();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String configString = gson.toJson(config, Config.class);
		
		try {
			File configFile = new File("config.json");
			if (!configFile.exists()) {
				File parent = configFile.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				configFile.createNewFile();
			}
			BufferedWriter bwr = new BufferedWriter(new FileWriter(configFile, false));
			bwr.write(configString);
			bwr.newLine();
			bwr.close();
			print2Console("配置信息保存成功！");
			shell.dispose();
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

	@Override
	public void run(IAction arg0) {
		isPlugin = true;
		try {
			ConfigEditor window = new ConfigEditor();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
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
