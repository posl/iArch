package jp.ac.kyushu_u.iarch.checkplugin.view;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ReceiveOutputDialog extends Dialog {

	private static final String TITLE = "Receive Output";
	private static final Point initialSize = new Point(960, 640);

	private String title;
	private Text text = null;
	private String result = null;

	public ReceiveOutputDialog(Shell parentShell) {
		this(parentShell, TITLE);
	}
	public ReceiveOutputDialog(Shell parentShell, String title) {
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
		text.setEditable(true);

		return composite;
	}

	@Override
	protected void okPressed() {
		result = text.getText();
		super.okPressed();
	}

	public String getResult() {
		return result;
	}
}
