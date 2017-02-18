package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.List;

import jp.ac.kyushu.iarch.checkplugin.model.AbstractionRatio;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.UncertainBehaviorContainer;
import jp.ac.kyushu.iarch.checkplugin.view.ArchfaceViewPart;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class SourceCodeCheckerHandler {
	public void showArchface(List<ComponentClassPairModel> classPairs,
			List<UncertainBehaviorContainer> behaviorContainers,
			AbstractionRatio abstractionRatio) {
		ArchfaceViewPart archfaceView = null;
		IWorkbenchWindow[] views = PlatformUI.getWorkbench()
				.getWorkbenchWindows();
		if (views.length > 0) {
			for (IViewReference view : views[0].getActivePage().getViewReferences()) {
				if (view.getId().equals(ArchfaceViewPart.ID)) {
					// Even when ID check is OK, obtained IViewPart happens to be
					// different kind (we don't know why...) so type check is performed.
					IViewPart viewPart = view.getView(true);
					if (viewPart instanceof ArchfaceViewPart) {
						archfaceView = (ArchfaceViewPart) viewPart;
					}
				}
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
		archfaceView.setModels(classPairs, behaviorContainers);
		archfaceView.setAbstractionRatio(abstractionRatio);
	}

}
