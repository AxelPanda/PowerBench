package cn.edu.zju.fm.powerbench;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DisplayCurrent extends Thread {
	
	private static Label lVoltage, lCurrent, voltageValue, currentValue, lV, lmA;
	private static boolean finish;
	
	public void run() {
		finish = false;
		main(null);
	}
	
	public void setFinish() {
		finish = true;
	}

	public static void main(String[] args) {
		Display display = new Display();
		
		//power thread create
		final PowerSource power = new PowerSource();
		power.start();
		
		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setText("Display Current");
		
		shell.setLayout(new GridLayout(3, false));
		
		lVoltage = new Label(shell, SWT.RIGHT);
		lVoltage.setText("VOLTAGE = ");
		voltageValue = new Label(shell, SWT.LEFT);
		voltageValue.setText("0.000");
		lV = new Label(shell, SWT.LEFT);
		lV.setText("V");
		lCurrent = new Label(shell, SWT.RIGHT);
		lCurrent.setText("CURRENT = ");
		currentValue = new Label(shell, SWT.LEFT);
		currentValue.setText("000.000");
		lmA = new Label(shell, SWT.LEFT);
		lmA.setText("mA");
		
		final Runnable timer = new Runnable() {
			@Override
			public void run() {
				synchronized (this) {
					if (!finish) {
						voltageValue.setText(String.format("%.3f", power.getVoltage()));
						currentValue.setText(String.format("%.3f", power.getCurrent()));	
					}
				}
			}
		};
		
		shell.pack();
		shell.open();
		shell.setFocus();
		
		while (!shell.isDisposed() && !finish) {
			if (!display.readAndDispatch()) {
				display.sleep();
			} else {
				display.timerExec(200, timer);
			}
		}
		
		display.dispose();
		power.dispose();
	}
	
}
