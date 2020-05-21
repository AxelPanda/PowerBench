package org.fmlab.evaluation.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.fmlab.assiatant.AssistBoardData;
import org.fmlab.assiatant.EventItem;
import org.fmlab.assiatant.PowerDataItem;

public class PowerView extends ViewPart{
	
	private Canvas canvas;
	public PowerDataItem[] data;
	public EventItem[] events;
	
	private static final int currentStep = 5;
	public static int timeBound = 5000;
	private Font font;
	private int currentBound;
	private int currentLowerBound;
	
	@Override
	public void createPartControl(Composite parent) {
		AssistBoardData.powerView = this;
		
		canvas = new Canvas(parent, SWT.NONE);
		font = new Font(canvas.getDisplay(), "Consolas", 7, SWT.BOLD);
		currentBound = 5;
		currentLowerBound = 0;
		canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				System.out.println("In Paint Control");
				Rectangle clientArea = canvas.getBounds();
				final double ratio = 0.13;
				Rectangle chartArea = new Rectangle(
						clientArea.x + (int)(clientArea.width * ratio),
						clientArea.y + (int)(clientArea.height * ratio),
						(int)(clientArea.width * (1 - 2 * ratio)),
						(int)(clientArea.height * (1 - 2 * ratio)));
				event.gc.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
				event.gc.fillRectangle(chartArea);
				event.gc.setClipping(chartArea);
				
				updateBounds();
				
				drawDotLine(event, chartArea);
				
				drawChart(event, chartArea);
				
				event.gc.setClipping(clientArea);
				event.gc.setFont(font);
				drawCurrentAxis(event, chartArea);
				drawTimeAxis(event, chartArea);
				
				if (events != null) {
					for (EventItem item: events) {
						drawSignal(event, chartArea, item);
					}
				}

			}
		});
	}

	@Override
	public void setFocus() {
		canvas.setFocus();
		// ODO Auto-generated method stub
		
	}

	private void updateBounds() {
		double maxCurrent = 0;
		double minCurrent = 0;
		boolean set = false;
		if (data == null) {
			return;
		}
		for (PowerDataItem item: data) {
			if (!set) {
				maxCurrent = item.current;
				minCurrent = item.current;
				set = true;
			} else {
				if (item.current > maxCurrent) {
					maxCurrent = item.current;
				}
				if (item.current < minCurrent) {
					minCurrent = item.current;
				}
			}
		}	
		currentBound = maxCurrent >= 0?
				(int)(maxCurrent / currentStep) * currentStep + currentStep :
				(int)(maxCurrent / currentStep) * currentStep;
		currentLowerBound = minCurrent >= 0?
				(int)(minCurrent / currentStep) * currentStep :
				(int)(minCurrent / currentStep) * currentStep - currentStep;
	}
	
	private void drawDotLine(PaintEvent e, Rectangle rect) {
		e.gc.setLineStyle(SWT.LINE_DOT);
		e.gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_GRAY));
		
		for (double r = 0; r < 1.1; r = r + 0.2) {
			e.gc.drawLine(rect.x,
					(int)(rect.y + r * rect.height),
					rect.x + rect.width,
					(int)(rect.y + r * rect.height));
		}
		
		e.gc.setLineStyle(SWT.LINE_SOLID);
	}
	
	private void drawChart(PaintEvent e, Rectangle rec) {
		Point[] p;
		double time;
		
		if (data == null) {
			return;
		}
		time = data[0].time;
		
		//draw current
		e.gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLUE));
		
		p = new Point[data.length];
		for (int i = 0; i < data.length; i++) {
			p[i] = getCurrentPoint(rec, data[i].current, - time + data[i].time);
		}
		for (int i = 0; i < p.length; i++) {
			drawChartPoint(e, p[i]);
			if (i != 0) {
				e.gc.drawLine(p[i-1].x, p[i-1].y, p[i].x, p[i].y);
			}
		}
		
	}
	
	private Point getCurrentPoint(Rectangle rec, double current, double time) {
		int x = rec.x + (int)(rec.width * time / timeBound);
		int y = rec.y + rec.height -  (int)(rec.height * 
				(current - currentLowerBound) / (currentBound - currentLowerBound));
		return new Point(x, y);
	}
	
	private void drawChartPoint(PaintEvent e, Point p) {
		e.gc.fillOval(p.x - 1, p.y - 1, 2, 2);
		// draw chart point
	}
	private void drawSignal(PaintEvent e, Rectangle rect, 
			EventItem event) {
		
		double ratio = event.time * 1.0 / timeBound;
		String pin, action, mesg;
		
		pin = String.format("%d", event.id);
		action = (event.type == 1) ? "up" : "down";
		mesg = pin + " " + action;
		
		e.gc.setLineStyle(SWT.LINE_DASHDOTDOT);
		e.gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		e.gc.drawLine(rect.x + (int)(rect.width * ratio),
				rect.y, 
				rect.x + (int)(rect.width * ratio), 
				rect.y + rect.height);
		//roll back to old line style
		e.gc.setLineStyle(SWT.LINE_SOLID);		
		
		e.gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		Point p = e.gc.textExtent(mesg);
		e.gc.drawText(mesg, 
				rect.x + (int)(rect.width *ratio) - p.x / 2,
				rect.y + rect.height * 3 / 10,true);
	}
	private void drawCurrentAxis(PaintEvent e, Rectangle rect) {
		e.gc.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		e.gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLUE));
		
		final int line = 5;
		e.gc.drawLine(rect.x, rect.y, rect.x, rect.y + rect.height);
		for (double r = 0; r < 1.1; r = r + 0.2) {
			e.gc.drawLine(rect.x, rect.y + (int)(rect.height * r), rect.x - line, rect.y + (int)(rect.height * r));
			 double value = currentLowerBound + (1 - r) * (currentBound - currentLowerBound);
			drawTextLeft(e, String.format("%.2f mA", value), rect.x - line, rect.y + (int)(rect.height * r));
		}
		
	}
	
	private void drawTimeAxis(PaintEvent e, Rectangle rect) {
		e.gc.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		e.gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLUE));
		
		final int line = 5;
		e.gc.drawLine(rect.x, rect.y + rect.height, rect.x + rect.width, rect.y+rect.height);
		e.gc.drawLine(rect.x, rect.y + rect.height, rect.x, rect.y + rect.height + line);
		e.gc.drawLine(rect.x + rect.width, rect.y + rect.height, rect.x + rect.width, rect.y + rect.height + line);
		drawTextDown(e, "0s", rect.x, rect.y + rect.height + line);
		drawTextDown(e, String.format("+%ds", (int)(timeBound * 0.001)), rect.x + rect.width, rect.y + rect.height + line);
	}
	
	private void drawTextLeft(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x - p.x, y - p.y / 2);
	}
	
	private void drawTextDown(PaintEvent e, String text, int x, int y) {
		Point p = e.gc.textExtent(text);
		e.gc.drawText(text, x - p.x / 2, y);
	}
	
	public void setData(PowerDataItem[] data, EventItem[] event) {
		this.data = data;
		events = event;
		System.out.println("In function() setData");
		canvas.getDisplay().asyncExec(
			    new Runnable() { 
					public void run(){ 
						canvas.redraw(); 
					} 
				}); 
	}
	
	public void stop() {
		events = null;
		data = null;
		currentLowerBound = 0;
		currentBound = 5;
//		canvas.redraw();
		canvas.getDisplay().syncExec( 
			    new Runnable() { 
					public void run(){ 
						canvas.redraw(); 
					} 
				}); 
	}
	
}
