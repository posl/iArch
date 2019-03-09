package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.util.List;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu_u.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu_u.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu_u.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu_u.iarch.basefunction.utils.MessageDialogUtils;
import jp.ac.kyushu_u.iarch.checkplugin.handler.AnalyzeCounterexample.DubiousPoint;
import jp.ac.kyushu_u.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu_u.iarch.checkplugin.view.ReceiveOutputDialog;
import jp.ac.kyushu_u.iarch.checkplugin.view.ShowProblemDialog;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class ReceiveCounterexample implements IHandler {

	private static final String HANDLER_TITLE = "Receive LTSA-PCA output";

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Open a dialog to receive result of LTSA-PCA tool.
		Shell shell = HandlerUtil.getActiveShell(event);
		ReceiveOutputDialog dialog = new ReceiveOutputDialog(shell, HANDLER_TITLE);
		int ret = dialog.open();

		if (ret == IDialogConstants.OK_ID) {
			// Receive output from the dialog.
			String output = dialog.getResult();

			// Get Archface model.
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
			Model model = (new ArchModel(archfile)).getModel();

			// Get dubious points.
			AnalyzeCounterexample ac = new AnalyzeCounterexample();
			List<DubiousPoint> points = ac.analyze(model, output);

			if (points.isEmpty()) {
				MessageDialogUtils.showInfo(HANDLER_TITLE, "Problems are not found.");
			} else {
				String problemMessage = makeOutput(points);
				//System.out.print(problemMessage);
				ShowProblemDialog problemDialog = new ShowProblemDialog(shell, "Problem candidates");
				problemDialog.setMessage(problemMessage);
				problemDialog.open();
			}
		}

		return null;
	}

	private String makeOutput(List<DubiousPoint> points) {
		StringBuffer sb = new StringBuffer();

		int count = 1;
		for (DubiousPoint point : points) {
			if (point != points.get(0)) {
				sb.append("\n");
			}

			sb.append("#").append(count++).append("\nScore: ")
				.append(point.score).append("\n");

			// element is either Behavior or UncertainBehavior.
			if (point.element instanceof Behavior) {
				sb.append("Behavior type: certain\n");
			} else if (point.element instanceof UncertainBehavior) {
				sb.append("Behavior type: uncertain\n");
			}
			String s = ArchModelUtils.serialize(point.element);
			// hide annotations.
			s = s.replaceAll("@\\S+(?:\\s*\\([^\\)]+\\))?", "");
			// compress white spaces.
			s = s.replaceAll("\\s+", " ").trim();
			sb.append("\t").append(s).append("\n");

			sb.append("Method(s):\n");
			for (EObject p : point.points) {
				if (p instanceof Method) {
					Method m = (Method) p;
					String mn = m.getName();
					String cn = ArchModelUtils.getContainedClassName(m);

					sb.append("\t");
					if (cn != null) {
						sb.append(cn).append(".");
					}
					sb.append(mn).append("\n");
				}
			}
		}

		return sb.toString();
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
