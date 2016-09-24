package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
import jp.ac.kyushu.iarch.basefunction.utils.MessageDialogUtils;
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

			// Generate Archface-U code.
			IFile file = project.getFile("/Gen-Arch2.arch");
			GenerateArchfaceU(dialog.getClassDiagram(), dialog.getSequenceDiagrams(), file);
		}

		return null;
	}

	// Stop generating (Uncertain)Behavior on any inconsistency.
	private static final int RESOLVE_BY_ABORT = 0;
	// Skip inconsistent message.
	private static final int RESOLVE_BY_SKIP = 1;
	// Generate missing methods as much as possible.
	private static final int RESOLVE_BY_GENERATE = 2;

	private void GenerateArchfaceU(IResource classDiagramResource,
			List<IResource> sequenceDiagramResources, IFile file) {
		// Create empty Archface model.
		ArchModel archModel = new ArchModel(file, true);
		Model model = archModel.getModel();

		// Extract Class/Operations and put into the model.
		gatherComponents(classDiagramResource, model);

		// Extract Sequences and put them into the model.
		for (IResource sequenceDiagramResource : sequenceDiagramResources) {
			gatherConnector(sequenceDiagramResource, model, RESOLVE_BY_GENERATE);
		}

		// Generate Archface file.
		try {
			archModel.save();
		} catch (IOException e) {
			MessageDialogUtils.showError("Generate Archcode", "Failed to save Archcode.");
		} catch (RuntimeException e) {
			// Model error falls here.
			String m = e.getMessage();
			StringBuilder sb = new StringBuilder("Model validation failed.\n");
			sb.append(m != null ? m : "Unknown reason.");
			MessageDialogUtils.showError("Generate Archcode", sb.toString());
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
				Interface cInterface = ArchModelUtils.createInterfaceElement(className);
				String uIfName = ArchModelUtils.getAutoUncertainInterfaceName(className);
				UncertainInterface uInterface = ArchModelUtils.createUncertainInterfaceElement(uIfName, cInterface);

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
						OptMethod optMethod =
								ArchModelUtils.createOptMethodElement(convertToMethod(classDiagram, operation));
						uInterface.getOptmethods().add(optMethod);
					} else {
						Method method = convertToMethod(classDiagram, operation);
						cInterface.getMethods().add(method);
					}
				}

				// Add interfaces if needed.
				boolean uInterfaceNeeded = uInterface.getOptmethods().size() > 0
						|| uInterface.getAltmethods().size() > 0;
				if (uInterfaceNeeded || cInterface.getMethods().size() > 0) {
					model.getInterfaces().add(cInterface);
				}
				if (uInterfaceNeeded) {
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

	private void gatherConnector(IResource sequenceDiagramResource, Model model,
			int resolveType) {
		// Get connector name from file.
		String sequenceName = sequenceDiagramResource.getName();
		// Drop extension.
		int extIndex = sequenceName.lastIndexOf('.');
		if (extIndex >= 0) {
			sequenceName = sequenceName.substring(0, extIndex);
		}
		// If it does not match ID rule, use safe name.
		if (!isValidID(sequenceName)) {
			sequenceName = generateConnectorName(model, "Connector");
		}

		Resource sequenceDiagram = GraphitiModelManager.getGraphitiModel(sequenceDiagramResource);

		// Create Connectors.
		Connector connector = ArchDSLFactory.eINSTANCE.createConnector();
		connector.setName(sequenceName);
		model.getConnectors().add(connector);

		// Collect ordered Messages.
		List<Message> messages = MessageUtils.collectMessages(sequenceDiagram);

		// Check whether message is certain or uncertain.
		boolean certainBehavior = true;
		for (Message message : messages) {
			if (!message.isArchpoint()) { continue; }
			if (message instanceof OptionalMessage || message instanceof AlternativeMessage) {
				certainBehavior = false;
				break;
			}
		}

		if (certainBehavior) {
			// Convert Messages to Behavior and put it.
			Behavior behavior = convertToBehavior(model, messages, resolveType);
			if (behavior != null) {
				connector.getBehaviors().add(behavior);
			}
		} else {
			// Create UncertainConnector
			UncertainConnector uConnector = ArchDSLFactory.eINSTANCE.createUncertainConnector();
			uConnector.setSuperInterface(connector);
			uConnector.setName(ArchModelUtils.getAutoUncertainConnectorName(sequenceName));
			model.getU_connectors().add(uConnector);

			// Convert Messages to UncertainBehavior and put it into UncertainConnector.
			UncertainBehavior uBehavior = convertToUncertainBehavior(model, messages, resolveType);
			if (uBehavior != null) {
				uBehavior.setName(ArchModelUtils.generateUncertainBehaviorName(uConnector));
				uConnector.getU_behaviors().add(uBehavior);
			}
		}
	}

	// Xtext ID terminal definition.
	// see https://eclipse.org/Xtext/documentation/301_grammarlanguage.html
	private static final Pattern idPattern = Pattern.compile("\\^?[a-zA-Z_]\\w*");
	private boolean isValidID(String name) {
		return idPattern.matcher(name).matches();
	}

	/**
	 * Generate an unique connector name.
	 * @param model
	 * @return
	 */
	private String generateConnectorName(Model model, String prefix) {
		for (int i = 0; ; ++i) {
			String name = prefix + String.valueOf(i);
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

	private String generateUncertainInterfaceName(Model model, String prefix) {
		for (int i = 0; ; ++i) {
			String name = prefix + String.valueOf(i);
			boolean isUnique = true;
			for (UncertainInterface uInterface : model.getU_interfaces()) {
				if (name.equals(uInterface.getName())) {
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

	private Behavior convertToBehavior(Model model, List<Message> messages,
			int resolveType) {
		Behavior behavior = ArchDSLFactory.eINSTANCE.createBehavior();

		for (Message message : messages) {
			if (!message.isArchpoint()) { continue; }

			String className = getClassName(message);
			if (className == null) {
				// Diagram may be broken.
				if (resolveType == RESOLVE_BY_ABORT) {
					return null;
				} else {
					continue;
				}
			}

			// Find Method from model.
			Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
			if (cInterface == null) {
				if (resolveType == RESOLVE_BY_ABORT) {
					return null;
				} else if (resolveType == RESOLVE_BY_SKIP) {
					continue;
				} else {
					// Generate Interface.
					cInterface = ArchModelUtils.createInterfaceElement(className);
					model.getInterfaces().add(cInterface);
				}
			}
			String methodName = message.getName();
			Method method = ArchModelUtils.findMethodByName(cInterface, methodName);
			if (method == null) {
				if (resolveType == RESOLVE_BY_ABORT) {
					return null;
				} else if (resolveType == RESOLVE_BY_SKIP) {
					continue;
				} else {
					// Generate Method.
					method = ArchModelUtils.createMethodElement(methodName);
					cInterface.getMethods().add(method);
				}
			}

			behavior.getCall().add(method);

			if (behavior.getInterface() == null) {
				behavior.setInterface(cInterface);
			}
			behavior.setEnd(cInterface);
		}

		return behavior.getCall().size() > 0 ? behavior : null;
	}

	private UncertainBehavior convertToUncertainBehavior(Model model, List<Message> messages,
			int resolveType) {
		UncertainBehavior uBehavior = ArchDSLFactory.eINSTANCE.createUncertainBehavior();

		for (Message message : messages) {
			if (!message.isArchpoint()) { continue; }

			if (message instanceof AlternativeMessage) {
				AltCall altCall = convertToAltCall(model, message, resolveType);
				if (altCall != null) {
					// AltCall->Method->AltMethod->UncertainInterface->Interface
					uBehavior.setEnd(((UncertainInterface) altCall.getName().eContainer().eContainer()).getSuperInterface());
					uBehavior.getCall().add(altCall);
				} else if (resolveType == RESOLVE_BY_ABORT) {
					return null;
				}
			} else if (message instanceof OptionalMessage) {
				OptCall optCall = convertToOptCall(model, message, resolveType);
				if (optCall != null) {
					// OptCall->Method->OptMethod->UncertainInterface->Interface
					uBehavior.setEnd(((UncertainInterface) optCall.getName().eContainer().eContainer()).getSuperInterface());
					uBehavior.getCall().add(optCall);
				} else if (resolveType == RESOLVE_BY_ABORT) {
					return null;
				}
			} else {
				CertainCall certainCall = convertToCertainCall(model, message, resolveType);
				if (certainCall != null) {
					uBehavior.setEnd((Interface) certainCall.getName().eContainer());
					uBehavior.getCall().add(certainCall);
				} else if (resolveType == RESOLVE_BY_ABORT) {
					return null;
				}
			}
		}

		return uBehavior.getCall().size() > 0 ? uBehavior : null;
	}

	private CertainCall convertToCertainCall(Model model, Message message, int resolveType) {
		String className = getClassName(message);
		if (className == null) {
			// Diagram may be broken.
			return null;
		}

		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			if (resolveType == RESOLVE_BY_GENERATE) {
				// Generate Interface.
				cInterface = ArchModelUtils.createInterfaceElement(className);
				model.getInterfaces().add(cInterface);
			} else {
				return null;
			}
		}

		String methodName = message.getName();
		Method method = ArchModelUtils.findMethodByName(cInterface, methodName);
		if (method == null) {
			if (resolveType == RESOLVE_BY_GENERATE) {
				// Generate Method.
				method = ArchModelUtils.createMethodElement(methodName);
				cInterface.getMethods().add(method);
			} else {
				return null;
			}
		}

		return ArchModelUtils.createCertainCallElement(method);
	}

	private OptCall convertToOptCall(Model model, Message message, int resolveType) {
		String className = getClassName(message);
		if (className == null) {
			// Diagram may be broken.
			return null;
		}

		String methodName = message.getName();

		OptMethod optMethod = null;
		for (UncertainInterface uInterface
				: ArchModelUtils.searchUncertainInterfaceBySuperName(model, className)) {
			for (OptMethod om : uInterface.getOptmethods()) {
				if (om.getMethod().getName().equals(methodName)) {
					optMethod = om;
					break;
				}
			}
			if (optMethod != null) {
				break;
			}
		}

		if (optMethod == null && resolveType == RESOLVE_BY_GENERATE) {
			// Generate UncertainInterface and OptMethod.
			Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
			if (cInterface == null) {
				cInterface = ArchModelUtils.createInterfaceElement(className);
				model.getInterfaces().add(cInterface);
			}

			String uIfName = generateUncertainInterfaceName(model,
					ArchModelUtils.getAutoUncertainInterfaceName(className));
			UncertainInterface uInterface =
					ArchModelUtils.createUncertainInterfaceElement(uIfName, cInterface);
			model.getU_interfaces().add(uInterface);

			optMethod = ArchModelUtils.createOptMethodElement(methodName);
			uInterface.getOptmethods().add(optMethod);
		}

		if (optMethod != null) {
			return ArchModelUtils.createOptCallElement(optMethod.getMethod());
		} else {
			return null;
		}
	}

	private AltCall convertToAltCall(Model model, Message message, int resolveType) {
		String className = getClassName(message);
		if (className == null) {
			// Diagram may be broken.
			return null;
		}

		List<MethodEquality> equalities = new ArrayList<MethodEquality>();
		for (Message m : ((AlternativeMessage) message).getMessages()) {
			equalities.add(MethodEqualityUtils.createMethodEquality(m.getName()));
		}
		MethodEquality altMethodEquality = MethodEqualityUtils.createMethodEqualityForAlt(equalities);

		AltMethod altMethod = null;
		for (UncertainInterface uInterface
				: ArchModelUtils.searchUncertainInterfaceBySuperName(model, className)) {
			for (AltMethod am : uInterface.getAltmethods()) {
				if (altMethodEquality.match(am)) {
					altMethod = am;
					break;
				}
			}
			if (altMethod != null) {
				break;
			}
		}

		if (altMethod == null && resolveType == RESOLVE_BY_GENERATE) {
			// Generate UncertainInterface and AltMethod.
			Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
			if (cInterface == null) {
				cInterface = ArchModelUtils.createInterfaceElement(className);
				model.getInterfaces().add(cInterface);
			}

			String uIfName = generateUncertainInterfaceName(model,
					ArchModelUtils.getAutoUncertainInterfaceName(className));
			UncertainInterface uInterface =
					ArchModelUtils.createUncertainInterfaceElement(uIfName, cInterface);
			model.getU_interfaces().add(uInterface);

			altMethod = ArchDSLFactory.eINSTANCE.createAltMethod();
			for (Message m : ((AlternativeMessage) message).getMessages()) {
				altMethod.getMethods().add(ArchModelUtils.createMethodElement(m.getName()));
			}
			uInterface.getAltmethods().add(altMethod);
		}

		if (altMethod != null) {
			AltCall altCall = ArchDSLFactory.eINSTANCE.createAltCall();
			boolean first = true;
			for (Method m : altMethod.getMethods()) {
				if (first) {
					altCall.setName(m);
					first = false;
				} else {
					altCall.getA_name().add(m);
				}
			}
			return altCall;
		} else {
			return null;
		}
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
