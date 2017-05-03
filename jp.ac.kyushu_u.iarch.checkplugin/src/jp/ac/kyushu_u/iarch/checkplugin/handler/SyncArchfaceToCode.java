package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu_u.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu_u.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu_u.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu_u.iarch.basefunction.utils.MessageDialogUtils;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class SyncArchfaceToCode implements IHandler {

	private static final String HANDLER_TITLE = "Sync: iArch -> Code";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the project.
		IProject project = null;
		try {
			project = ProjectReader.getProject();
		} catch (ProjectNotFoundException e) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Project not found.");
			return null;
		}

		// Get Archface model.
		IResource archfile = new XMLreader(project).getArchfileResource();
		if (archfile == null) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Failed to get the archfile resource.");
			return null;
		}
		ArchModel archModel = new ArchModel(archfile);
		Model model = archModel.getModel();

		generateCodeFromModel(model, project);

		return null;
	}

	private void generateCodeFromModel(Model model, IProject project) {
		// Get (or create) a target directory.
		// TODO: Get appropriate directories to synchronize.
		IFolder folder = project.getFolder("src-gen");
		if (!folder.exists()) {
			try {
				folder.create(false, true, null);
			} catch (CoreException e) {
				MessageDialogUtils.showError(HANDLER_TITLE, "Failed create a directory to generate codes.");
				return;
			}
		}

		List<String> failedClasses = new ArrayList<String>();

		for (Interface cInterface : model.getInterfaces()) {
			// Get corresponding file.
			String className = cInterface.getName();
			IFile file = folder.getFile(className + ".java");
			IJavaElement je = JavaCore.create(file);
			if (je instanceof ICompilationUnit) {
				ICompilationUnit cu = null;
				boolean modified = false;

				try {
					cu = ((ICompilationUnit) je).getWorkingCopy(null);

					// Get (or create) class.
					IType type = cu.getType(className);
					if (!type.exists()) {
						String classContents = "class " + className + " {\n}";
						type = cu.createType(classContents, null, false, null);
						modified = true;
					}

					// Create methods if not exist.
					for (Method method : cInterface.getMethods()) {
						String methodName = method.getName();
						// TODO: method arguments.
						IMethod m = type.getMethod(methodName, new String[0]);
						if (!m.exists()) {
							String methodContents = method.getType() + " " + methodName + "() {}\n";
							type.createMethod(methodContents, null, false, null);
							modified = true;
						}
					}

					// Save if modified.
					if (modified) {
						cu.commitWorkingCopy(false, null);
					} else {
						cu.discardWorkingCopy();
					}
					cu = null;
				} catch (JavaModelException e) {
					failedClasses.add(className);
				} finally {
					if (cu != null) {
						try {
							cu.discardWorkingCopy();
						} catch (JavaModelException e) {
						}
					}
				}
			}
		}

		if (!failedClasses.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append("Failed create class(es):");
			for (String fc : failedClasses) {
				sb.append(" ").append(fc);
			}
			MessageDialogUtils.showError(HANDLER_TITLE, sb.toString());
		}
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
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

}
