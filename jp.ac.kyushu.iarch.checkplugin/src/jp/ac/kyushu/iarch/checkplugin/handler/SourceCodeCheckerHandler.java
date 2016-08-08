package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.List;

import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.UncertainBehaviorContainer;
import jp.ac.kyushu.iarch.checkplugin.view.ArchfaceViewPart;

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class SourceCodeCheckerHandler {
	public void showArchface(List<ComponentClassPairModel> classPairs, List<UncertainBehaviorContainer> list) {
		ArchfaceViewPart archfaceView = null;
		IWorkbenchWindow[] views = PlatformUI.getWorkbench()
				.getWorkbenchWindows();
		for (IViewReference view : views[0].getActivePage().getViewReferences()) {
			if (view.getId().equals(ArchfaceViewPart.ID)) {
				archfaceView = (ArchfaceViewPart) view.getView(true);

			}
		}
		if (archfaceView == null) { // if view not open.
			try {
				archfaceView = (ArchfaceViewPart) PlatformUI
						.getWorkbench().getActiveWorkbenchWindow()
						.getActivePage().showView(ArchfaceViewPart.ID);
			} catch (PartInitException pie) {
				pie.printStackTrace();
				return;
			}
		}
		archfaceView.setModels(classPairs, list);
	}

}
