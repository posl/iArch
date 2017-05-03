package jp.ac.kyushu_u.iarch.checkplugin.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;

import jp.ac.kyushu_u.iarch.checkplugin.Activator;
import jp.ac.kyushu_u.iarch.checkplugin.testsupport.AspectGenerator;

public class RemoveAspectCode implements IHandler {

	@Override
	public void addHandlerListener(IHandlerListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		try {
			AspectGenerator.removeAspectCode();
		} catch (Exception e) {
			ErrorDialog.openError(Display.getDefault().getActiveShell(), null, null, new Status(
					IStatus.ERROR, Activator.PLUGIN_ID,
					"An error occurred during processing.", e));
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener arg0) {
		// TODO Auto-generated method stub
		
	}

}
