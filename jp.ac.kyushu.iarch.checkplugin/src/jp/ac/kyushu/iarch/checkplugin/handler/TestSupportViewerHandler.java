package jp.ac.kyushu.iarch.checkplugin.handler;

import jp.ac.kyushu.iarch.checkplugin.view.TestSupportViewPart;

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class TestSupportViewerHandler {

	public void refresh() {
		TestSupportViewPart testSupportView = null;
		IWorkbenchWindow[] views = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IViewReference view : views[0].getActivePage().getViewReferences()) {
			if (view.getId().equals(TestSupportViewPart.ID)) {
				testSupportView = (TestSupportViewPart) view.getView(true);
				testSupportView.refresh();
			}
		}
	}

}
