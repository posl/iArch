package jp.ac.kyushu_u.iarch.checkplugin.view;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu_u.iarch.basefunction.controller.GraphitiModelManager;
import jp.ac.kyushu_u.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu_u.iarch.basefunction.utils.MessageDialogUtils;
import jp.ac.kyushu_u.iarch.checkplugin.handler.ASTSourceCodeChecker;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class SelectAllFileDialog extends Dialog {
	private Table archiTable;
	private Table classTable;
	private Table sequenceTable;
	private Table sourceTable;
	private Table xmlTable;
	private IProject project;
	private Button okButton;
	private IResource archiface;
	private IResource classDiagram;
	private IResource xml;
	private List<IResource> sequenceDiagrams;
	private List<IResource> sourceCode;

	/**
	 * Constructor
	 * @param parentShell
	 * @param project
	 */
	public SelectAllFileDialog(Shell parentShell, IProject project) {
		super(parentShell);
		this.project = project;
		sequenceDiagrams = new  ArrayList<>();
		sourceCode = new  ArrayList<>();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(480, 740);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select Files");
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Create dialog widgets
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		CheckCheckedStateListener checkboxListener = new CheckCheckedStateListener();
		Composite composite = (Composite)super.createDialogArea(parent);
		GridData gridData = new GridData(GridData.FILL_BOTH);

		Label archiLabel = new Label(composite, SWT.NONE);
		archiLabel.setText("Archiface file (Select exactly 1 file)");
		archiTable = new Table(composite, SWT.CHECK|SWT.BORDER | SWT.V_SCROLL);
		archiTable.setLayoutData(gridData);
		archiTable.addSelectionListener(checkboxListener);

		Label classLabel = new Label(composite, SWT.NONE);
		classLabel.setText("Class Diagram");
		classTable = new Table(composite, SWT.CHECK|SWT.BORDER|SWT.V_SCROLL);
		classTable.setLayoutData(gridData);
		classTable.addSelectionListener(checkboxListener);

		Label sequenceLabel = new Label(composite, SWT.NONE);
		sequenceLabel.setText("Sequence Diagrams");
		sequenceTable = new Table(composite, SWT.CHECK|SWT.BORDER|SWT.V_SCROLL);
		sequenceTable.setLayoutData(gridData);
		sequenceTable.addSelectionListener(checkboxListener);

		Label sourceLabel = new Label(composite, SWT.NONE);
		sourceLabel.setText("Source code files (Select 1 or more files)");
		sourceTable = new Table(composite, SWT.CHECK|SWT.BORDER|SWT.V_SCROLL);
		sourceTable.setLayoutData(gridData);
		sourceTable.addSelectionListener(checkboxListener);

		Label xmlLabel = new Label(composite,SWT.NONE);
		xmlLabel.setText("Code xml files (Select exactly 1 file)");
		xmlTable = new Table(composite, SWT.CHECK|SWT.BORDER | SWT.V_SCROLL);
		xmlTable.setLayoutData(gridData);
		xmlTable.addSelectionListener(checkboxListener);
		setInitialItems();

		return composite;
	}

	/**
	 * Create OK and Cancel button
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, "OK", true);
		okButton.setEnabled(true);
		createButton(parent, IDialogConstants.CANCEL_ID,"Cancel", true);
	}

	/**
	 * move selected files from widget
	 */
	@Override
	protected void okPressed() {
		if (!isCorrectlySelected()) {
			MessageDialogUtils.showWarning("Select Files", "Select files as many times as indicated");
			return;
		}
		
		for(TableItem item : archiTable.getItems()){
			if(item.getChecked()){
				archiface = (IResource)item.getData();
			}
		}
		for(TableItem item : classTable.getItems()){
			if(item.getChecked()){
				classDiagram = (IResource)item.getData();
			}
		}
		for(TableItem item : sequenceTable.getItems()){
			if(item.getChecked()){
				sequenceDiagrams.add((IResource)item.getData());
			}
		}
		for(TableItem item : sourceTable.getItems()){
			if(item.getChecked()){
				sourceCode.add((IResource)item.getData());
			}
		}
		for(TableItem item : xmlTable.getItems()){
			if(item.getChecked()){
				xml = (IResource)item.getData();
			}
		}
		setReturnCode(OK);
		super.okPressed();
	}

	/**
	 * @author Ai Di
	 * Selection validation
	 */
	class CheckCheckedStateListener implements SelectionListener{
		@Override
		public void widgetSelected(SelectionEvent e) {
			if(isCorrectlySelected()){
				okButton.setEnabled(true);
			}else{
				okButton.setEnabled(false);
			}
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {	}
	}
	
	private boolean isCorrectlySelected() {
		int cntArchiFiles = 0;
		int cntClassDiagrams = 0;
		int cntSequenceDiagrams = 0;
		int cntSourceCode = 0;
		int cnxml = 0;
		for(TableItem item : archiTable.getItems()){
			if(item.getChecked()) cntArchiFiles++;
		}
		for(TableItem item : classTable.getItems()){
			if(item.getChecked()) cntClassDiagrams++;
		}
		for(TableItem item : sequenceTable.getItems()){
			if(item.getChecked()) cntSequenceDiagrams++;
		}
		for(TableItem item : sourceTable.getItems()){
			if(item.getChecked()) cntSourceCode++;
		}
		for(TableItem item : xmlTable.getItems()){
			if(item.getChecked()) cnxml++;
		}
		return cntArchiFiles == 1 &&
				cntClassDiagrams >= 0 &&
				cntSequenceDiagrams >= 0 &&
				cntSourceCode > 0 &&
				cnxml==1;
	}

	/**
	 * Set archiface file and diagram files to table.
	 */
	private void setInitialItems() {
		// automatically create codeXML
		IFile xmlFile = project.getFile(ASTSourceCodeChecker.CODEXML_FILEPATH);
		if (!xmlFile.exists()) {
			try {
				xmlFile.create(new ByteArrayInputStream("".getBytes()), false, new NullProgressMonitor());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					String ext = resource.getFileExtension();
					if(ext==null) return true;
					switch(ext){
					case "arch":
						TableItem aitem = new TableItem(archiTable, SWT.CHECK);
						aitem.setText(resource.getName());
						aitem.setData(resource);
						aitem.setChecked(true);

						break;
					case "diagram":
						Resource model = GraphitiModelManager.getGraphitiModel(resource);
						Diagram o = (Diagram)model.getContents().get(0);
						if("ClassDiagram".equals(o.getDiagramTypeId())){
							TableItem citem = new TableItem(classTable, SWT.CHECK);
							citem.setText(resource.getName());
							citem.setData(resource);
							citem.setChecked(true);
						}else if("SequenceDiagram".equals(o.getDiagramTypeId())){
							TableItem sitem = new TableItem(sequenceTable, SWT.CHECK);
							sitem.setText(resource.getName());
							sitem.setData(resource);
							sitem.setChecked(true);
						}else{
							System.err.println("undefine diagram types");
						}
						break;
					case "java":
						TableItem javaitem = new TableItem(sourceTable, SWT.CHECK);
						javaitem.setText(resource.getName());
						javaitem.setData(resource);
						javaitem.setChecked(true);

						break;
					case "xml":
						if(resource.getName().equals(XMLreader.CONFIG_FILEPATH)){
							break;
						}
						TableItem XMLitem = new TableItem(xmlTable,SWT.CHECK);
						XMLitem.setText(resource.getName());
						XMLitem.setData(resource);
						XMLitem.setChecked(true);
						break;
					default:
						break;
					}
					return true;
				}
			});
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/*Accessors*/
	public IResource getArchiface(){
		return archiface;
	}

	public IResource getClassDiagram(){
		return classDiagram;
	}

	public List<IResource> getSequenceDiagrams(){
		return sequenceDiagrams;
	}

	public List<IResource> getSourceCode(){
		return sourceCode;
	}

	public IResource getXml(){
		return xml;
	}
}
