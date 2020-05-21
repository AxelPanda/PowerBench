package cn.edu.zju.fm.powerbench;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.MessageConsoleStream;

class DownloadException extends Exception {
	private static final long serialVersionUID = 943905075020089618L;
}

class ExecuteException extends Exception {
	private static final long serialVersionUID = -7787192127881619341L;
}

public class TestCase {
	public String name = "DEFAULT";
	public String target = "";
	public String serialPort = "";
	public String data = "";
	public String signalPin = "";
	public boolean useSignal = false;
	public int times = 1;
	public int duration = 1;
	public String framework = "";
	public int multiplier = 1;
	public String IDE = "";

	public boolean canCalc = false;

	private void download() throws DownloadException {
		Stm32Download download = new Stm32Download();
		download.isPlugin = isPlugin;

		try {
			download.setSerialPort(serialPort);

			download.reboot2isp();

			download.sync();
			download.cmdGet();
			download.getID();
			download.read(0x08000000, 32);
			download.erase();
			download.write(HexFile.parse(target));

			download.reboot2run();
		} catch (Exception e) {
			e.printStackTrace();
			throw new DownloadException();
		} finally {
			download.serialClose();
		}
	}

	private void exec() throws ExecuteException {
		try {

			do {
				Chart chart = new Chart(isPlugin);
				chart.exec(new File(data), duration, useSignal, serialPort,
						signalPin);
			} while (!dataCheck());
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExecuteException();
		}
	}

	class Point {
		double voltage;
		double current;
		double time;

		public Point(String str) {
			str = str.substring(1);
			String[] list = str.split(",");
			voltage = Double.parseDouble(list[0]);
			current = Double.parseDouble(list[1]);
			time = Double.parseDouble(list[2]);
		}

		public String toString() {
			return String.format("%f,%f,%f", voltage, current, time);
		}
	}

	class Signal {
		int pin;
		int type;
		double time;

		public Signal(String str) {
			str = str.substring(1);
			String[] list = str.split(",");
			pin = Integer.parseInt(list[0]);
			type = Integer.parseInt(list[1]);
			time = Double.parseDouble(list[2]);
		}

		public String toString() {
			return String.format("PIN_%d,%s,%f", pin,
					type == 1 ? "UP" : "Down", time);
		}
	}

	class Result {
		double voltage = 0;
		double current = 0;
		double power = 0;
		double interval = 0;

		public String toString() {
			return String.format("interval = %f, power = %fJ", interval, power);
		}
	}

	public String calc() throws IOException {
		// testName, averageCurrent, times, totalTimes, each-time-data,
		// average-power, average-time

		ArrayList<Result> list = new ArrayList<>();
		ArrayList<Point> points = new ArrayList<>();
		ArrayList<Signal> signals = new ArrayList<>();
		File dataFile = new File(data);
		BufferedReader br = new BufferedReader(new FileReader(dataFile));

		String str = br.readLine();
		while (str != null) {
			if (str.charAt(0) == 'p') {
				points.add(new Point(str));
			} else if (str.charAt(0) == 's') {
				signals.add(new Signal(str));
			}
			str = br.readLine();
		}

		br.close();

		int i = 0;
		while (i + 1 < signals.size()) {
			Signal s0, s1;
			Result r = new Result();
			s0 = signals.get(i++);
			if (s0.type == 2) {
				continue;
			}
			s1 = signals.get(i++);
			if (s1.time == s0.time) {
				print2Console("PIN UP and DOWN at same time");
				continue;
			}

			r.interval = s1.time - s0.time;
			for (int k = 0; k + 1 < points.size(); k++) {
				if (points.get(k).time > s0.time
						&& points.get(k).time < s1.time) {
					double t = points.get(k + 1).time - points.get(k).time;
					r.voltage += points.get(k).voltage * t;
					r.current += points.get(k).current * t;
					r.power += points.get(k).voltage * points.get(k).current
							* t;
				}
			}
			r.voltage /= r.interval;
			r.current /= r.interval;
			list.add(r);
		}

		Result average = new Result();
		for (Result re : list) {
			average.current += re.current;
			average.voltage += re.current;
			average.power += re.power;
			average.interval += re.interval;
		}

		String ret = "ERR";
		int cnt = list.size();

		if (cnt != 0) {
			average.current /= cnt;
			average.voltage /= cnt;
			average.power /= cnt;
			average.interval /= cnt;

			ret = String.format("%s,%s,%f,%f,%d,%f,%f,%d", name, framework,
					average.current, average.interval, times, average.power,
					average.power / times, cnt);
			for (Result re : list) {
				ret += String.format(",%f", re.power);
			}
		}

		return ret;
	}

	private boolean dataCheck() throws IOException {
		// ArrayList<Point> points = new ArrayList<>();
		File dataFile = new File(data);
		BufferedReader br = new BufferedReader(new FileReader(dataFile));
		String str = br.readLine();
		double current = -1;
		while (str != null) {
			if (str.charAt(0) == 'p') {
				Point p = new Point(str);
				if (p.time < current) {
					br.close();
					return false;
				} else {
					current = p.time;
				}
			}
			str = br.readLine();
		}

		br.close();
		return true;
	}

	public Config config;

	public void test(Config config) {
		try {
			this.config = config;
			times *= multiplier;
			if (IDE.equals("Keil")) {
				framework = "UCOS";
			}
			//compile();
			serialPort = config.serialPort;
			target = config.workingDirectory + config.hexDir + target + "-"
					+ framework + ".hex";
			data = config.workingDirectory + config.logDir + data + "-"
					+ framework + ".log";
			download();
			Thread.sleep(500);
			exec();
			canCalc = true;
		} catch (DownloadException e) {
			print2Console("Exception in downloading.");
		} catch (ExecuteException e) {
			print2Console("Exception in executing");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class StreamGobbler extends Thread {
		InputStream is;

		String type;

		StreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}
	}

	private static String readCmdResult(final Process process) throws Exception {
		String result = null;

		ExecutorService execService = Executors.newFixedThreadPool(1);

		result = execService.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
				InputStream is = process.getInputStream();
				OutputStream os = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int len = 0;
				try {
					while ((len = is.read(buffer)) != -1) {
						os.write(buffer, 0, len);
					}

					return os.toString();
				} catch (Exception e) {
					throw e;
				} finally {
				}
			}
		}).get();

		// 执行完成后关闭线程池
		execService.shutdown();

		return result;
	}

	public void compile() {
		try {
			if (IDE.equals("Keil")) {
				deleteFile(new File(config.workingDirectory + framework
						+ "\\USER\\main.c"));
				copyFile(config.workingDirectory + framework + "\\USER\\"
						+ target + ".c", config.workingDirectory + framework
						+ "\\USER\\main.c");
				ProcessBuilder processBuilder = new ProcessBuilder(
						new String[] { config.workingDirectory + framework
								+ "\\USER\\UCOSII.BAT" });
				processBuilder.redirectErrorStream(true);
				Process process = processBuilder.start();
				if (true) {
					String result;
					try {
						result = readCmdResult(process);
						process.waitFor();
						print2Console(result);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				process.destroy();
				copyFile(config.workingDirectory + framework
						+ "\\OBJ\\test.hex", config.workingDirectory
						+ config.hexDir + target + "-" + framework + ".hex");
			} else {
				deleteFile(new File(config.workingDirectory
						+ config.frameworkDirectory + framework
						+ "\\Src\\testcase.c"));
				copyFile(config.workingDirectory + config.testcaseDirectory
						+ target + ".c", config.workingDirectory
						+ config.frameworkDirectory + framework
						+ "\\Src\\testcase.c");
				Process process = Runtime.getRuntime().exec(
						"make clean -C " + config.workingDirectory
								+ config.frameworkDirectory + framework);
				process.waitFor();
				process.destroy();
				process = null;
				ProcessBuilder processBuilder = new ProcessBuilder(
						new String[] {
								"make",
								"-C",
								config.workingDirectory
										+ config.frameworkDirectory + framework });
				processBuilder.redirectErrorStream(true);
				process = processBuilder.start();
				if (true) {
					String result;
					try {
						result = readCmdResult(process);
						process.waitFor();
						print2Console(result);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				process.destroy();
				copyFile(config.workingDirectory + config.frameworkDirectory
						+ framework + "\\Build\\test.hex",
						config.workingDirectory + config.hexDir + target + "-"
								+ framework + ".hex");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

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

	public static void copyDir(String sourcePath, String newPath)
			throws IOException {
		File file = new File(sourcePath);
		String[] filePath = file.list();

		if (!(new File(newPath)).exists()) {
			(new File(newPath)).mkdir();
		}

		for (int i = 0; i < filePath.length; i++) {
			if ((new File(sourcePath + file.separator + filePath[i]))
					.isDirectory()) {
				copyDir(sourcePath + file.separator + filePath[i], newPath
						+ file.separator + filePath[i]);
			}

			if (new File(sourcePath + file.separator + filePath[i]).isFile()) {
				copyFile(sourcePath + file.separator + filePath[i], newPath
						+ file.separator + filePath[i]);
			}

		}
	}

	public static void copyFile(String oldPath, String newPath)
			throws IOException {
		File oldFile = new File(oldPath);
		File file = new File(newPath);
		FileInputStream in = new FileInputStream(oldFile);
		FileOutputStream out = new FileOutputStream(file);
		;

		byte[] buffer = new byte[2097152];
		int readByte = 0;
		while ((readByte = in.read(buffer)) != -1) {
			out.write(buffer, 0, readByte);
		}

		in.close();
		out.close();
	}

	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("name: " + name + "\n");
		strbuf.append("target: " + target + "\n");
		strbuf.append("framework: ");
		strbuf.append(framework);
		strbuf.append("\n");
		strbuf.append("IDE: ");
		strbuf.append(IDE);
		strbuf.append("\n");
		strbuf.append("useSignal: ");
		strbuf.append(useSignal);
		strbuf.append("\n");
		strbuf.append("signalPin: ");
		strbuf.append(signalPin);
		strbuf.append("\n");
		strbuf.append("times: ");
		strbuf.append(times);
		strbuf.append("\n");
		strbuf.append("duration: ");
		strbuf.append(duration);
		return strbuf.toString();
	}

	public static boolean isPlugin = false;
	static MessageConsoleStream printer = null;

	public static void print2Console(String printStr) {
		if (isPlugin) {
			if (printer == null) {
				printer = ConsoleFactory.getConsole().newMessageStream();
			}
			printer.setActivateOnWrite(true);
			printer.println(printStr);
		} else {
			System.out.println(printStr);
		}
	}
}
