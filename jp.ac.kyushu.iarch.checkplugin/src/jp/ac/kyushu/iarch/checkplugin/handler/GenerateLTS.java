package jp.ac.kyushu.iarch.checkplugin.handler;

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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.basefunction.utils.MessageDialogUtils;
import jp.ac.kyushu.iarch.checkplugin.view.SelectArchfile;
import jp.ac.kyushu.iarch.checkplugin.view.ShowFSPDialog;

public class GenerateLTS implements IHandler {

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
			MessageDialogUtils.showError("Generate LTS file", "Project not found.");
			return null;
		}

		Shell shell = HandlerUtil.getActiveShell(event);
		SelectArchfile archfile = new SelectArchfile(shell, project);
		if (archfile.open() == MessageDialog.OK) {
			String code = getSequenceCode(archfile.getArchiface());
			if (code == null) {
				code = "null"; // backward compatibility
			}

			writeLTSCode(project, code);

			ShowFSPDialog dialog = new ShowFSPDialog(shell);
			dialog.setCode(code);
			dialog.open();
		}
		return null;
	}

	private void writeLTSCode(IProject project, String code) {
		String projectPath = project.getLocation().toOSString();
		String ltsCodeFile = projectPath + "/Gen-LTS.lts";
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
		return cfsp.convert(archModel);
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
