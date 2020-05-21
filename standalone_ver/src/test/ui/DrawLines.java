package test.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DrawLines {
	
	private static Display display = new Display();
	private static Shell shell = new Shell(display);
	
	public static void drawOnImage() {
		Label label = new Label(shell, SWT.CENTER);
		Image image = new Image(display, "C:\\Users\\tony\\Pictures\\ppt\\20160802223738122.png");
		
		GC gc = new GC(image);
		Rectangle bounds = image.getBounds();
		gc.drawLine(0, bounds.height/2, bounds.width, bounds.height/2);
		gc.dispose();
		
		label.setImage(image);
		label.pack();
		
		shell.pack();
	}
	
	public static void drawControl() {
		Label label = new Label(shell, SWT.CENTER);
		Image image = new Image(display, "C:\\Users\\tony\\Pictures\\ppt\\20160802223738122.png");
		
		label.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent arg0) {
				Rectangle bounds = label.getBounds();
				arg0.gc.drawLine(0, bounds.height/2, bounds.width, bounds.height/2);
			}
		});
		
		label.setImage(image);
		label.pack();
		
		shell.pack();
	}
	
	public static void clipping() {
		shell.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent arg0) {
				Rectangle clientArea = shell.getClientArea();
				arg0.gc.setClipping(20, 20, clientArea.width - 40, clientArea.height - 40);
				arg0.gc.setBackground(display.getSystemColor(SWT.COLOR_CYAN));
				arg0.gc.fillPolygon(new int[]{0, 0, clientArea.width, 0, clientArea.width/2, clientArea.height});
			}
		});
		shell.setSize(150, 150);
	}
	
	public static void drawChart() {
		shell.setLayout(new FillLayout());
		shell.setText("Chart");
		final Canvas canvas = new Canvas(shell, SWT.NONE);
		
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle clientArea = canvas.getClientArea();
//				e.gc.drawLine(0, clientArea.height/2, clientArea.width, clientArea.height/2);
				Font font = new Font(display, "Consolas", 14, SWT.NONE);
				e.gc.setForeground(display.getSystemColor(SWT.COLOR_CYAN));
				e.gc.setFont(font);
				e.gc.drawText("Hello hello", 0, 0);
				e.gc.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
				e.gc.drawLine(0, 19, clientArea.width, 19);
				font.dispose();
			}
		});
		
		shell.setSize(640, 480);
	}
	
	public static void main(String[] args) {
//		clipping();
		drawChart();
		
		shell.open();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		
		display.dispose();
	}
	
}
