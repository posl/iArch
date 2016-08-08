package jp.ac.kyushu.iarch.checkplugin.view;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
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

public class SelectSourceCodeFile extends Dialog {
	private Table classTable;
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
	public SelectSourceCodeFile(Shell parentShell, IProject project) {
		super(parentShell);
		this.project = project;
		sequenceDiagrams = new  ArrayList<>();
		sourceCode = new  ArrayList<>();
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(480, 400);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Select  Code  Files");
	}
	
	/**
	 * Create dialog widgets
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		CheckCheckedStateListener checkboxListener = new CheckCheckedStateListener();
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Label sourceLabel = new Label(composite, SWT.NONE);
		sourceLabel.setText("Source code files");
		sourceTable = new Table(composite, SWT.CHECK|SWT.BORDER|SWT.V_SCROLL);
		sourceTable.setLayoutData(new GridData(400, 100));
		sourceTable.addSelectionListener(checkboxListener);

		Label xmlLabel = new Label(composite,SWT.NONE);
		xmlLabel.setText("XML  files");
		xmlTable = new Table(composite, SWT.CHECK|SWT.BORDER | SWT.V_SCROLL);
		xmlTable.setLayoutData(new GridData(400, 50));
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
			int cntArchiFiles = 0;
			int cntClassDiagrams = 0;
			int cntSequenceDiagrams = 0;
			int cntSourceCode = 0;
			int cnxml = 0;
			for(TableItem item : sourceTable.getItems()){
				if(item.getChecked()) cntSourceCode++;
			}
			for(TableItem item : classTable.getItems()){
				if(item.getChecked()) cnxml++;
			}

			if(cntArchiFiles==1&&cntClassDiagrams==1&&cntSequenceDiagrams>0&&cntSourceCode>0&&cnxml==1){
				okButton.setEnabled(true);
			}else{
				okButton.setEnabled(false);
			}
		}
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {	}		
	}
	
	/**
	 * Set archiface file and diagram files to table.
	 */
	private void setInitialItems() {
		try {
			project.accept(new IResourceVisitor() {
				@Override
				public boolean visit(IResource resource) throws CoreException {
					String ext = resource.getFileExtension();
					if(ext==null) return true;
					switch(ext){
					case "java":
						TableItem javaitem = new TableItem(sourceTable, SWT.CHECK);
						javaitem.setText(resource.getName());
						javaitem.setData(resource);
						javaitem.setChecked(true);

						break;
					case "xml":
						if(resource.getName().equals("Config.xml")){
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
