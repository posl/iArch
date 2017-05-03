package jp.ac.kyushu_u.iarch.checkplugin.handler;
import java.io.IOException;

import jp.ac.kyushu_u.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu_u.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu_u.iarch.basefunction.utils.FileIOUtils;
import jp.ac.kyushu_u.iarch.checkplugin.view.SelectAllFileDialog;

import org.dom4j.DocumentHelper;
import org.dom4j.Document;
import org.dom4j.Element;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

public class ConfigurationXMLHandler implements IHandler{

	public void CreateConfigFile(IProject project, SelectAllFileDialog AllDialog){
		Document codeXmlDocument = DocumentHelper.createDocument();
		Element rootElement = codeXmlDocument.addElement("Project");
		rootElement.addAttribute("name", project.getName());

		final Element ArchCodeElement = rootElement.addElement("Archfile");
		{
			final Element PathElement = ArchCodeElement.addElement("Path");
			PathElement.addAttribute("Attribute", AllDialog.getArchiface().getFullPath().toString());
		}


		final Element ClassDiagramElement = rootElement.addElement("ClassDiagram");
		if(AllDialog.getClassDiagram() != null)
		{
			final Element PathElement = ClassDiagramElement.addElement("Path");
			PathElement.addAttribute("Attribute", AllDialog.getClassDiagram().getFullPath().toString());
		}

		final Element SequenceDiagramElement = rootElement.addElement("SequenceDiagram");
		if(AllDialog.getSequenceDiagrams() != null)
		{
			for(IResource resource:AllDialog.getSequenceDiagrams()){
				final Element PathElement = SequenceDiagramElement.addElement("Path");
				PathElement.addAttribute("Attribute", resource.getFullPath().toString());
			}
		}

		final Element SourceCodeElement = rootElement.addElement("SourceCode");
		{

			for(IResource resource:AllDialog.getSourceCode()){
				final Element PathElement = SourceCodeElement.addElement("Path");
				PathElement.addAttribute("Attribute", resource.getFullPath().toString());
			}
		}


		final Element XMLElement = rootElement.addElement("ARXML");
		{
			final Element PathElement = XMLElement.addElement("Path");
			try{
			PathElement.addAttribute("Attribute", AllDialog.getXml().getFullPath().toString());
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		}


		try {
            FileIOUtils.xmlWriteFile(XMLreader.getConfigFile(project), codeXmlDocument);
		} catch (IOException | CoreException e) {
			System.out.println(e.getMessage());
		}
	}



	public boolean isConfigFileExist(IProject project){
		return XMLreader.isConfigFileExist(project);
	}
	@Override
	public void addHandlerListener(IHandlerListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = ProjectReader.getProject();

		SelectAllFileDialog dialog = new SelectAllFileDialog(
				HandlerUtil.getActiveShell(event), project);
		if (dialog.open() == MessageDialog.OK) {
			if(project == null){
				return null;
			}
			if(!isConfigFileExist(project)){
				System.out.println("config file does not exist.");
			}
			CreateConfigFile(project,dialog);
			//ArchfaceChecker archfaceChecker = new ArchfaceChecker(project);
		}

		return null;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isHandled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void removeHandlerListener(IHandlerListener arg0) {
		// TODO Auto-generated method stub

	}

}
