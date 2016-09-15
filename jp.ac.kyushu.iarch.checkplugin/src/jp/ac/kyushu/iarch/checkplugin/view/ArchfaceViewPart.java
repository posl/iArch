package jp.ac.kyushu.iarch.checkplugin.view;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import jp.ac.kyushu.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.checkplugin.handler.ASTSourceCodeChecker;
import jp.ac.kyushu.iarch.checkplugin.handler.CheckerWorkSpaceJob;
import jp.ac.kyushu.iarch.checkplugin.model.AltMethodPairsContainer;
import jp.ac.kyushu.iarch.checkplugin.model.BehaviorPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.CallPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentMethodPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.UncertainBehaviorContainer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

public class ArchfaceViewPart extends ViewPart {
	public static final String ID = "jp.ac.kyushu.iarch.checkplugin.archfaceview";
	private TreeViewer componentTreeViewer;
	private TreeViewer behaviorTreeViewer;

	public class ComponentTreeContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// TODO Auto-generated method stub

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub

		}

		@Override
		public Object[] getElements(Object inputElement) {
			// TODO Auto-generated method stub
			if (inputElement instanceof List) {
				return ((List) inputElement).toArray();
			}
			return null;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			// TODO Auto-generated method stub
			if (parentElement instanceof ComponentClassPairModel) {
				return ((ComponentClassPairModel) parentElement).methodPairsList.toArray();
			} else if (parentElement instanceof AltMethodPairsContainer) {
				return ((AltMethodPairsContainer) parentElement).getAltMethodPairs().toArray();
			} else if (parentElement instanceof ComponentMethodPairModel) {
				return null;
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			// TODO Auto-generated method stub
			if (element instanceof ComponentMethodPairModel) {
				return ((ComponentMethodPairModel) element).getParentModel();
			} else if (element instanceof ComponentMethodPairModel) {
				if (element instanceof AltMethodPairsContainer) {
					return ((AltMethodPairsContainer) element).getAltMethodPairs();
				}
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			// TODO Auto-generated method stub
			if (element instanceof ComponentClassPairModel) {
				return true;
			} else if (element instanceof ComponentMethodPairModel) {
				if (element instanceof AltMethodPairsContainer) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

	}

	class ComponentTableLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub

		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub

		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			switch (columnIndex) {
			case 0:
				if (element instanceof ComponentClassPairModel) {
					return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
				} else if (element instanceof ComponentMethodPairModel) {
					return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
				} else {
					element.toString();
					return null;
				}
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			switch (columnIndex) {
			case 0:
				if (element instanceof ComponentClassPairModel) {
					return ((ComponentClassPairModel) element).getName();
				} else if (element instanceof ComponentMethodPairModel) {
					return ((ComponentMethodPairModel) element).getName();
				} else {
					element.toString();
				}
				break;
			case 1:
				if (element instanceof ComponentClassPairModel) {
					return "-";
				} else if (element instanceof ComponentMethodPairModel) {
					if (((ComponentMethodPairModel) element).isOpt()) {
						return "Optional";
					} else if (element instanceof AltMethodPairsContainer) {
						return "Alternatives";
					} else if (((ComponentMethodPairModel) element).isAlt()) {
						return "Alternative";
					} else {
						return "Certain";
					}
				}
				break;
			case 2:
				if (element instanceof ComponentClassPairModel) {
					if (((ComponentClassPairModel) element).hasJavaNode()) {
						return "✔";
					} else {
						return "✘";
					}
				} else if (element instanceof AltMethodPairsContainer) {
					if (((AltMethodPairsContainer) element).hasJavaNode()) {
						return "✔";
					} else {
						return "✘";
					}
				} else if (element instanceof ComponentMethodPairModel) {
					if (((ComponentMethodPairModel) element).hasJavaNode()) {
						return "✔";
					} else {
						return "✘";
					}
				}
				break;
			case 3:
				if (element instanceof ComponentMethodPairModel) {
					if (((ComponentMethodPairModel) element).getRecentDiff() != null) {
						Date commitTime =((ComponentMethodPairModel) element)
								.getRecentDiff().getCommitB().getAuthorIdent().getWhen();
						SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY/MM/dd hh:mm");
						return dateFormat.format(commitTime);
					}
				} else {
					return null;
				}
			case 4:
				if (element instanceof ComponentMethodPairModel) {
					if (((ComponentMethodPairModel) element).getRecentDiff() != null) {
						return ((ComponentMethodPairModel) element).getRecentDiff().getUncertainStrTypeA() +
								" -> " +((ComponentMethodPairModel) element).getRecentDiff().getUncertainStrTypeB();
					}
				}
			}
			return null;
		}
	}

	public class BehaviorTreeContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public Object[] getElements(Object inputElement) {
			// TODO 自動生成されたメソッド・スタブ
			if (inputElement instanceof List) {
				return ((List) inputElement).toArray();
			}
			return null;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof UncertainBehaviorContainer) {
				return ((UncertainBehaviorContainer) parentElement).getSeparatedBehaviors().toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			// TODO 自動生成されたメソッド・スタブ
			if (element instanceof BehaviorPairModel) {
				return ((BehaviorPairModel) element).getParentUncertainBehaviorContainer();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			// TODO 自動生成されたメソッド・スタブ
			if (element instanceof UncertainBehaviorContainer) {
				return true;
			}
			return false;
		}

	}

	class BehaviorTableLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void dispose() {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			// TODO 自動生成されたメソッド・スタブ
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			// TODO 自動生成されたメソッド・スタブ
			switch (columnIndex) {
			case 0:
				if (element instanceof UncertainBehaviorContainer) {
					String str = ((UncertainBehaviorContainer) element).getName() + ": ";
					for (Iterator<CallPairModel> callIterator = ((UncertainBehaviorContainer) element).getOriginalBehavior()
							.getCallModels().iterator(); callIterator.hasNext();) {
						CallPairModel call = callIterator.next();
						if (call.isAlt()) {
							str += "{ " + call.getName();
							for (ComponentMethodPairModel altCall : call.getAltMethodPairSets()) {
								str += "," + altCall.getName();
							}
							str += " }";

						} else if (call.isOpt()) {
							str += "[ " + call.getName() + " ]";
						} else {
							str += call.getName();
						}
						if (callIterator.hasNext()) {
							str += " -> ";
						}
					}
					return str;
				} else if (element instanceof BehaviorPairModel) {
					String str = "";
					for (Iterator<CallPairModel> call = ((BehaviorPairModel) element).getCallModels().iterator(); call.hasNext();) {
						str += call.next().getName();
						if (call.hasNext()) {
							str += " -> ";
						}
					}
					return str;
				}
				break;
			case 1:
				if (element instanceof UncertainBehaviorContainer) {
					if (((UncertainBehaviorContainer) element).getCompileSuccessedBehaviors().size() == 1) {
						return "✔";
					} else {
						return "✘";
					}
				} else if (element instanceof BehaviorPairModel) {
					UncertainBehaviorContainer parentContainer = ((BehaviorPairModel) element)
							.getParentUncertainBehaviorContainer();
					if (parentContainer != null) {
						if (parentContainer.getCompileSuccessedBehaviors().contains((BehaviorPairModel) element)) {
							return "✔";
						} else if (parentContainer.getCompileFailedBehaviors().contains((BehaviorPairModel) element)) {
							return "✘";
						} else {
							return "-";
						}
					}
				}
			}
			return null;
		}

	}

	public void setModels(List<ComponentClassPairModel> classPairs, List<UncertainBehaviorContainer> behaviorContainers) {
		componentTreeViewer.setInput(classPairs);
		behaviorTreeViewer.setInput(behaviorContainers);
		componentTreeViewer.expandAll();
	}

	public ArchfaceViewPart() {
		// TODO Auto-generated constructor stub
	}

	class ComponentPanel extends Composite {

		public ComponentPanel(Composite parent, int style) {
			super(parent, style);
			// TODO Auto-generated constructor stub
		}

	}

	@Override
	public void createPartControl(final Composite parent) {
		// TODO Auto-generated method stub
		GridLayout parentGridLayout = new GridLayout(1, true);
		parent.setLayout(parentGridLayout);
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));
		final Tree componentTree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER_SOLID | SWT.FULL_SELECTION);
		componentTree.setHeaderVisible(true);
		componentTree.setLinesVisible(true);
		componentTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		componentTreeViewer = new TreeViewer(componentTree);
		componentTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.isEmpty()) {
					return;
				}
				Object element = selection.getFirstElement();
				if (element instanceof ComponentMethodPairModel && ASTSourceCodeChecker.isEnableGit) {
					GitDetailDialog dialog = new GitDetailDialog(parent.getShell(), (ComponentMethodPairModel) element);
				}
			}
		});
		TreeColumn nameColumn = new TreeColumn(componentTree, SWT.LEFT);
		nameColumn.setText("Component Name");
		nameColumn.setWidth(150);
		TreeColumn uncertainColumn = new TreeColumn(componentTree, SWT.LEFT);
		uncertainColumn.setText("Uncertain Type");
		uncertainColumn.setWidth(100);
		TreeColumn isExistColumn = new TreeColumn(componentTree, SWT.LEFT);
		isExistColumn.setText("Impl");
		isExistColumn.setWidth(50);
		TreeColumn commitMsg = new TreeColumn(componentTree, SWT.LEFT);
		commitMsg.setText("Uncertain Event Date");
		commitMsg.setWidth(150);
		TreeColumn recentType = new TreeColumn(componentTree, SWT.LEFT);
		recentType.setText("Recent Uncertain Type");
		recentType.setWidth(200);
		componentTree.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO 自動生成されたメソッド・スタブ

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 自動生成されたメソッド・スタブ

			}
		});
		componentTreeViewer.setContentProvider(new ComponentTreeContentProvider());
		componentTreeViewer.setLabelProvider(new ComponentTableLabelProvider());

		Tree behaviorTree = new Tree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER_SOLID | SWT.FULL_SELECTION);
		behaviorTree.setHeaderVisible(true);
		behaviorTree.setLinesVisible(true);
		behaviorTree.setLayout(parentGridLayout);
		behaviorTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		TreeColumn behaviorNameColumn = new TreeColumn(behaviorTree, SWT.LEFT);
		behaviorNameColumn.setText("Behavior Name");
		behaviorNameColumn.setWidth(300);
		TreeColumn behaviorImplColumn = new TreeColumn(behaviorTree, SWT.LEFT);
		behaviorImplColumn.setText("Impl");
		behaviorImplColumn.setWidth(50);
		behaviorTreeViewer = new TreeViewer(behaviorTree);
		behaviorTreeViewer.setContentProvider(new BehaviorTreeContentProvider());
		behaviorTreeViewer.setLabelProvider(new BehaviorTableLabelProvider());
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void init(IViewSite site) throws PartInitException {
		// ビューの初期化の時にビューの内容を更新する。
		// これにより、Eclipse起動時にArchface-U Viewの内容が表示されるようになる。
		try {
			CheckerWorkSpaceJob.getInstance(ProjectReader.getProject()).checkProject(new NullProgressMonitor());
		} catch (ProjectNotFoundException e) {
			// do nothing
		}
		super.init(site);
	}

}
