package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.ArchDSLFactory;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu.iarch.basefunction.controller.GraphitiModelManager;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.MethodEquality;
import jp.ac.kyushu.iarch.checkplugin.utils.MethodEqualityUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.ProjectSelectionUtils;
import jp.ac.kyushu.iarch.checkplugin.view.SelectDiagramsDialog;
import jp.ac.kyushu.iarch.sequencediagram.utils.MessageUtils;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import umlClass.AlternativeOperation;
import umlClass.DataType;
import umlClass.Operation;
import umlClass.OptionalOperation;
import behavior.AlternativeMessage;
import behavior.Lifeline;
import behavior.Message;
import behavior.MessageOccurrenceSpecification;
import behavior.OptionalMessage;

public class GenerateArchCode implements IHandler {

	@Override
	public void addHandlerListener(IHandlerListener handlerListener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject project = ProjectSelectionUtils.getProject(event, "Generate ArchiCode");
		
		SelectDiagramsDialog dialog = new SelectDiagramsDialog(
				HandlerUtil.getActiveShell(event), project);
		if (dialog.open() == MessageDialog.OK) {
//			// clear problems
//			ProblemViewManager.removeAllProblems(project);

//			// Generate archiface code
//			GenerateArchifaceCode(
//					dialog.getClassDiagram(), dialog.getSequenceDiagrams());

			GenerateArchfaceU(project, dialog.getClassDiagram(), dialog.getSequenceDiagrams());
		}

		return null;
	}

	private void GenerateArchfaceU(IProject project,
			IResource classDiagramResource,
			List<IResource> sequenceDiagramResources) {
		// Create empty Archface model.
		IFile file = project.getFile("/Gen-Arch2.arch");
		ArchModel archModel = new ArchModel(file, true);
		Model model = archModel.getModel();

		// Extract Class/Operations and put into the model.
		gatherComponents(classDiagramResource, model);

		// Extract Sequences and put them into the model.
		for (IResource sequenceDiagramResource : sequenceDiagramResources) {
			gatherConnector(sequenceDiagramResource, model);
		}

		// Generate Archface file.
		try {
			archModel.save();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		
	}

	private void gatherComponents(IResource classDiagramResource, Model model) {
		Resource classDiagram = GraphitiModelManager.getGraphitiModel(classDiagramResource);
		for (EObject eObj : classDiagram.getContents()) {
			if (eObj instanceof umlClass.Class) {
				umlClass.Class eClass = (umlClass.Class) eObj;
				if (!eClass.isArchpoint()) { continue; }
				String className = eClass.getName();

				// Create interfaces.
				Interface cInterface = ArchDSLFactory.eINSTANCE.createInterface();
				cInterface.setName(className);
				UncertainInterface uInterface = ArchDSLFactory.eINSTANCE.createUncertainInterface();
				uInterface.setSuperInterface(cInterface);
				uInterface.setName(ArchModelUtils.getAutoUncertainInterfaceName(className));

				// Convert Operations to Methods and put them.
				for (Operation operation : eClass.getOwnedOperation()) {
					if (!operation.isArchpoint()) { continue; }

					if (operation instanceof AlternativeOperation) {
						AltMethod altMethod = ArchDSLFactory.eINSTANCE.createAltMethod();
						for (Operation op : ((AlternativeOperation) operation).getOperations()) {
							altMethod.getMethods().add(convertToMethod(classDiagram, op));
						}
						uInterface.getAltmethods().add(altMethod);
					} else if (operation instanceof OptionalOperation) {
						OptMethod optMethod = ArchDSLFactory.eINSTANCE.createOptMethod();
						optMethod.setMethod(convertToMethod(classDiagram, operation));
						uInterface.getOptmethods().add(optMethod);
					} else {
						Method method = convertToMethod(classDiagram, operation);
						cInterface.getMethods().add(method);
					}
				}

				model.getInterfaces().add(cInterface);
				if (uInterface.getOptmethods().size() > 0 || uInterface.getAltmethods().size() > 0) {
					model.getU_interfaces().add(uInterface);
				}
			}
		}
	}

	private Method convertToMethod(Resource classDiagram, Operation operation) {
		Method method = ArchDSLFactory.eINSTANCE.createMethod();
		method.setName(operation.getName());
		DataType dataType = operation.getDatatype();
		// Due to ECore property, DataType is added to Resource directly.
		if (dataType == null) {
			dataType = findDataType(classDiagram, operation);
		}
		method.setType(dataType != null ? dataType.getName() : "void");
		return method;
	}

	private DataType findDataType(Resource classDiagram, Operation operation) {
		for (EObject eObj : classDiagram.getContents()) {
			if (eObj instanceof DataType) {
				DataType dataType = (DataType) eObj;
				for (Operation op : dataType.getOwnedOperation()) {
					if (op == operation) {
						return dataType;
					}
				}
			}
		}
		return null;
	}

	private void gatherConnector(IResource sequenceDiagramResource, Model model) {
		String sequenceName = sequenceDiagramResource.getName();
		// Drop extension.
		int extIndex = sequenceName.lastIndexOf('.');
		if (extIndex >= 0) {
			sequenceName = sequenceName.substring(0, extIndex);
		}
		// TODO: verification needed.
		// https://eclipse.org/Xtext/documentation/301_grammarlanguage.html
		// sequenceName = generateConnectorName(model);

		Resource sequenceDiagram = GraphitiModelManager.getGraphitiModel(sequenceDiagramResource);

		// Create Connectors.
		Connector connector = ArchDSLFactory.eINSTANCE.createConnector();
		connector.setName(sequenceName);

		List<Message> messages = MessageUtils.collectMessages(sequenceDiagram);
		// Check whether message is certain or uncertain.
		boolean certainBehavior = true;
		for (Message message : messages) {
			if (message instanceof OptionalMessage || message instanceof AlternativeMessage) {
				certainBehavior = false;
				break;
			}
		}

		if (certainBehavior) {
			// Convert Messages to Behavior and put it.
			Behavior behavior = ArchDSLFactory.eINSTANCE.createBehavior();

			for (Message message : messages) {
				// Find Method from model.
				String className = getClassName(message);
				if (className == null) {
					// TODO: Maybe diagram is not correct.
					continue;
				}
				Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
				if (cInterface == null) {
					// TODO: Generate Interface or abort?
					continue;
				}
				String methodName = message.getName();
				Method method = ArchModelUtils.findMethodByName(cInterface, methodName);
				if (method == null) {
					// TODO: Generate Method or abort?
					continue;
				}

				behavior.getCall().add(method);

				if (behavior.getInterface() == null) {
					behavior.setInterface(cInterface);
				}
				behavior.setEnd(cInterface);
			}

			connector.getBehaviors().add(behavior);
		} else {
			// Convert Messages to UncertainBehavior and put it into UncertainConnector.
			UncertainConnector uConnector = ArchDSLFactory.eINSTANCE.createUncertainConnector();
			uConnector.setSuperInterface(connector);
			uConnector.setName(ArchModelUtils.getAutoUncertainConnectorName(sequenceName));
			UncertainBehavior uBehavior = ArchDSLFactory.eINSTANCE.createUncertainBehavior();
			uBehavior.setName(ArchModelUtils.generateUncertainBehaviorName(uConnector));

			for (Message message : messages) {
				String className = getClassName(message);
				if (className == null) {
					// TODO: Maybe diagram is not correct.
					continue;
				}

				if (message instanceof AlternativeMessage) {
					List<UncertainInterface> uInterfaces =
							ArchModelUtils.searchUncertainInterfaceBySuperName(model, className);
					if (uInterfaces.isEmpty()) {
						// TODO: Generate Interface or abort?
						continue;
					}

					List<MethodEquality> equalities = new ArrayList<MethodEquality>();
					for (Message m : ((AlternativeMessage) message).getMessages()) {
						equalities.add(MethodEqualityUtils.createMethodEquality(m.getName()));
					}
					MethodEquality altMethodEquality = MethodEqualityUtils.createMethodEqualityForAlt(equalities);

					AltCall altCall = null;
					for (UncertainInterface uInterface : uInterfaces) {
						for (AltMethod altMethod : uInterface.getAltmethods()) {
							if (altMethodEquality.match(altMethod)) {
								altCall = ArchDSLFactory.eINSTANCE.createAltCall();
								boolean first = true;
								for (Method m : altMethod.getMethods()) {
									if (first) {
										altCall.setName(m);
										first = false;
									} else {
										altCall.getA_name().add(m);
									}
								}
								uBehavior.setEnd(uInterface.getSuperInterface());
							}
						}
					}
					if (altCall == null) {
						// TODO: Generate AltMethod or abort?
						continue;
					}
					uBehavior.getCall().add(altCall);
				} else if (message instanceof OptionalMessage) {
					List<UncertainInterface> uInterfaces =
							ArchModelUtils.searchUncertainInterfaceBySuperName(model, className);
					if (uInterfaces.isEmpty()) {
						// TODO: Generate Interface or abort?
						continue;
					}

					String methodName = message.getName();
					OptCall optCall = null;
					for (UncertainInterface uInterface : uInterfaces) {
						for (OptMethod optMethod : uInterface.getOptmethods()) {
							if (optMethod.getMethod().getName().equals(methodName)) {
								optCall = ArchDSLFactory.eINSTANCE.createOptCall();
								optCall.setName(optMethod.getMethod());
								uBehavior.setEnd(uInterface.getSuperInterface());
							}
						}
					}
					if (optCall == null) {
						// TODO: Generate OptMethod or abort?
						continue;
					}
					uBehavior.getCall().add(optCall);
				} else {
					Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
					if (cInterface == null) {
						// TODO: Generate Interface or abort?
						continue;
					}

					String methodName = message.getName();
					Method method = ArchModelUtils.findMethodByName(cInterface, methodName);
					if (method == null) {
						// TODO: Generate Method or abort?
						continue;
					}

					CertainCall certainCall = ArchDSLFactory.eINSTANCE.createCertainCall();
					certainCall.setName(method);
					uBehavior.getCall().add(certainCall);
					uBehavior.setEnd(cInterface);
				}
			}

			uConnector.getU_behaviors().add(uBehavior);
			model.getU_connectors().add(uConnector);
		}

		model.getConnectors().add(connector);
	}

	/**
	 * Generate an unique connector name.
	 * @param model
	 * @return
	 */
	private String generateConnectorName(Model model) {
		for (int i = 0; ; ++i) {
			String name = "Connector" + String.valueOf(i);
			boolean isUnique = true;
			for (Connector connector : model.getConnectors()) {
				if (name.equals(connector.getName())) {
					isUnique = false;
					break;
				}
			}
			if (isUnique) {
				return name;
			}
		}
	}

	private String getClassName(Message message) {
		while (message instanceof AlternativeMessage) {
			message = ((AlternativeMessage) message).getMessages().get(0);
		}
		behavior.Object bObj = MessageUtils.getReceivingObject(message);
		return bObj == null ? null : bObj.getName();
	}

	/**
	 * Gen the Archiface Code
	 *
	 */
	private void GenerateArchifaceCode(
			IResource classDiagramResource,
			List<IResource> sequenceDiagramResources) {

		String Code="";
		Resource classDiagram = GraphitiModelManager
				.getGraphitiModel(classDiagramResource);
		
		Code+= findClass(classDiagram.getContents());//Gen Class Code
		Code+= "\n";
		Code+= getSequenceCode(sequenceDiagramResources);//Gen Squence Code
		String projectPath = classDiagramResource.getProject().getLocation().toOSString();
		String ArchiCodeFile=projectPath +"/Gen-Arch.arch";
		File myFilePath = new File(ArchiCodeFile);
		try {
			if (!myFilePath.exists()) {
				myFilePath.createNewFile();   
				}
			FileWriter resultFile = new FileWriter(myFilePath);
			PrintWriter myFile = new PrintWriter(resultFile);
			myFile.println(Code);
			resultFile.close();
			}
		catch (Exception e) {
			System.out.println("Create File Error!");
			e.printStackTrace();
			}
		
	}
	
	/**
	 * Gen the Sequence Code
	 *
	 * @return the String(Sequence Code)
	 */
	private String getSequenceCode(List<IResource> sequenceDiagramResources) {
		String SequenceCode="";//CODE
		for(IResource sequenceDiagramResource : sequenceDiagramResources){
			Resource sequenceDiagram = GraphitiModelManager.getGraphitiModel(sequenceDiagramResource);
			List<behavior.Object> diagramObject = new ArrayList<behavior.Object>();
			List<Message> messages = new ArrayList<Message>();
			for(EObject obj : sequenceDiagram.getContents()){
				if(obj instanceof behavior.Object&&((behavior.Object) obj).isArchpoint()){
					diagramObject.add((behavior.Object)obj);
				}
				if(obj instanceof Message){
					messages.add((Message)obj);
				}
			}
			for(behavior.Object ob: diagramObject){
				SequenceCode += ob.getName() + " = (";
				for(Message msg:messages){
					Lifeline oblifeline = ob.getInclude();
					Lifeline sendlifeline = ((MessageOccurrenceSpecification)msg.getSendEvent()).getCovered().get(0);
					Lifeline recivelifeline = ((MessageOccurrenceSpecification)msg.getReceiveEvent()).getCovered().get(0);					
					if (oblifeline == sendlifeline){
						SequenceCode += recivelifeline.getActor().getName()+ "." +msg.getName()+"->";
						
					}else if(oblifeline == recivelifeline){
						SequenceCode += ob.getName()+ "." +msg.getName()+"->";
					}
				}
				SequenceCode += ob.getName() + ");\n";
			}
			
			SequenceCode += "";
		}
		return SequenceCode;
	}

	
	/**
	 * Gen the class code
	 *
	 * @return the String(interface code)
	 */
	private String findClass(List<EObject> umlClasses) {
		String InterfaceCode="";
		for (EObject obj : umlClasses) {
			if (obj instanceof umlClass.Class) {				
				umlClass.Class umlClass = (umlClass.Class) obj;
				InterfaceCode+="\ninterface component "+umlClass.getName()+" {\n";
				InterfaceCode+=findMethod(umlClass);
				InterfaceCode+="\n}\n";
			}			
		}
		
		return InterfaceCode;
	}
	
	/**
	 * Gen method Code
	 *
	 * @return the String(method Code)
	 */
	private String findMethod(umlClass.Class umlClass){
		String Code="";
		for(Operation operation:umlClass.getOwnedOperation()){
			Code+="\tvoid "+operation.getName()+"();\n";//+"\t\t"+"execution(void "+operation.getName()+"()) ;\n"
		}
		return Code;
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
	public void removeHandlerListener(IHandlerListener handlerListener) {
	}
}
