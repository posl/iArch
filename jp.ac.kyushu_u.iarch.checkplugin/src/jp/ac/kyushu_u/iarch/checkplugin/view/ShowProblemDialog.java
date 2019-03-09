package jp.ac.kyushu_u.iarch.checkplugin.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ShowProblemDialog extends Dialog {

	private static final String TITLE = "Problems";
	private static final Point initialSize = new Point(960, 640);

	private String title = null;
	private Text text = null;
	private String message = null;

	public ShowProblemDialog(Shell parentShell) {
		this(parentShell, TITLE);
	}
	public ShowProblemDialog(Shell parentShell, String title) {
		super(parentShell);
		this.title = title;
	}

	@Override
	protected Point getInitialSize() {
		return initialSize;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		text = new Text(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setLayoutData(new GridData(GridData.FILL_BOTH));
		if (message != null) {
			text.setText(message);
		}
		text.setEditable(false);

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	public void setMessage(String newCode) {
		message = newCode;
		if (text != null) {
			text.setText(message != null ? message : "");
		}
	}
}
