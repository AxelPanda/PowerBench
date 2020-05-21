package cn.edu.zju.fm.powerbench;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

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
import com.ibm.icu.text.SimpleDateFormat;


class JsonParseException extends Exception {
	private static final long serialVersionUID = -4061901483701085764L;}

public class AutoTest_bak implements IWorkbenchWindowActionDelegate {
	
	private Settings settings;
	public static Config config;
	
	public AutoTest_bak(File jsonFile) throws FileNotFoundException, JsonParseException {
		FileReader fr = new FileReader(jsonFile);
		Gson gson = new Gson();
		try {
			settings = gson.fromJson(fr, Settings.class);
		} catch (Exception e) {
			e.printStackTrace();
			throw new JsonParseException();
		} finally {
			try { fr.close(); } catch (IOException e) { }
		}
		print2Console(settings.toString());
	}
	
	private void test() {
		//print2Console();
		File configFile = new File(config.workingDirectory + new SimpleDateFormat("[yyyy-MM-dd HHmmss]")
		.format(Calendar.getInstance().getTime()) + "autotest_result.csv");
		for (TestCase testcase: settings.testCases) {
			testcase.isPlugin = isPlugin;
			testcase.test(config);
		}
		print2Console("测试内容,框架,平均电流(mA),平均时间(s),执行次数,总平均功耗(mJ),单次功耗(mJ),测试次数,数据");
		for (TestCase testcase: settings.testCases) {
			if (testcase.canCalc) {
			//if (true) {
				try {
					print2Console(testcase.calc());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			if (!configFile.exists()) {
				File parent = configFile.getParentFile();
				if (!parent.exists()) {
					parent.mkdirs();
				}
				configFile.createNewFile();
				BufferedWriter bwr = new BufferedWriter(new FileWriter(configFile, false));
				bwr.write("测试内容,框架,平均电流(mA),平均时间(s),执行次数,总平均功耗(mJ),单次功耗(mJ),测试次数,数据");
				bwr.newLine();
				for (TestCase testcase: settings.testCases) {
					if (testcase.canCalc) {
						bwr.write(testcase.calc());
						bwr.newLine();
					}
				}
				bwr.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
			fileDialog.setText("选择自动测试配置文件");
			FileReader configJson = new FileReader(fileDialog.open());
			//FileReader configJson = new FileReader(new File("config.json"));
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
//		deleteFile(new File(config.workingDirectory + config.workspaceDirectory));
//		try {
//			copyDir(config.workingDirectory + config.frameworkDirectory + config.framework, config.workingDirectory + config.workspaceDirectory);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
		try {
			AutoTest auto = new AutoTest(new File(config.workingDirectory + config.testcaseJson));
			PowerSource power = new PowerSource();
			power.isPlugin = isPlugin;
			power.start();
			try {
				Thread.currentThread().sleep(3000);	// 等待3秒板子正常上电，不等待会导致第一个包无法正常下载
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			power.dispose();
			auto.test();
		} catch (FileNotFoundException e) {
			print2Console("Can't not find settings file, or can not open.");
			e.printStackTrace();
		} catch (JsonParseException e) {
			print2Console("Json file parse err.");
		}
	}
	
	/**
	 * 先根遍历序递归删除文件夹
	 *
	 * @param dirFile 要被删除的文件或者目录
	 * @return 删除成功返回true, 否则返回false
	 */
	public static boolean deleteFile(File dirFile) {
	    // 如果dir对应的文件不存在，则退出
	    if (!dirFile.exists()) {
	        return false;
	    }

	    if (dirFile.isFile()) {
	        return dirFile.delete();
	    } else {

	        for (File file : dirFile.listFiles()) {
	            deleteFile(file);
	        }
	    }

	    return dirFile.delete();
	}
	
	public static void copyDir(String sourcePath, String newPath) throws IOException {
        File file = new File(sourcePath);
        String[] filePath = file.list();
        
        if (!(new File(newPath)).exists()) {
            (new File(newPath)).mkdir();
        }
        
        for (int i = 0; i < filePath.length; i++) {
            if ((new File(sourcePath + file.separator + filePath[i])).isDirectory()) {
                copyDir(sourcePath  + file.separator  + filePath[i], newPath  + file.separator + filePath[i]);
            }
            
            if (new File(sourcePath  + file.separator + filePath[i]).isFile()) {
                copyFile(sourcePath + file.separator + filePath[i], newPath + file.separator + filePath[i]);
            }
            
        }
    }
	
	public static void copyFile(String oldPath, String newPath) throws IOException {
        File oldFile = new File(oldPath);
        File file = new File(newPath);
        FileInputStream in = new FileInputStream(oldFile);
        FileOutputStream out = new FileOutputStream(file);;

        byte[] buffer=new byte[2097152];
        int readByte = 0;
        while((readByte = in.read(buffer)) != -1){
            out.write(buffer, 0, readByte);
        }
    
        in.close();
        out.close();
    }
	
	public static boolean isPlugin = false;
	//static MessageConsoleStream printer = null;
	
	public static void print2Console(String printStr) {
		if (isPlugin) {
			/*if (printer == null) {
				printer = ConsoleFactory.getConsole()
						.newMessageStream();
			}
			printer.setActivateOnWrite(true);
			printer.println(printStr);*/
		} else {
			System.out.println(printStr);
		}
	}

	@Override
	public void run(IAction arg0) {
		// TODO Auto-generated method stub
		//isPlugin = true;
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
