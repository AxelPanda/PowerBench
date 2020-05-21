package org.fmlab.evaluation.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.fmlab.assiatant.AssistBoard;
import org.fmlab.assiatant.AssistBoardData;

public class Evaluation implements IWorkbenchWindowActionDelegate{
	
	private IWorkbenchWindow window;

	@Override
	public void run(IAction action) {
		AssistBoardData assistData = new AssistBoardData();
		AssistBoard assistBoard = new AssistBoard("COM3", assistData, 50000);
		
		assistBoard.start();
//		AssistBoardData.test();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

}
