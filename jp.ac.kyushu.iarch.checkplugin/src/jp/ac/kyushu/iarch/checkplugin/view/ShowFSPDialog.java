package jp.ac.kyushu.iarch.checkplugin.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ShowFSPDialog extends Dialog {

	private static final String TITLE = "Generated FSP";
	private static final Point initialSize = new Point(640, 480);

	private Text text = null;
	private String code = null;

	public ShowFSPDialog(Shell parentShell) {
		super(parentShell);
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
		newShell.setText(TITLE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		text = new Text(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setLayoutData(new GridData(GridData.FILL_BOTH));
		if (code != null) {
			text.setText(code);
			text.selectAll();
		}
		text.setEditable(false);

		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}

	public void setCode(String newCode) {
		code = newCode;
		if (text != null) {
			text.setText(code != null ? code : "");
			text.selectAll();
		}
	}
}
