package jp.ac.kyushu.iarch.checkplugin.view;

import java.io.IOException;
import java.util.Objects;

import jp.ac.kyushu.iarch.checkplugin.Activator;
import jp.ac.kyushu.iarch.checkplugin.testsupport.AbstractUncertaintyInfo;
import jp.ac.kyushu.iarch.checkplugin.testsupport.AlternativeInfo;
import jp.ac.kyushu.iarch.checkplugin.testsupport.ITreeContentInfo;
import jp.ac.kyushu.iarch.checkplugin.testsupport.InterfaceInfo;
import jp.ac.kyushu.iarch.checkplugin.testsupport.MethodInfo;
import jp.ac.kyushu.iarch.checkplugin.testsupport.OptionalInfo;
import jp.ac.kyushu.iarch.checkplugin.testsupport.SelectionInfo;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

class TestSupportEditDialog extends Dialog {

	private static final Image CHECKBOX_TRUE = getImageFromProject("/icons/checkbox1.png");
	private static final Image CHECKBOX_FALSE = getImageFromProject("/icons/checkbox0.png");
	private static final Image CHECKBOX_NEUTRAL = getImageFromProject("/icons/checkbox2.png");
	private static final Image RADIOBUTTON_TRUE = getImageFromProject("/icons/radiobutton1.png");
	private static final Image RADIOBUTTON_FALSE = getImageFromProject("/icons/radiobutton0.png");

	static private Image getImageFromProject(String imagePath) {
		try {
			org.osgi.framework.Bundle bundle = Activator.getDefault().getBundle();
			java.net.URL url = bundle.getEntry(imagePath);
			java.net.URL fileUrl = FileLocator.toFileURL(url);
			java.io.File imageFile = new java.io.File(fileUrl.getPath());
			java.io.InputStream inputStream = new java.io.FileInputStream(imageFile);
			return new Image(Display.getDefault(), inputStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private SelectionInfo selectionInfo;
	private Tree tree;
	private TreeViewer treeViewer;
	private Menu treeMenu;

	protected TestSupportEditDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	// ツリーの展開
	private class TreeContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// TODO Auto-generated method stub

		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public Object[] getChildren(Object element) {
			if (element instanceof ITreeContentInfo) {
				return ((ITreeContentInfo) element).getTreeContentChildren();
			} else {
				return new Object[] {};
			}
		}

		@Override
		public Object[] getElements(Object element) {
			return getChildren(element);
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof ITreeContentInfo) {
				return ((ITreeContentInfo) element).hasTreeContentChildren();
			} else {
				return false;
			}
		}

	}

	// ラベルの表示
	private class TreeLabelProvider implements ITableLabelProvider, IColorProvider {

		@Override
		public void addListener(ILabelProviderListener arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (element instanceof InterfaceInfo) {
					return JavaUI.getSharedImages().getImage(
							ISharedImages.IMG_OBJS_CLASS);
				} else if (element instanceof MethodInfo) {
					return JavaUI.getSharedImages().getImage(
							ISharedImages.IMG_OBJS_PUBLIC);
				}
				break;
			case 1:
				if (element instanceof MethodInfo) {
					MethodInfo methodInfo = (MethodInfo)element;
					AbstractUncertaintyInfo uncertaintyInfo = methodInfo.getParent();
					if (uncertaintyInfo instanceof OptionalInfo) {
						if (uncertaintyInfo.getSelected() == null) {
							return CHECKBOX_NEUTRAL;
						} else if (Objects.equals(uncertaintyInfo.getSelected(), methodInfo)) {
							return CHECKBOX_TRUE;
						} else {
							return CHECKBOX_FALSE;
						}
					} else if (uncertaintyInfo instanceof AlternativeInfo) {
						if (Objects.equals(uncertaintyInfo.getSelected(), methodInfo)) {
							return RADIOBUTTON_TRUE;
						} else {
							return RADIOBUTTON_FALSE;
						}
					}
				}
				break;
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (element instanceof ITreeContentInfo) {
					return ((ITreeContentInfo) element).getTreeContentLabel();
				}
				break;
			}
			return null;
		}

		@Override
		public Color getBackground(Object arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Color getForeground(Object element) {
			if (element instanceof AbstractUncertaintyInfo) {
				return new Color(Display.getCurrent(), 150, 150, 150);
			} else {
				return null;
			}
		}

	}

	private class TreeMenuAdapter extends MenuAdapter {

		@Override
		public void menuShown(MenuEvent e)
		{
			// 既存のメニュー項目の削除
			MenuItem[] items = treeMenu.getItems();
			for (MenuItem item : items) {
				item.dispose();
			}
			// メニュー項目の登録
			final Object element = tree.getSelection()[0].getData();
			if (element instanceof MethodInfo) {
				final MethodInfo methodInfo = (MethodInfo)element;
				final AbstractUncertaintyInfo uncertaintyInfo = methodInfo.getParent();
				if (uncertaintyInfo instanceof OptionalInfo) {
					boolean undefined = uncertaintyInfo.getSelected() == null;
					boolean selected = Objects.equals(uncertaintyInfo.getSelected(), methodInfo);
					final OptionalInfo optionalInfo = (OptionalInfo)uncertaintyInfo;
					if (undefined || !selected) {
						MenuItem menuItem = new MenuItem(treeMenu, SWT.NONE);
						menuItem.setText("Select");
						menuItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								optionalInfo.setSelected(true);
								refresh();
							}
						});
					}
					if (undefined || selected) {
						MenuItem menuItem = new MenuItem(treeMenu, SWT.NONE);
						menuItem.setText("Deselect");
						menuItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								optionalInfo.setSelected(false);
								refresh();
							}
						});
					}
				} else if (uncertaintyInfo instanceof AlternativeInfo) {
					boolean selected = Objects.equals(uncertaintyInfo.getSelected(), methodInfo);
					final AlternativeInfo alternativeInfo = (AlternativeInfo)uncertaintyInfo;
					if (!selected) {
						MenuItem menuItem = new MenuItem(treeMenu, SWT.NONE);
						menuItem.setText("Select");
						menuItem.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								alternativeInfo.setSelected(methodInfo);
								refresh();
							}
						});
					}
				}
			}
		}

	}

	public void setInput(SelectionInfo selectionInfo) {
		this.selectionInfo = selectionInfo;
	}

	public void refresh() {
		treeViewer.setInput(selectionInfo);
		treeViewer.expandAll();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite)super.createDialogArea(parent);

		Label label = new Label(composite, SWT.BORDER);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		label.setText("Right click to toggle a check box or a radio button.");

		tree = new Tree(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER_SOLID);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		treeViewer = new TreeViewer(tree);
		treeViewer.setContentProvider(new TreeContentProvider());
		treeViewer.setLabelProvider(new TreeLabelProvider());

		treeMenu = new Menu(tree);
		treeMenu.addMenuListener(new TreeMenuAdapter());
		tree.setMenu(treeMenu);

		TreeColumn nameColumn = new TreeColumn(tree, SWT.LEFT);
		nameColumn.setText("Name");
		nameColumn.setWidth(500);

		TreeColumn selectionColumn = new TreeColumn(tree, SWT.LEFT);
		selectionColumn.setText("Selection");
		selectionColumn.setWidth(100);

		refresh();
		return composite;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Edit Possibility");
	}

}

