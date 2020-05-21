package cn.edu.zju.fm.powerbench;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


class ViewData {
	
	private ArrayList<SignalChangedEvent> signalData;
	private ArrayList<CpuUsageEvent> cpuUsageData;
	private ArrayList<TcbPrintEvent> tcbPrintData;
	private ArrayList<CpuTmprtEvent> cpuTmprtData;
	private ArrayList<PowerSourceMessage> powerData;
	private double timeLowerBound;
	private double timeHigherBound;
	
	public ViewData(File file) throws IOException {
		if (file == null || !file.exists() || !file.isFile()) {
			throw new FileNotFoundException();
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		try {
			String newLine = br.readLine();
			signalData = new ArrayList<>();
			cpuUsageData = new ArrayList<>();
			tcbPrintData = new ArrayList<>();
			cpuTmprtData = new ArrayList<>();
			powerData = new ArrayList<>();
			while (newLine!= null && newLine.startsWith("p")) {
				PowerSourceMessage p = PowerSourceMessage.parse(newLine);
				if (p.getTime() != 0) {
					powerData.add(p);
				}
				newLine = br.readLine();
			}
			while (newLine != null && newLine.startsWith("s")) {
				signalData.add(SignalChangedEvent.parse(newLine));
				newLine = br.readLine();
			}
			while (newLine != null && newLine.startsWith("c")) {
				cpuUsageData.add(CpuUsageEvent.parse(newLine));
				newLine = br.readLine();
			}
			while (newLine != null && newLine.startsWith("t")) {
				tcbPrintData.add(TcbPrintEvent.parse(newLine));
				newLine = br.readLine();
			}
			while (newLine != null && newLine.startsWith("T")) {
				cpuTmprtData.add(CpuTmprtEvent.parse(newLine));
				newLine = br.readLine();
			}
			timeLowerBound = powerData.get(0).getTime();
			timeHigherBound = powerData.get(powerData.size() - 1).getTime();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			br.close();
		}
	}
	
	public SignalChangedEvent[] getSignalData(double startTime, double interval) {
		ArrayList<SignalChangedEvent> events = new ArrayList<>();
		for (SignalChangedEvent e : signalData) {
			if (e.time > startTime && e.time < startTime + interval) {
				events.add(e);
			}
		}
		SignalChangedEvent[] signalArray = new SignalChangedEvent[events.size()];
		events.toArray(signalArray);
		return signalArray;
	}
	
	public CpuUsageEvent[] getCpuUsageData(double startTime, double interval) {
		ArrayList<CpuUsageEvent> events = new ArrayList<>();
		CpuUsageEvent before = null;
		CpuUsageEvent after = null;
		for (CpuUsageEvent e : cpuUsageData) {
			if (e.time > startTime && e.time < startTime + interval) {
				events.add(e);
			}
			else if (e.time < startTime) {
				before = e;
			}
			else {
				after = e;
				break;
			}
		}
		if (after != null) {
			events.add(after);
		}
		if (before != null) {
			events.add(0, before);
		}
		CpuUsageEvent[] cpuUsageArray = new CpuUsageEvent[events.size()];
		events.toArray(cpuUsageArray);
		return cpuUsageArray;
	}
	
	public CpuTmprtEvent[] getCpuTmprtData(double startTime, double interval) {
		ArrayList<CpuTmprtEvent> events = new ArrayList<>();
		CpuTmprtEvent before = null;
		CpuTmprtEvent after = null;
		for (CpuTmprtEvent e : cpuTmprtData) {
			if (e.time > startTime && e.time < startTime + interval) {
				events.add(e);
			}
			else if (e.time < startTime) {
				before = e;
			}
			else {
				after = e;
				break;
			}
		}
		if (after != null) {
			events.add(after);
		}
		if (before != null) {
			events.add(0, before);
		}
		CpuTmprtEvent[] cpuTmprtArray = new CpuTmprtEvent[events.size()];
		events.toArray(cpuTmprtArray);
		return cpuTmprtArray;
	}
	
	public TcbPrintEvent[] getTcbPrintData(double startTime, double interval) {
		ArrayList<TcbPrintEvent> events = new ArrayList<>();
		for (TcbPrintEvent e : tcbPrintData) {
			if (e.time > startTime && e.time < startTime + interval) {
				events.add(e);
			}
		}
		TcbPrintEvent[] tcbPrintArray = new TcbPrintEvent[events.size()];
		events.toArray(tcbPrintArray);
		return tcbPrintArray;
	}
	
	public PowerSourceMessage[] getPowerData(double startTime, double interval) {
		ArrayList<PowerSourceMessage> msg = new ArrayList<>();
		PowerSourceMessage before = null;
		PowerSourceMessage after = null;
		for (PowerSourceMessage p : powerData) {
			if (p.getTime() >= startTime && p.getTime() <= startTime + interval) {
				msg.add(p);
			}
			else if (p.getTime() < startTime) {
				before = p;
			}
			else {
				after = p;
				break;
			}
		}
		if (after != null) {
			msg.add(after);
		}
		if (before != null) {
			msg.add(0, before);
		}
		PowerSourceMessage[] msgArr = new PowerSourceMessage[msg.size()];
		msg.toArray(msgArr);
		return msgArr;
	}
	
	public double getTimeHigherBound() {
		return timeHigherBound;
	}
	
	public double getTimeLowerBound() {
		return timeLowerBound;
	}
	
	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append(String.format("time %.3f - %.3f", timeLowerBound, timeHigherBound));
		str.append("\n");
		str.append(String.format("Powerdata = %d, SignalData = %d", powerData.size(), signalData.size()));
		return str.toString();
	}
	
}

public class View implements IWorkbenchWindowActionDelegate {
	private static Display display;
	private static Shell shell;
	private static Canvas canvas;
	
	private static final int currentStep = 5;
	private static final int voltageStep = 1;
	
	private static int currentBound;
	private static int voltageBound;
	private static int currentLowerBound;
	private static int timeBound = 3;
	
	private static String[] channelName = new String[]{"CTS", "DSR"};
	private static ViewData viewData;
	private static PowerSourceMessage[] powerData;
	private static SignalChangedEvent[] signalData;
	private static CpuUsageEvent[] cpuUsageData;
	private static TcbPrintEvent[] tcbPrintData;
	private static CpuTmprtEvent[] cpuTmprtData;
	private static double time;
	private static Point[] currentPoints, voltagePoints, cpuUsagePoints, cpuTmprtPoints;
	
	private static boolean mouseSelect = false;
	private static int selectId = -1;
	private static boolean selectType = true;
	
	private static Font font;
	
	public View() {
		display = Display.getDefault();
		//display = new Display();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setText("数据回看");
		shell.setSize(640, 480);
		
		Composite parent;
		parent = new Composite(shell, SWT.BORDER);
		parent.setLayout(new GridLayout(1, true));
	
		canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		font = new Font(display, "Consolas", 8, SWT.BOLD);
	}
	
	public static void init(File file) throws IOException {
		viewData = new ViewData(file);
		powerData = viewData.getPowerData(viewData.getTimeLowerBound(), timeBound);
		signalData = viewData.getSignalData(viewData.getTimeLowerBound(), timeBound);
		cpuUsageData = viewData.getCpuUsageData(viewData.getTimeLowerBound(), timeBound);
		tcbPrintData = viewData.getTcbPrintData(viewData.getTimeLowerBound(), timeBound);
		cpuTmprtData = viewData.getCpuTmprtData(viewData.getTimeLowerBound(), timeBound);
		time = viewData.getTimeLowerBound();
	}
	
	public static void draw() {
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle clientArea = canvas.getClientArea();
				final double ratio = 0.09;
				Rectangle chartArea = new Rectangle(
						clientArea.x + (int)(clientArea.width * ratio),
						clientArea.y + (int)(clientArea.height * ratio),
						(int)(clientArea.width * (1 - 2 * ratio)),
						(int)(clientArea.height * (1 - 2 * ratio)));
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
				e.gc.fillRectangle(chartArea);
				e.gc.setClipping(chartArea);
				updateBounds(powerData);
				
				drawDotLine(e, chartArea);
				
				e.gc.setFont(font);
				
				drawChart(e, powerData, chartArea);
				drawCpuUsage(e, cpuUsageData, chartArea);
				//drawTcbPrint(e, powerData, chartArea);
				drawCpuTmprt(e, cpuTmprtData, chartArea);
				
				if (powerData[0] != null) {
					double displayTime = powerData[powerData.length - 1].getTime() - timeBound;
					for (SignalChangedEvent event: signalData) {
						drawSignal(e, chartArea, event, displayTime);
					}
				}
				
				//draw point value
				e.gc.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
				e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
				if (mouseSelect) {
					mouseSelect = false;
					if (selectId < powerData.length && selectId < currentPoints.length && selectId < voltagePoints.length) {
						if (selectType) {
							//voltage
							String s = String.format("%.3fV", powerData[selectId].getVoltage())
									+ "\n" + String.format("%.3fs", powerData[selectId].getTime());
							Point extent = e.gc.stringExtent(s);
							e.gc.drawText(s, voltagePoints[selectId].x - extent.x / 2, voltagePoints[selectId].y - extent.y);
						} else {
							//current
							String s = String.format("%.3fmA", powerData[selectId].getCurrent()) 
									+ "\n" + String.format("%.3fs", powerData[selectId].getTime());
							Point extent = e.gc.stringExtent(s);
							e.gc.drawText(s, currentPoints[selectId].x - extent.x / 2, currentPoints[selectId].y - extent.y);
						}
					}
				}
				
				e.gc.setClipping(clientArea);
				drawCurrentAxis(e, chartArea);
				drawVoltageAxis(e, chartArea);
				drawTimeAxis(e, chartArea);
			}
		});
		
		canvas.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent arg) {
				if (arg.count > 0) {
					chartMove(true);
				} else if (arg.count < 0) {
					chartMove(false);
				}
			}
		});
		
		shell.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent arg0) {
				//doNothing
			}
			@Override
			public void keyPressed(KeyEvent event) {
				double lower;
				double higher;
				int step = 1;
				lower = viewData.getTimeLowerBound();
				higher = viewData.getTimeHigherBound();
				if (event.character == '-') {
					if (timeBound + step < higher - lower) {
						timeBound = timeBound + step;
						if (time + timeBound >= higher) {
							time = higher - timeBound;
						}
					}
				}
				if (event.character == '+') {
					if (timeBound - step > 0) {
						timeBound = timeBound - step;
					}
				}
				if (event.character == '*') {
					chartMove(true);
				}
				if (event.character == '/') {
					chartMove(false);
				}
				powerData = viewData.getPowerData(time, timeBound);
				signalData = viewData.getSignalData(time, timeBound);
				cpuUsageData = viewData.getCpuUsageData(time, timeBound);
				tcbPrintData = viewData.getTcbPrintData(time, timeBound);
				cpuTmprtData = viewData.getCpuTmprtData(time, timeBound);
				canvas.redraw();
			}
		});
		
		canvas.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent event) {
				if (voltagePoints == null || currentPoints == null || cpuUsagePoints == null || cpuTmprtPoints == null) {
					return;
				}
				int cnt = 0;
				for (Point vp : voltagePoints) {
					cnt++;
					if (Math.abs(vp.x - event.x) <= 2 && Math.abs(vp.y - event.y) <= 2 ) {
						mouseSelect = true;
						selectId = cnt - 1;
						selectType = true;
					}
				}
				cnt = 0;
				for (Point cp: currentPoints) {
					cnt++;
					if (Math.abs(cp.x - event.x) <= 2 && Math.abs(cp.y - event.y) <= 2) {
						mouseSelect = true;
						selectId = -1 + cnt;
						selectType = false;
					}
				}/*
				cnt = 0;
				for (Point cup: cpuUsagePoints) {
					cnt++;
					if (Math.abs(cup.x - event.x) <= 2 && Math.abs(cup.y - event.y) <= 2) {
						mouseSelect = true;
						selectId = -1 + cnt;
						selectType = false;
					}
				}*//*
				cnt = 0;
				for (Point ctp: cpuTmprtPoints) {
					cnt++;
					if (Math.abs(ctp.x - event.x) <= 2 && Math.abs(ctp.y - event.y) <= 2) {
						mouseSelect = true;
						selectId = -1 + cnt;
						selectType = false;
					}
				}*/
				canvas.redraw();
			}
		});
	}
	
	private static void chartMove(boolean direction) {
		//direct = true, right move,  direct = false, left move
		double lower =viewData.getTimeLowerBound(), higher = viewData.getTimeHigherBound();
		final double step = 0.3;
		if (direction) {
			if (time + step + timeBound < higher) {
				time += step;
			} else {
				time = higher - timeBound;
			}
		} else {
			if (time - step > lower) {
				time = time - step;
			} else {
				time = lower;
			}
		}
		powerData = viewData.getPowerData(time, timeBound);
		signalData = viewData.getSignalData(time, timeBound);
		cpuUsageData = viewData.getCpuUsageData(time, timeBound);
		tcbPrintData = viewData.getTcbPrintData(time, timeBound);
		cpuTmprtData = viewData.getCpuTmprtData(time, timeBound);
		canvas.redraw();
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
 		
	}

	private static void drawChart(PaintEvent e, PowerSourceMessage[] data, Rectangle rec) {
		double time;
		time = data[data.length - 1].getTime();
		
		//draw current
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
		
		currentPoints = new Point[data.length];
		for (int i = 0; i < data.length; i++) {
			currentPoints[i] = getCurrentPoint(rec, data[i].getCurrent(), time - data[i].getTime());
		}
		for (int i = 0; i < currentPoints.length; i++) {
			drawChartPoint(e, currentPoints[i]);
			if (i != 0) {
				e.gc.drawLine(currentPoints[i-1].x, currentPoints[i-1].y, currentPoints[i].x, currentPoints[i].y);
			}
		}
		
		//draw voltage
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_RED));
		
		voltagePoints = new Point[data.length];
		for (int i = 0; i < data.length; i++) {
			voltagePoints[i] = getVoltagePoint(rec, data[i].getVoltage(), time - data[i].getTime());
		}
		for (int i = 0; i < voltagePoints.length; i++) {
			drawChartPoint(e, voltagePoints[i]);
			if (i != 0) {
				e.gc.drawLine(voltagePoints[i-1].x, voltagePoints[i-1].y, voltagePoints[i].x, voltagePoints[i].y);
			}
		}	
	}

	private static void drawCpuUsage(PaintEvent e, CpuUsageEvent[] data, Rectangle rec) {
		if (data.length == 0) {
			return;
		}
		double time;
		time = data[data.length - 1].time;
		
		//draw current
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
		
		cpuUsagePoints = new Point[data.length];
		for (int i = 0; i < data.length; i++) {
			cpuUsagePoints[i] = getCpuUsagePoint(rec, data[i].CpuUsageValue, time - data[i].time);
		}
		for (int i = 0; i < cpuUsagePoints.length; i++) {
			drawChartPoint(e, cpuUsagePoints[i]);
			if (i != 0) {
				e.gc.drawLine(cpuUsagePoints[i-1].x, cpuUsagePoints[i-1].y, cpuUsagePoints[i].x, cpuUsagePoints[i].y);
			}
		}
	}

	private static void drawCpuTmprt(PaintEvent e, CpuTmprtEvent[] data, Rectangle rec) {
		if (data.length == 0) {
			return;
		}
		double time;
		time = data[data.length - 1].time;
		
		//draw current
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_YELLOW));
		
		cpuTmprtPoints = new Point[data.length];
		for (int i = 0; i < data.length; i++) {
			cpuTmprtPoints[i] = getCpuTmprtPoint(rec, data[i].CpuTmprtValue, time - data[i].time);
		}
		for (int i = 0; i < cpuTmprtPoints.length; i++) {
			drawChartPoint(e, cpuTmprtPoints[i]);
			if (i != 0) {
				e.gc.drawLine(cpuTmprtPoints[i-1].x, cpuTmprtPoints[i-1].y, cpuTmprtPoints[i].x, cpuTmprtPoints[i].y);
			}
		}
	}
	
	private static Point getCurrentPoint(Rectangle rec, double current, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height -  (int)(rec.height * 
				(current - currentLowerBound) / (currentBound - currentLowerBound));
		return new Point(x, y);
	}
	
	private static Point getVoltagePoint(Rectangle rec, double voltage, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height - (int)(rec.height * voltage / voltageBound);
		return new Point(x, y);	
	}
	
	private static Point getCpuUsagePoint(Rectangle rec, double cpuUsageValue, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height -  (int)(rec.height * cpuUsageValue / 100);
		return new Point(x, y);
	}
	
	private static Point getCpuTmprtPoint(Rectangle rec, double cpuTmprtValue, double time) {
		int x = rec.x + rec.width - (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height -  (int)(rec.height * cpuTmprtValue / 100);
		return new Point(x, y);
	}
	
	private static void drawChartPoint(PaintEvent e, Point p) {
		e.gc.fillOval(p.x - 1, p.y - 1, 2, 2);
	}
	
	private static void drawDotLine(PaintEvent e, Rectangle rect) {
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
	private static void drawSignal(PaintEvent e, Rectangle rect, 
			SignalChangedEvent signal, double displayTime) {
		
		double ratio = (signal.time - displayTime) / timeBound;
		String pin, action, mesg;
		
		pin = (signal.pin == SignalChangedEvent.PIN_CTS) ? channelName[0] : channelName[1];
		action = (signal.action == SignalChangedEvent.RISING_EDGE) ? "up" : "down";
		mesg = pin + " " + action;
		
		e.gc.setLineStyle(SWT.LINE_DASHDOTDOT);
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_DARK_GREEN));
		e.gc.drawLine(rect.x + (int)(rect.width * ratio),
				rect.y, 
				rect.x + (int)(rect.width * ratio), 
				rect.y + rect.height);
		//roll back to old line style
		e.gc.setLineStyle(SWT.LINE_SOLID);		
		
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		Point p = e.gc.textExtent(mesg);
		e.gc.drawText(mesg, 
				rect.x + (int)(rect.width *ratio) - p.x / 2,
				rect.y + rect.height * 3 / 10,true);
	}
	
	private static void drawTimeAxis(PaintEvent e, Rectangle rect) {
		e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		
		final int line = 5;
		e.gc.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y+rect.height);
		e.gc.drawLine(rect.x, rect.y + rect.height, rect.x, rect.y + rect.height + line);
		e.gc.drawLine(rect.x + rect.width, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height + line);
		drawTextDown(e, String.format("%.3fs", time), rect.x, rect.y + rect.height + line);
		drawTextDown(e, String.format("%.3fs", time + timeBound), rect.x + rect.width, rect.y + rect.height + line);
	}
	
	private static void drawCurrentAxis(PaintEvent e, Rectangle rect) {
		e.gc.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
		
		final int line = 5;
		e.gc.drawLine(rect.x, rect.y, rect.x, rect.y + rect.height);
		for (double r = 0; r < 1.1; r = r + 0.2) {
			e.gc.drawLine(rect.x, rect.y + (int)(rect.height * r), rect.x - line, rect.y + (int)(rect.height * r));
			double value = currentLowerBound + (1 - r) * (currentBound - currentLowerBound);
			drawTextLeft(e, String.format("%.1f mA", value), rect.x - line, rect.y + (int)(rect.height * r));
		}
		
	}
	
	private static void drawVoltageAxis(PaintEvent e, Rectangle rect) {
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
	
	private static void drawTextLeft(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x - p.x, y - p.y / 2);
	}
	
	private static void drawTextRight(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x, y - p.y / 2);
	}
	
	private static void drawTextDown(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x - p.x / 2, y);
	}
	
	public static void main(String[] args) throws IOException {
		View veiw = new View();
		FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
		fileDialog.setText("选择要查看的日志文件");
		init(new File(fileDialog.open()));
		draw();
		shell.open();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		//display.dispose();
	}
	
	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		try {
			main(null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	private IWorkbenchWindow window;
	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}
