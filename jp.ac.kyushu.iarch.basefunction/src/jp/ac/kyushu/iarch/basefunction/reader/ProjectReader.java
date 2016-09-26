package jp.ac.kyushu.iarch.basefunction.reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import jp.ac.kyushu.iarch.basefunction.exception.ProjectNotFoundException;
/**
 * A class for getting current project when using diagram editor.
 * 
 * @author Templar
 *
 */
public class ProjectReader {
	
	private static ProjectReader pr = new ProjectReader();
	private ProjectReader(){
		
	}
	
	public static ProjectReader getInstance(){
		return pr;
	}
	public static IProject getProject(){
		IProject project = null;
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor != null) {
			Object object = activeEditor.getEditorInput().getAdapter(IFile.class);
			if (object != null) {  
				project = ((IFile)object).getProject();  
			}
		}
		if (project == null) {
			throw new ProjectNotFoundException();
		} else {
			return project;
		}
	}

}
