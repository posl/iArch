package jp.ac.kyushu_u.iarch.checkplugin.handler;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import jp.ac.kyushu_u.iarch.basefunction.utils.ProblemViewManager;

/**
 * Create a new job to check after project saving.
 */
public class CheckerWorkSpaceJob extends Job {
	private IProject proj = null;

	private static String Name = "Archface Auto-check";

	private CheckerWorkSpaceJob() {
		super(Name);
	}
	private CheckerWorkSpaceJob(IProject project) {
		super(Name);
		proj = project;
	}

	public static CheckerWorkSpaceJob getInstance(IProject project) {
		return new CheckerWorkSpaceJob(project);
	}

	public IStatus checkProject(IProgressMonitor monitor) {
		return run(monitor);
	}

	// A new job to do the check
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (proj == null) {
			System.out.println("Project is null: Auto-check");
			return Status.CANCEL_STATUS;
		}

		// To avoid a parallel check, which may disturb the result markers.
		synchronized (proj) {
			final ArchfaceChecker archfaceChecker = new ArchfaceChecker(proj);
			if (!archfaceChecker.hasConfig()) {
				System.out.println("Project does not have the config file: Auto-check");
				return Status.CANCEL_STATUS;
			}

			ProblemViewManager.removeAllProblems(proj);
			archfaceChecker.checkProject();

			// Processes to set results to the view will be serialized in the UI thread.
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public synchronized void run() {
					SourceCodeCheckerHandler srchandler = new SourceCodeCheckerHandler();
					srchandler.showArchface(archfaceChecker.getClassPairs(),
							archfaceChecker.getBehaviorPairs(),
							archfaceChecker.getAbstractionRatio());
				}
			});
		}
		monitor.done();
		return Status.OK_STATUS;
	}
}
