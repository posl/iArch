/**
 *
 */
package jp.ac.kyushu.iarch.checkplugin.view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jp.ac.kyushu.iarch.checkplugin.handler.ASTSourceCodeChecker;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentMethodPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.GitDiff;
import jp.ac.kyushu.iarch.checkplugin.utils.GitUtils;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * @author fukamachi
 *
 */
public class GitDetailDialog extends Dialog {
	Table table;
	Text commitMsgText;
	Composite diffComposite;
	Text commitAText;
	Text commitBText;
	ComponentMethodPairModel methodPairModel;
	Button okButton;
	ArrayList<GitDiff> diffList;

	/**
	 * @param parentShell
	 */
	public GitDetailDialog(Shell parentShell, ComponentMethodPairModel model) {
		super(parentShell);
		this.methodPairModel = model;
		if (ASTSourceCodeChecker.project != null) {
			ArrayList<GitDiff> allDiffs = GitUtils.getAllDiffList(ASTSourceCodeChecker.project.getProject());
			this.diffList = GitUtils.getSingleMethodDiffList(allDiffs, this.methodPairModel);
		} else {
			System.out.println("Dialog Init Error:Project path is not found.");
		}
		this.open();
	}

	/**
	 * @param parentShell
	 */
	public GitDetailDialog(IShellProvider parentShell) {
		super(parentShell);
	}

	/**
	 * Create OK and Cancel button
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		this.okButton = createButton(parent, IDialogConstants.OK_ID, "OK", true);
		this.okButton.setEnabled(true);
	}

	/**
	 * Initialize Window size
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	/**
	 * Placement SWT or Swing Components here !
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite composite = (Composite) super.createDialogArea(parent);
		GridData compositeGridData = new GridData(GridData.FILL_BOTH);
		composite.setLayoutData(compositeGridData);
		GridLayout compositeLayout = new GridLayout(1, true);
		compositeLayout.marginHeight = 10;
		composite.setLayout(compositeLayout);
		Label mes = new Label(composite, SWT.LEFT);
		GridData mesGrid = new GridData();
		mes.setText("These are uncertainties about " + this.methodPairModel.getParentModel().getName() + "."
				+ this.methodPairModel.getName() + ".");
		mesGrid.grabExcessHorizontalSpace = true;
		mes.setLayoutData(mesGrid);
		if (this.diffList != null) {
			setInitialTable(composite, diffList);
			Label commitMsgLabel = new Label(composite, SWT.LEFT);
			commitMsgLabel.setText("Commit Message");
			commitMsgText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
			GridData commitGrid = new GridData();
			commitGrid.horizontalAlignment = GridData.FILL;
			commitGrid.verticalAlignment = GridData.FILL;
			commitGrid.grabExcessHorizontalSpace = true;
			commitGrid.grabExcessVerticalSpace = true;
			commitMsgText.setLayoutData(commitGrid);
			diffComposite = new Composite(composite, SWT.NONE);
			GridData diffCompositeGridData = new GridData(GridData.FILL_BOTH);
			diffComposite.setLayoutData(diffCompositeGridData);
			GridLayout diffCompositeLayout = new GridLayout(2, true);
			diffComposite.setLayout(diffCompositeLayout);
			Label commitALabel = new Label(diffComposite, SWT.LEFT);
			commitALabel.setText("Previous Commit");
			Label commitBLabel = new Label(diffComposite, SWT.LEFT);
			commitBLabel.setText("Current Commit");
			commitAText = new Text(diffComposite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
			commitAText.setLayoutData(commitGrid);
			commitBText = new Text(diffComposite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
			commitBText.setLayoutData(commitGrid);
		}
		return composite;
	}

	private void setInitialTableColumns() {
		TableColumn[] columns = new TableColumn[6];
		columns[0] = new TableColumn(table, SWT.CENTER);
		columns[0].setWidth(15);
		columns[1] = new TableColumn(table, SWT.CENTER);
		columns[1].setText("Uncertain Type");
		columns[1].setWidth(200);
		columns[2] = new TableColumn(table, SWT.CENTER);
		columns[2].setText("Date");
		columns[2].setWidth(130);
		columns[3] = new TableColumn(table, SWT.CENTER);
		columns[3].setText("Committer");
		columns[3].setWidth(100);
		columns[4] = new TableColumn(table, SWT.CENTER);
		columns[4].setText("Commit ID");
		columns[4].setWidth(100);
		columns[5] = new TableColumn(table, SWT.CENTER);
		columns[5].setText("Commit Message");
		columns[5].setWidth(200);
	}

	private void setInitialTable(final Composite composite, ArrayList<GitDiff> diffs) {
		table = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		GridData tableGrid = new GridData();
		tableGrid.verticalAlignment = SWT.FILL;
		tableGrid.grabExcessVerticalSpace = true;
		table.setLayoutData(tableGrid);
		// Columns Settings
		table.setHeaderVisible(true);
		setInitialTableColumns();

		// Lines Settings
		int index = 1;
		for (GitDiff diff : diffs) {
			TableItem item = new TableItem(table, SWT.LEFT);
			item.setData(diff);
			item.setText(0, Integer.toString(index));
			item.setText(1, diff.getUncertainStrTypeA() + " -> " + diff.getUncertainStrTypeB());
			Date date = diff.getCommitB().getCommitterIdent().getWhen();
			SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY/MM/dd hh:mm");
			item.setText(2, dateFormat.format(date));
			item.setText(3, diff.getCommitB().getCommitterIdent().getName());
			item.setText(4, diff.getCommitB().name());
			item.setText(5, diff.getCommitB().getShortMessage());
			index++;
		}
		table.addSelectionListener(new SelectionListener() {
			// Click Listener
			@Override
			public void widgetSelected(SelectionEvent e) {
				int select = table.getSelectionIndex();
				if (select < 0) {
					return;
				}
				Object data = table.getItem(select).getData();
				if (data == null) {
					return;
				}
				if(data instanceof GitDiff){
					commitMsgText.setText(((GitDiff) data).getCommitB().getFullMessage());
					commitAText.setText(((GitDiff) data).getDeletedCode());
					commitBText.setText(((GitDiff) data).getInsertedCode());
				}
			}

			// Double Click Listener
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
	}

	/**
	 * Settings of Shell (ex:Title)
	 */
	@Override
	protected void configureShell(Shell newShell) {
		// TODO 自動生成されたメソッド・スタブ
		super.configureShell(newShell);
		newShell.setText("Git Log Detail - " + this.methodPairModel.getParentModel().getName() + "."
				+ this.methodPairModel.getName());
	}
}
