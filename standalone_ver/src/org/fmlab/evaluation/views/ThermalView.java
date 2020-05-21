package org.fmlab.evaluation.views;


import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.*;
import org.fmlab.assiatant.AssistBoardData;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;



/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class ThermalView extends ViewPart {
	private Canvas canvas;
	private double data[];
	
	public void setData(double data[]) {
		this.data = data;
		canvas.getDisplay().syncExec( 
		    new Runnable() { 
				public void run(){ 
					canvas.redraw(); 
				} 
			}); 
	}
	
	public void stopDisplay() {
		double data[] = new double[64];
		for (int i = 0; i < 64; i++) {
			data[i] = 0;
		}
		setData(data);
	}
	
	private void paintGrid(GC gc) {
		Rectangle rect = canvas.getBounds();
		int length = Math.min(rect.height, rect.width * 8 / 10) * 8 / 10;
		int ilength = length / 8;
		Point center = new Point(rect.width * 4 / 10, rect.height / 2);
		Point spoint = new Point(center.x - ilength * 4, center.y - 4 * ilength);
		
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				int id = i * 8 + j;
				Point lefttop = new Point(spoint.x + j * ilength, spoint.y + i * ilength);
				gc.drawRectangle(lefttop.x, lefttop.y, ilength, ilength);
				if (data[id] > 35) {
					data[id] = 35;
				} else if (data[id] < 25 && data[id] > 10) {
					data[id] = 25;
				}
				if (data[id] >= 25 && data[id] <= 35) {
					double ratio = 1. * (data[id] - 25) / 10;
					int r = (int)(ratio * 255); int green = 70;
					int blue = (int)(-ratio * 255 + 255);
					gc.setBackground(new Color(canvas.getDisplay(), r, green, blue));
				} else {
					gc.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
				}
				gc.fillRectangle(lefttop.x+1, lefttop.y+1, ilength-1, ilength-1);
			}
		}
	}
	
	public void paintBar(GC gc) {
		Rectangle rect = canvas.getBounds();
		Point center = new Point(rect.width * 9 / 10, rect.height / 2);
		int height = rect.height * 8 / 10;
		int width = rect.width * 1 / 20;
		Point spoint = new Point(center.x - width / 2, center.y - height / 2);
		
		gc.setForeground(new Color(canvas.getDisplay(), 255, 70, 0));
		gc.setBackground(new Color(canvas.getDisplay(), 0, 70, 255));
		gc.fillGradientRectangle(spoint.x, spoint.y, width, height, true);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		canvas = new Canvas(parent, SWT.NONE);
		data = new double[64];
		
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent event) {
				paintGrid(event.gc);
				paintBar(event.gc);
			}
		});
		
		AssistBoardData.thermalView = this;
	}
	
	@Override
	public void setFocus() {
		canvas.setFocus();
	}


}
