package jp.ac.kyushu_u.iarch.basefunction.utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public class MessageDialogUtils {

	public static void showInfo(String title, String message) {
		showInfo(PlatformUtils.getActiveShell(), title, message);
	}
	public static void showInfo(Shell parent, String title, String message) {
		MessageDialog.open(MessageDialog.INFORMATION, parent, title, message, SWT.None);
	}

	public static void showWarning(String title, String message) {
		showWarning(PlatformUtils.getActiveShell(), title, message);
	}
	public static void showWarning(Shell parent, String title, String message) {
		MessageDialog.open(MessageDialog.WARNING, parent, title, message, SWT.None);
	}

	public static void showError(String title, String message) {
		showError(PlatformUtils.getActiveShell(), title, message);
	}
	public static void showError(Shell parent, String title, String message) {
		MessageDialog.open(MessageDialog.ERROR, parent, title, message, SWT.None);
	}
}
