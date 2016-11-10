package jp.ac.kyushu.iarch.checkplugin.handler;

import jp.ac.kyushu.iarch.basefunction.utils.ProblemViewManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

/**
 * Create a new job to check after project saving.
 */
public class CheckerWorkSpaceJob extends Job {
	private IProject proj = null;
	private static String Name = "Archface Auto-check";
	private static CheckerWorkSpaceJob cwsJob = new CheckerWorkSpaceJob();

	private CheckerWorkSpaceJob() {
		super(Name);
	}

	public static CheckerWorkSpaceJob getInstance(IProject project) {
		cwsJob.proj = project;
		return cwsJob;
	}
	
	public IStatus checkProject(IProgressMonitor monitor) {
		return run(monitor);
	}

	// A new job to do the check
	protected IStatus run(IProgressMonitor monitor) {
		if (proj == null) {
			System.out.println("Project is null: Auto-check");
			return Status.CANCEL_STATUS;
		}
		ProblemViewManager.removeAllProblems(proj);
		final ArchfaceChecker archfaceChecker = new ArchfaceChecker(proj);
		archfaceChecker.checkProject();

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public synchronized void run() {
				SourceCodeCheckerHandler srchandler = new SourceCodeCheckerHandler();
				srchandler.showArchface(archfaceChecker.getClassPairs(), archfaceChecker.getBehaviorPairs());
			}
		});
		monitor.done();
		return Status.OK_STATUS;
	}
}
