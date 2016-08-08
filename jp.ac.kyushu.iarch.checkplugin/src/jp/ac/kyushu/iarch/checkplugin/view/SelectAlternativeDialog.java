package jp.ac.kyushu.iarch.checkplugin.view;

import java.util.regex.Pattern;

import jp.ac.kyushu.iarch.archdsl.archDSL.Model;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SelectAlternativeDialog extends Dialog {
	private static final String TITLE = "Select Alternative Methods";
	private static final Point initialSize = new Point(480, 320);
	private static final Pattern methodNameSplitPattern = Pattern.compile("\\s+");
	private static final Pattern methodNamePattern = Pattern.compile("[a-zA-Z_]\\w*");

//	private Model model;
	private String methodName;
	private Text inputText = null;
	private String[] altNames = null;

	public SelectAlternativeDialog(Shell parentShell, Model model, String methodName) {
		super(parentShell);
//		this.model = model;
		this.methodName = methodName;
	}

	@Override
	protected Point getInitialSize() {
		return initialSize;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(TITLE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginHeight = 16;
		gridLayout.marginWidth = 8;
		composite.setLayout(gridLayout);

		Label inputLabel = new Label(composite, SWT.NONE);
		inputLabel.setText("Input alternatives to method \"" + methodName + "\".");
		inputLabel.setLayoutData(new GridData(
				GridData.BEGINNING, GridData.FILL, true, false, 1, 1));

		inputText = new Text(composite, SWT.NONE);
		inputText.setLayoutData(new GridData(
				GridData.FILL, GridData.FILL, true, false, 1, 1));
		inputText.addModifyListener(new TextChecker());

		return composite;
	}

	@Override
	protected void okPressed() {
		if (inputText != null) {
			altNames = getMethodNames(inputText.getText());
		}
		super.okPressed();
	}

	private class TextChecker implements ModifyListener {
		@Override
		public void modifyText(ModifyEvent e) {
			Button okButton = getButton(IDialogConstants.OK_ID);
			if (okButton == null) {
				return;
			}
			Object source = e.getSource();
			if (source instanceof Text) {
				String[] altNames = getMethodNames(((Text) source).getText());
				okButton.setEnabled(altNames != null);
			}
		}
	}

	private String[] getMethodNames(String text) {
		String[] methodNames = methodNameSplitPattern.split(text);
		for (String methodName: methodNames) {
			if (!methodNamePattern.matcher(methodName).matches()) {
				return null;
			}
		}
		return methodNames;
	}

	public String[] getInput() {
		return altNames;
	}
}
