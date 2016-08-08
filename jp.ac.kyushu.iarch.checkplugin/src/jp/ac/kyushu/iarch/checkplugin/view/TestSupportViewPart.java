package jp.ac.kyushu.iarch.checkplugin.view;

import java.util.ArrayList;
import jp.ac.kyushu.iarch.checkplugin.Activator;
import jp.ac.kyushu.iarch.checkplugin.testsupport.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.part.ViewPart;

/**
 * テストサポートのGUI
 * @author watanabeke
 */
public class TestSupportViewPart extends ViewPart {

	public static final String ID = "jp.ac.kyushu.iarch.checkplugin.testsupportview";

	private ResourceFetcher fetcher;
	private IFile archFile;
	private java.util.List<IFile> configFiles = new ArrayList<>();
	private IFile aspectFile;

	private Label label;
	private List list;

	private class CancellationException extends Exception {

		private static final long serialVersionUID = 902670948221571639L;

	}

	private abstract class ButtonListener extends SelectionAdapter {

		@Override
		public void widgetSelected(SelectionEvent arg0) {
			try {
				handleWidgetSelected(arg0);
			} catch (CancellationException e) {
				// 何もしない
			} catch (Exception e) {
				ErrorDialog.openError(null, null, null, new Status(
						IStatus.ERROR, Activator.PLUGIN_ID,
						"An error occurred during processing.", e));
			}
		}

		protected IFile getSelectedConfigFile() throws CancellationException {
			int selectionIndex = list.getSelectionIndex();
			if (selectionIndex < 0) {
				// 未選択
				throw new CancellationException();
			}
			return configFiles.get(selectionIndex);
		}

		protected String input(String dialogTitle, String dialogMessage, String initialValue)
				throws CancellationException {
			InputDialog inputDialog = new InputDialog(
					null, dialogTitle, dialogMessage, initialValue, null);
			inputDialog.open();
			String input = inputDialog.getValue();
			if (input == null) {
				// キャンセル時
				throw new CancellationException();
			} else {
				return input;
			}
		}

		protected void confirm(String title, String message) throws CancellationException {
			boolean confirm = MessageDialog.openConfirm(null, title, message);
			if (!confirm) {
				// キャンセル時
				throw new CancellationException();
			}
		}

		protected void infomation(String title, String message) {
			MessageDialog.openInformation(null, title, message);
		}

		abstract void handleWidgetSelected(SelectionEvent arg0) throws Exception;

	}

	// ボタンを押したときの処理
	private class NewButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			String name = input("New Possibility", "Possibility name:", "NewPossibility");
			IFile configFile = fetcher.fetchConfigFile(name);
			ResourceUtility.generateConfig(archFile, configFile);
			refresh();
		}

	}

	private class RenameButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			IFile origConfigFile = getSelectedConfigFile();
			String name = input("Rename Possibility", "Possibility name:",
					Utility.withoutSuffix(origConfigFile.getName()));
			IPath destConfigPath = fetcher.fetchConfigFile(name).getFullPath();
			origConfigFile.move(destConfigPath, true, null);
			refresh();
		}

	}

	private class CopyButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			IFile origConfigFile = getSelectedConfigFile();
			String name = input("Copy Possibility", "Possibility name:",
					String.format("CopyOf%s", Utility.withoutSuffix(origConfigFile.getName())));
			IFile destConfigFile = fetcher.fetchConfigFile(name);
			ResourceUtility.copyConfig(origConfigFile, destConfigFile);
			refresh();
		}

	}

	private class DeleteButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			IFile configFile = getSelectedConfigFile();
			confirm("Delete Possibility",
					String.format("Are you sure you want to delete \"%s\"?",
							Utility.withoutSuffix(configFile.getName())));
			configFile.delete(true, null);
			refresh();
		}

	}

	private class EditButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			IFile configFile = getSelectedConfigFile();
			SelectionInfo info = ResourceUtility.readConfig(configFile);
			TestSupportEditDialog dialog = new TestSupportEditDialog(null);
			dialog.setInput(info);
			int confirm = dialog.open();
			if (confirm == TestSupportEditDialog.OK) {
				ResourceUtility.writeConfig(info, configFile);
			}
		}

	}

	private class AdjustButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			IFile configFile = getSelectedConfigFile();
			confirm("Adjust Possibility",
					String.format("Are you sure you want to adjust \"%s\" to current archface?",
							Utility.withoutSuffix(configFile.getName())));
			ResourceUtility.adjustConfig(archFile, configFile);
			infomation("Adjust Possibility",
					String.format("\"%s\" is adjusted to current archface successfully.",
							Utility.withoutSuffix(configFile.getName())));
		}

	}

	private class AdjustAllButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			confirm("Adjust Possibility",
					"Are you sure you want to adjust all possibilities to current archface?");
			for (IFile configFile : configFiles) {
				ResourceUtility.adjustConfig(archFile, configFile);
			}
			infomation("Adjust Possibility",
					"All config files are adjusted to current archface successfully.");
		}

	}

	private class GenerateButtonListener extends ButtonListener {

		@Override
		void handleWidgetSelected(SelectionEvent arg0) throws Exception {
			IFile configFile = getSelectedConfigFile();
			ResourceUtility.generateAspect(configFile, aspectFile);
			infomation("Generate Aspect",
					String.format("An aspect is generated from \"%s\" successfully.",
							Utility.withoutSuffix(configFile.getName())));
		}

	}

	@Override
	public void createPartControl(Composite parent) {
		// 準備
		parent.setLayout(new GridLayout(1, false));

		// ラベル
		label = new Label(parent, SWT.BORDER);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// リスト
		list = new List(parent, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL);
		list.setLayoutData(new GridData(GridData.FILL_BOTH));

		// メニュー
		Menu menu = new Menu(list);
		list.setMenu(menu);

		MenuItem newMenu = new MenuItem(menu, SWT.PUSH);
		newMenu.setText("New");
		newMenu.addSelectionListener(new NewButtonListener());

		MenuItem renameMenu = new MenuItem(menu, SWT.PUSH);
		renameMenu.setText("Rename");
		renameMenu.addSelectionListener(new RenameButtonListener());

		MenuItem copyMenu = new MenuItem(menu, SWT.PUSH);
		copyMenu.setText("Copy");
		copyMenu.addSelectionListener(new CopyButtonListener());

		MenuItem deleteMenu = new MenuItem(menu, SWT.PUSH);
		deleteMenu.setText("Delete");
		deleteMenu.addSelectionListener(new DeleteButtonListener());

		new MenuItem(menu, SWT.SEPARATOR);

		MenuItem editMenu = new MenuItem(menu, SWT.PUSH);
		editMenu.setText("Edit");
		editMenu.addSelectionListener(new EditButtonListener());

		MenuItem adjustMenu = new MenuItem(menu, SWT.PUSH);
		adjustMenu.setText("Adjust");
		adjustMenu.addSelectionListener(new AdjustButtonListener());

		MenuItem adjustAllMenu = new MenuItem(menu, SWT.PUSH);
		adjustAllMenu.setText("Adjust All");
		adjustAllMenu.addSelectionListener(new AdjustAllButtonListener());

		MenuItem generateMenu = new MenuItem(menu, SWT.PUSH);
		generateMenu.setText("Generate Aspect");
		generateMenu.addSelectionListener(new GenerateButtonListener());

		refresh();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public void refresh() {
		// 初期化
		fetcher = null;
		archFile = null;
		configFiles.clear();
		aspectFile = null;
		list.removeAll();
		// 更新
		IProject project = ResourceFetcher.fetchProject();
		if (project == null) {
			label.setText("Project:");
		} else {
			fetcher = new ResourceFetcher(project);
			label.setText(String.format("Project: %s", fetcher.getProjectName()));
			archFile = fetcher.fetchArchFile();
			aspectFile = fetcher.fetchAspectFile();
			for (IFile configFile : fetcher.fetchConfigFiles()) {
				configFiles.add(configFile);
				list.add(Utility.withoutSuffix(configFile.getName()));
			}
		}
	}

}

