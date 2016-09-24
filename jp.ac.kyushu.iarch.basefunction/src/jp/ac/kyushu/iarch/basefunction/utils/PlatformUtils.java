package jp.ac.kyushu.iarch.basefunction.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class PlatformUtils {

	public static Shell getActiveShell() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
			if (wbw != null) {
				return wbw.getShell();
			}
		}
		return null;
	}

	public static IEditorPart getActiveEditor() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
			if (wbw != null) {
				IWorkbenchPage wbp = wbw.getActivePage();
				if (wbp != null) {
					return wbp.getActiveEditor();
				}
			}
		}
		return null;
	}

	public static IFile getActiveFile() {
		IEditorPart activeEditor = getActiveEditor();
		if (activeEditor != null) {
			Object file = activeEditor.getEditorInput().getAdapter(IFile.class);
			if (file instanceof IFile) {
				return (IFile) file;
			}
		}
		return null;
	}

	public static IProject getActiveProject() {
		IFile file = getActiveFile();
		return file != null ? file.getProject() : null;
	}
}
