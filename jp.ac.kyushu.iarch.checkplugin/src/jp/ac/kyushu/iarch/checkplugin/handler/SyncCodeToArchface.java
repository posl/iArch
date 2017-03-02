package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.IOException;

import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.basefunction.utils.MessageDialogUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.ArchModelUtils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

public class SyncCodeToArchface implements IHandler {

	private static final String HANDLER_TITLE = "Sync: Code -> iArch";

	private static final String SYNC_ARCHFILE_PATH = "/Gen-Arch2.arch";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Get the project.
		IProject project = null;
		try {
			project = ProjectReader.getProject();
		} catch (ProjectNotFoundException e) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Project not found.");
			return null;
		}

		// Read the XML file which stores code info within the project.
		IFile codeXmlFile = project.getFile(ASTSourceCodeChecker.CODEXML_FILEPATH);
		if (!codeXmlFile.exists()) {
			MessageDialogUtils.showError(HANDLER_TITLE, ASTSourceCodeChecker.CODEXML_FILEPATH + " not found.");
			return null;
		}
		Document codeXml = null;
		try {
			SAXReader saxReader = new SAXReader();
			codeXml = saxReader.read(codeXmlFile.getContents());
		} catch (CoreException e) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Failed to read " + ASTSourceCodeChecker.CODEXML_FILEPATH);
			return null;
		} catch (DocumentException e) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Failed to read " + ASTSourceCodeChecker.CODEXML_FILEPATH);
			return null;
		}

		// Create empty Archface model.
		IFile archfile = project.getFile(SYNC_ARCHFILE_PATH);
		ArchModel archModel = new ArchModel(archfile, true);
		// TODO: open existing file to synchronize.

		Model model = archModel.getModel();
		boolean modified = generateModelFromCode(codeXml, model);
		// Save file
		if (modified) {
			try {
				archModel.save();
			} catch (IOException e) {
				MessageDialogUtils.showError(HANDLER_TITLE, "Failed to save " + SYNC_ARCHFILE_PATH);
			} catch (RuntimeException e) {
				// Model error falls here.
				String m = e.getMessage();
				StringBuilder sb = new StringBuilder("Model validation failed.\n");
				sb.append(m != null ? m : "Unknown reason.");
				MessageDialogUtils.showError(HANDLER_TITLE, sb.toString());
			}
		}

		return null;
	}

	private boolean generateModelFromCode(Document codeXml, Model model) {
		boolean modified = false;

		// TODO: Can search other than default package.
		Element root = codeXml.getRootElement();
		Element defaultPackage = (Element) root.selectSingleNode("//Package[@name='']");

		// Collect classes/methods from code.
		for (Object classObj : defaultPackage.elements()) {
			if (classObj instanceof Element) {
				Element classElement = (Element) classObj;
				if ("Class".equals(classElement.getName())){
					String className = classElement.attributeValue("name");

					// Find or generate Interface.
					Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
					if (cInterface == null) {
						cInterface = ArchModelUtils.createInterfaceElement(className);
						model.getInterfaces().add(cInterface);
						modified = true;
					}

					for (Object methodObj : classElement.elements()) {
						if (methodObj instanceof Element) {
							Element methodElement = (Element) methodObj;
							if ("MethodDeclaration".equals(methodElement.getName())) {
								String methodName = methodElement.attributeValue("name");

								// Find or generate method.
								Method method = ArchModelUtils.findMethodByName(cInterface, methodName);
								if (method == null) {
									method = ArchModelUtils.createMethodElement(methodName);
									// TODO: Support arguments and return type.
									method.setType("void");
									cInterface.getMethods().add(method);
									modified = true;
								}
							}
						}
					}
				}
			}
		}

		return modified;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

}
