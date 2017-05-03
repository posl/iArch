package jp.ac.kyushu_u.iarch.checkplugin.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.NullProgressMonitor;

import jp.ac.kyushu_u.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu_u.iarch.basefunction.reader.ProjectReader;

public class AutoCheckHandler implements IHandler {

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
			CheckerWorkSpaceJob.getInstance(ProjectReader.getProject()).checkProject(new NullProgressMonitor());
		} catch (ProjectNotFoundException e) {
			// do nothing
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isHandled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener arg0) {
		// TODO Auto-generated method stub
		
	}

}
