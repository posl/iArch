package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu_u.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu_u.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu_u.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu_u.iarch.basefunction.utils.MessageDialogUtils;
import jp.ac.kyushu_u.iarch.checkplugin.view.ShowFSPDialog;

public class GenerateLTS2 implements IHandler {

	private static final String HANDLER_TITLE = "Generate P-FSP for LTS (trial)";

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = null;
		try {
			project = ProjectReader.getProject();
		} catch (ProjectNotFoundException e) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Project not found.");
			return null;
		}

		IResource archfile = new XMLreader(project).getArchfileResource();
		if (archfile == null) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Archfile not found.");
			return null;
		}

		// Create FSP.
		String code = getSequenceCode(archfile);
		if (code == null) {
			code = "null"; // backward compatibility
		}

		// Write to a file.
		writeLTSCode(project, code);

		// Show FSP in a dialog.
		Shell shell = HandlerUtil.getActiveShell(event);
		ShowFSPDialog dialog = new ShowFSPDialog(shell, "Generated P-FSP");
		dialog.setCode(code);
		dialog.open();

		return null;
	}

	private void writeLTSCode(IProject project, String code) {
		String projectPath = project.getLocation().toOSString();
		String ltsCodeFile = projectPath + "/Gen-LTS2.lts";
		File myFilePath = new File(ltsCodeFile);
		FileWriter resultFile = null;
		try {
			if (!myFilePath.exists()) {
				myFilePath.createNewFile();
			}
			resultFile = new FileWriter(myFilePath);
			PrintWriter myFile = new PrintWriter(resultFile);
			myFile.print(code);
			myFile.close();
		} catch (IOException e) {
			System.out.println("Create file error");
			e.printStackTrace();
		} finally {
			try {
				if (resultFile != null) {
					resultFile.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String getSequenceCode(IResource archcode) {
		ArchModel archmodel = new ArchModel(archcode);
		Model archModel = archmodel.getModel();
		ConnectorToFSP cfsp = new ConnectorToFSP();
		return cfsp.convert(archModel, true);
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
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}

}
