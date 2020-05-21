package cn.edu.zju.fm.powerbench;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class Chart implements IWorkbenchWindowActionDelegate {
	
	private Display display;
	private Shell shell;
	private Canvas canvas;
	public PowerSource power;
	private static Signal signal;
	private static CpuUsage cpuUsage;
	//private static CpuTmprt cpuTmprt;
	private static boolean useSignal;
	
	private static final int currentStep = 5;
	private static final int voltageStep = 1;
	
	private static int currentBound = currentStep;
	private static int voltageBound = voltageStep;
	private static int currentLowerBound = 0x7FFFFFFF;
	private static int timeBound = 10;	// 图的时间长度
	
	private static String[] channelName = new String[]{"CTS", "DSR"};
	
	Font font;
	public static boolean isPlugin = false;
	
	private static boolean finish = false;
	
	public static double preCurrent = 0;
	public static double nowCurrent = 0;
	
	static {
	}
	
	public Chart() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("实时数据");
		shell.setSize(640, 480);
		canvas = new Canvas(shell, SWT.NONE | SWT.DOUBLE_BUFFERED);
		font = new Font(display, "Consolas", 8, SWT.BOLD);
	}
	
	public Chart(boolean plugin) {
		isPlugin = plugin;
		display = PlatformUI.createDisplay();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("实时数据");
		shell.setSize(640, 480);
		canvas = new Canvas(shell, SWT.NONE | SWT.DOUBLE_BUFFERED);
		font = new Font(display, "Consolas", 8, SWT.BOLD);
	}
	
	public void draw() {
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle clientArea = shell.getClientArea();
				final double ratio = 0.07;
				Rectangle chartArea = new Rectangle(
						clientArea.x + (int)(clientArea.width * ratio),
						clientArea.y + (int)(clientArea.height * ratio),
						(int)(clientArea.width * (1 - 2 * ratio)),
						(int)(clientArea.height * (1 - 2 * ratio)));
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
				e.gc.fillRectangle(chartArea);
				e.gc.setClipping(chartArea);
				PowerSourceMessage[] data = power.getLatestSecondsData(timeBound + 0.2);
				PowerSourceMessage[] windowData = power.getLatestSecondsData(1);
				for(PowerSourceMessage msg : windowData) {
					nowCurrent = nowCurrent + msg.getCurrent();
				}
				nowCurrent = nowCurrent / windowData.length;
				if (nowCurrent - preCurrent >= 10) {
					//System.out.println(nowCurrent);
					//System.out.println(preCurrent);
					//cpuUsage.sendQuery();
				}
				preCurrent = nowCurrent;
				updateBounds(data);
				
				drawDotLine(e, chartArea);

				double displayTime = data[0].getTime() - timeBound;
				drawChart(e, data, chartArea, displayTime);
				
				e.gc.setClipping(clientArea);
				e.gc.setFont(font);
				drawCurrentAxis(e, chartArea);
				drawVoltageAxis(e, chartArea);
				drawTimeAxis(e, chartArea);

				if (data[0] != null && useSignal) {
					SignalChangedEvent[] events = signal.getEvents(displayTime);
//					System.out.println(displayTime);
//					System.out.println(events.length);
					for (SignalChangedEvent event: events) {
						drawSignal(e, chartArea, event, displayTime);
					}
				}
				CpuUsageEvent[] cpuUsageEvents = cpuUsage.getEvents(data[0].getTime() - timeBound);
				CpuTmprtEvent[] cpuTmprtEvents = cpuUsage.getTmprtEvents(data[0].getTime() - timeBound);
				drawCpuUsage(e, chartArea, cpuUsageEvents, displayTime);
				drawCpuTmprt(e, chartArea, cpuTmprtEvents, displayTime);
			}
		});
	}

	private static void updateBounds(PowerSourceMessage[] data) {
		double maxCurrent = 0;
		double minCurrent = 0;
		double maxVoltage = 0;
		boolean set = false;
		for (PowerSourceMessage item : data) {
			if (!set) {
				maxCurrent = item.getCurrent();
				minCurrent = item.getCurrent();
				maxVoltage = item.getVoltage();
				set = true;
			} else {
				if (item.getCurrent() > maxCurrent) {
					maxCurrent = item.getCurrent();
				}
				if (item.getCurrent() < minCurrent) {
					minCurrent = item.getCurrent();
				}
				if (item.getVoltage() > maxVoltage) {
					maxVoltage = item.getVoltage();
				}	
			}
		}
		
		// new calculation method
		currentBound = maxCurrent >= 0?
				(int)(maxCurrent / currentStep) * currentStep + currentStep :
				(int)(maxCurrent / currentStep) * currentStep;
		currentLowerBound = minCurrent >= 0?
				(int)(minCurrent / currentStep) * currentStep :
				(int)(minCurrent / currentStep) * currentStep - currentStep;
				
 		voltageBound = (int)(maxVoltage / voltageStep) * voltageStep + voltageStep;
		
		/* old calculation method */
		/*
		double maxCurrent = currentBound;
		double minCurrent = currentLowerBound;
		double maxVoltage = voltageBound;
		
		if (maxCurrent > currentBound) {
			int step = (int)((maxCurrent - currentBound) / currentStep) + 1;
			currentBound = currentBound + step * currentStep;
		}
		if (minCurrent < currentLowerBound) {
			if (minCurrent < currentStep) {
				currentLowerBound = 0;
			} else {
				currentLowerBound = (int)(minCurrent / currentStep) * currentStep;
			}
		}
		if (maxVoltage > voltageBound) {
			int step = (int)((maxVoltage - voltageBound) / voltageStep) + 1;
			voltageBound = voltageBound + step * voltageStep;
		}
		*/
	}

	private void drawChart(PaintEvent e, PowerSourceMessage[] data, Rectangle rec, double displayTime) {
		Point[] p;
		//double time;
		//time = data[data.length - 1].getTime();
		
		//draw current
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
		
		p = new Point[data.length];
		for (int i = 0; i < data.length; i++) {
			p[i] = getCurrentPoint(rec, data[i].getCurrent(), - displayTime + data[i].getTime());
		}
		for (int i = 0; i < p.length; i++) {
			drawChartPoint(e, p[i]);
			if (i != 0) {
				e.gc.drawLine(p[i-1].x, p[i-1].y, p[i].x, p[i].y);
			}
		}
		
		//draw voltage
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
		
		p = new Point[data.length];
		for (int i = 0; i < data.length; i++) {
			p[i] = getVoltagePoint(rec, data[i].getVoltage(), - displayTime + data[i].getTime());
		}
		for (int i = 0; i < p.length; i++) {
			drawChartPoint(e, p[i]);
			if (i != 0) {
				e.gc.drawLine(p[i-1].x, p[i-1].y, p[i].x, p[i].y);
			}
		}	
	}
	
	private Point getCurrentPoint(Rectangle rec, double current, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height -  (int)(rec.height * 
				(current - currentLowerBound) / (currentBound - currentLowerBound));
		//TEST
//		ConsoleLog.print(String.format("x = %d, y = %d", x, y));
//		ConsoleLog.print(String.format("rec.x = %d, rec.y = %d, width = %d, height = %d", 
//				rec.x, rec.y, rec.width, rec.height));
		//TESTEND
		return new Point(x, y);
	}
	
	private Point getVoltagePoint(Rectangle rec, double voltage, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height - (int)(rec.height * voltage / voltageBound);
		return new Point(x, y);	
	}
	
	private void drawChartPoint(PaintEvent e, Point p) {
		e.gc.fillOval(p.x - 1, p.y - 1, 2, 2);
//		e.gc.fillOval(p.x - 2, p.y - 2, 4, 4);
	}
	
	private void drawDotLine(PaintEvent e, Rectangle rect) {
		e.gc.setLineStyle(SWT.LINE_DOT);
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_GRAY));
		
		for (double r = 0; r < 1.1; r = r + 0.2) {
			e.gc.drawLine(rect.x,
					(int)(rect.y + r * rect.height),
					rect.x + rect.width,
					(int)(rect.y + r * rect.height));
		}
		
		e.gc.setLineStyle(SWT.LINE_SOLID);
	}
	
	/*
	 * @rect : the rectangle of the display area
	 * @signal : Signal to draw
	 * @display : the left side time of the screen = current time - time bound
	 */
	private void drawSignal(PaintEvent e, Rectangle rect, 
			SignalChangedEvent signal, double displayTime) {
		
		double ratio = (signal.time - displayTime) / timeBound;
		String pin, action, mesg;
		
		pin = (signal.pin == SignalChangedEvent.PIN_CTS) ? channelName[0] : channelName[1];
		action = (signal.action == SignalChangedEvent.RISING_EDGE) ? "up" : "down";
		mesg = pin + " " + action;
		
		e.gc.setLineStyle(SWT.LINE_DASHDOTDOT);
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GREEN));
		e.gc.drawLine(rect.x + (int)(rect.width * (1-ratio)),
				rect.y, 
				rect.x + (int)(rect.width * (1-ratio)), 
				rect.y + rect.height);
		//roll back to old line style
		e.gc.setLineStyle(SWT.LINE_SOLID);		
		
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		Point p = e.gc.textExtent(mesg);
		e.gc.drawText(mesg, 
				rect.x + (int)(rect.width *(1-ratio)) - p.x / 2,
				rect.y + rect.height * 3 / 10,true);
	}
	
	private void drawCpuUsage(PaintEvent e, Rectangle rect, CpuUsageEvent[] cpuUsageEvents, double displayTime) {
		Point[] p;
		//double time;
		if (cpuUsageEvents.length != 0) {
			//time = cpuUsageEvents[cpuUsageEvents.length - 1].time;
			e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
			
			p = new Point[cpuUsageEvents.length];
			for (int i = 0; i < cpuUsageEvents.length; i++) {
				p[i] = getCpuUsagePoint(rect, cpuUsageEvents[i].CpuUsageValue, - displayTime + cpuUsageEvents[i].time);
			}
			for (int i = 0; i < p.length; i++) {
				drawChartPoint(e, p[i]);
				if (i != 0) {
					e.gc.drawLine(p[i-1].x, p[i-1].y, p[i].x, p[i].y);
				}
			}
			e.gc.drawLine(p[0].x, p[0].y, rect.x, p[0].y);
			e.gc.drawLine(p[p.length - 1].x, p[p.length - 1].y, rect.x + rect.width, p[p.length - 1].y);
		}
	}
	
	private Point getCpuUsagePoint(Rectangle rec, int cpuUsageValue, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height - (int)(rec.height * cpuUsageValue / 100);
		return new Point(x, y);	
	}
	
	private void drawCpuTmprt(PaintEvent e, Rectangle rect, CpuTmprtEvent[] cpuTmprtEvents, double displayTime) {
		Point[] p;
		//double time;
		if (cpuTmprtEvents.length != 0) {
			//time = cpuTmprtEvents[cpuTmprtEvents.length - 1].time;
			e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_YELLOW));
			
			p = new Point[cpuTmprtEvents.length];
			for (int i = 0; i < cpuTmprtEvents.length; i++) {
				p[i] = getCpuTmprtPoint(rect, cpuTmprtEvents[i].CpuTmprtValue, - displayTime + cpuTmprtEvents[i].time);
			}
			for (int i = 0; i < p.length; i++) {
				drawChartPoint(e, p[i]);
				if (i != 0) {
					e.gc.drawLine(p[i-1].x, p[i-1].y, p[i].x, p[i].y);
				}
			}
			e.gc.drawLine(p[0].x, p[0].y, rect.x, p[0].y);
			e.gc.drawLine(p[p.length - 1].x, p[p.length - 1].y, rect.x + rect.width, p[p.length - 1].y);
		}
	}
	
	private Point getCpuTmprtPoint(Rectangle rec, double cpuTmprtValue, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height - (int)(rec.height * cpuTmprtValue / 100);
		return new Point(x, y);	
	}
	
	private void drawTimeAxis(PaintEvent e, Rectangle rect) {
		e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		
		final int line = 5;
		e.gc.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y+rect.height);
		e.gc.drawLine(rect.x, rect.y + rect.height, rect.x, rect.y + rect.height + line);
		e.gc.drawLine(rect.x + rect.width, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height + line);
		drawTextDown(e, "0s", rect.x, rect.y + rect.height + line);
		drawTextDown(e, String.format("-%ds", timeBound), rect.x + rect.width, rect.y + rect.height + line);
	}
	
	private void drawCurrentAxis(PaintEvent e, Rectangle rect) {
		e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
		
		final int line = 5;
		e.gc.drawLine(rect.x, rect.y, rect.x, rect.y + rect.height);
		for (double r = 0; r < 1.1; r = r + 0.2) {
			e.gc.drawLine(rect.x, rect.y + (int)(rect.height * r), rect.x - line, rect.y + (int)(rect.height * r));
			int value = (int)(currentLowerBound + (1 - r) * (currentBound - currentLowerBound));
			drawTextLeft(e, String.format("%d mA", value), rect.x - line, rect.y + (int)(rect.height * r));
		}
		
	}
	
	private void drawVoltageAxis(PaintEvent e, Rectangle rect) {
		e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
		
		final int line = 5;
		e.gc.drawLine(rect.x + rect.width, rect.y, rect.x + rect.width, rect.y + rect.height);
		for (double r = 0; r < 1.1; r = r + 0.2) {
			e.gc.drawLine(rect.x + rect.width, 
					rect.y + (int)(rect.height * r),
					rect.x + rect.width + line,
					rect.y + (int)(rect.height * r));
			double value = voltageBound - r * voltageBound;
			drawTextRight(e, String.format("%.1f V", value),
					rect.x + rect.width + line,
					rect.y + (int)(rect.height * r));
		}
	}
	
	private void drawTextLeft(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x - p.x, y - p.y / 2);
	}
	
	private void drawTextRight(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x, y - p.y / 2);
	}
	
	private void drawTextDown(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x - p.x / 2, y);
	}
	
	public void exec(File dataFile, final int duration, boolean useSignal, String serialPort, String signalPin) 
			throws InterruptedException, SerialPortNotExist, PortInUseException, UnsupportedCommOperationException, IOException {
		final int TM_WAIT = 1000;
		final int TM_DISPLAY_INTERVAL = 100;
		Chart.useSignal = useSignal;
		
		power = new PowerSource();
		power.start();
		Thread.sleep(TM_WAIT);
		
		SerialPort portSerial;
		CommPortIdentifier identifier = Serial.getIdentifier(serialPort);
		portSerial = (SerialPort)identifier.open("SerialPort", 0);
		portSerial.setSerialPortParams(115200, 
				SerialPort.DATABITS_8, 
				SerialPort.STOPBITS_1, 
				SerialPort.PARITY_NONE);
		
		if (useSignal) {
			signal = new Signal(portSerial, System.currentTimeMillis(), power.getTime(), power);
			signal.start();
		}
		
		cpuUsage = new CpuUsage(portSerial, System.currentTimeMillis(), power.getTime(), power);
		cpuUsage.start();
		
		draw();
		
		finish = false;
		Runnable timer = new Runnable() {
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				while (true && !finish) {
					try { Thread.sleep(TM_DISPLAY_INTERVAL); } catch (Exception e) { }
					if (System.currentTimeMillis() - startTime > duration * 1000) {
						finish = true;
					}
					display.syncExec(new Runnable() {
						@Override
						public void run() {
							if (!finish)
								canvas.redraw();
						}
					});
				}		
			}
		};
		
		shell.open();
		new Thread(timer).start();
		
		while (!shell.isDisposed() && !finish) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		finish = true;
		
		Thread.sleep(TM_WAIT);
		
		//if (isPlugin) {
		//	shell.dispose();
		//} else {
			display.dispose();
		//}
		power.dispose();
		if (useSignal) {
			signal.close();
		}
		cpuUsage.close();
		//cpuTmprt.close();
		if (portSerial != null) {
			portSerial.close();
		}
		
		power.save(dataFile);
		if (useSignal) {
			signal.save(dataFile);
		}
		cpuUsage.save(dataFile);
		//cpuTmprt.save(dataFile);
		
	}
		
	public static void main(String[] args)
			throws SerialPortNotExist, PortInUseException, UnsupportedCommOperationException, IOException {
		
		finish = false;
		final Chart chart = new Chart();
		chart.isPlugin = isPlugin;
		chart.power = new PowerSource();
		chart.power.start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		SerialPort portSerial;
		CommPortIdentifier identifier = Serial.getIdentifier(chart.power.config.serialPort);
		portSerial = (SerialPort)identifier.open("SerialPort", 0);
		portSerial.setSerialPortParams(115200, 
				SerialPort.DATABITS_8, 
				SerialPort.STOPBITS_1, 
				SerialPort.PARITY_NONE);
		useSignal = true;
		signal = new Signal(portSerial, System.currentTimeMillis(), chart.power.getTime(), chart.power);
		signal.start();
		
		cpuUsage = new CpuUsage(portSerial, System.currentTimeMillis(), chart.power.getTime(), chart.power);
		cpuUsage.start();
		
		//cpuTmprt = new CpuTmprt(portSerial, System.currentTimeMillis(), chart.power.getTime(), chart.power);
		//cpuTmprt.start();
		
		chart.draw();
		
		final int DISPLAY_INTERVAL = 100;
		
		Runnable timer = new Runnable() {
			public void run() {
				while (true && !finish) {
					try {
						Thread.sleep(DISPLAY_INTERVAL);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					chart.display.syncExec(new Runnable() {
						@Override
						public void run() {
							if (!finish)
								chart.canvas.redraw();
						}
					});
				}
			}
		};
		
		chart.shell.open();
		new Thread(timer).start();
		
		while (! chart.shell.isDisposed()) {
			if (! chart.display.readAndDispatch()) {
				chart.display.sleep();
			}
		}
		
		finish = true;
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		
		if (chart.isPlugin) {
			chart.shell.dispose();
		} else {
			chart.display.dispose();
		}
		//chart.display.dispose();
		chart.power.dispose();
		signal.close();
		cpuUsage.close();
		//cpuTmprt.close();
		if (portSerial != null) {
			portSerial.close();
		}
		
		File file = new File("D:\\xfbench\\newBoard.txt");
		try {
			chart.power.save(file);
			signal.save(file);
			cpuUsage.save(file);
			//cpuTmprt.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(IAction arg0) {
		// TODO Auto-generated method stub
		try {
			isPlugin = true;
			main(null);
		} catch (SerialPortNotExist e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PortInUseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
