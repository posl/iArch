package jp.ac.kyushu_u.iarch.checkplugin.handler;

import java.util.Iterator;
import java.util.List;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.ArchDSLFactory;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu_u.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu_u.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu_u.iarch.checkplugin.utils.DiagramUtils;
import jp.ac.kyushu_u.iarch.checkplugin.utils.MethodEquality;
import jp.ac.kyushu_u.iarch.checkplugin.utils.MethodEqualityUtils;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class RemoveUncertaintyAsUnnecessaryHandler extends AbstractUncertaintyOperationHandler {
	@Override
	protected void operateCodeMethodDeclaration(ExecutionEvent event, MethodDeclarationInfo declInfo,
			ArchModel archModel) throws ExecutionException {
		String className = declInfo.typeName();
		String methodName = declInfo.methodName();
		Model model = archModel.getModel();

		boolean archModelModified = false;
		try {
			if (declInfo.inheritingTypes != null) {
				for (IType type : declInfo.inheritingTypes) {
					String inhClassName = type.getElementName();
					MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(inhClassName, methodName);
					archModelModified |= removeAsUnnecessary(inhClassName, nameEquality, declInfo.decl, model);
				}
			} else {
				MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(className, methodName);
				archModelModified |= removeAsUnnecessary(className, nameEquality, declInfo.decl, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsUnnecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean removeAsUnnecessary(String className, MethodEquality nameEquality,
			MethodDeclaration decl, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		modified |= modifyUncertainCallAsUnnecesary(model, MethodEqualityUtils.anyMethod, nameEquality);
		if (modified) {
			modified |= removeCertainCall(model, MethodEqualityUtils.anyMethod, nameEquality);
		}

		MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(decl);
		modified |= removeUnusedUncertainMethod(model, className, methodEquality, false);
		modified |= removeUnusedMethod(model, cInterface, methodEquality);

		return modified;
	}

	private boolean modifyUncertainCallAsUnnecesary(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality) throws ModelErrorException {
		boolean modified = false;

		for (UncertainConnector uConnector: model.getU_connectors()) {
			Iterator<UncertainBehavior> uBehaviorIt = uConnector.getU_behaviors().iterator();
			while (uBehaviorIt.hasNext()) {
				UncertainBehavior uBehavior = uBehaviorIt.next();
				EList<SuperCall> superCalls = uBehavior.getCall();

				boolean uBehaviorModified = false;
				for (int i = 0; i < superCalls.size(); ++i) {
					SuperCall caller = (i > 0) ? superCalls.get(i - 1) : null;
					SuperCall callee = superCalls.get(i);

					if (callerEquality.match(caller)) {
						if (callee instanceof OptCall) {
							if (calleeEquality.match(callee)) {
								// Shortcut
								superCalls.remove(callee);
								// Prevent next call from being skipped.
								i--;
								uBehaviorModified = true;
								modified = true;
							}
						} else if (callee instanceof AltCall){
							// Check Methods one by one.
							SuperCall newSuperCall = shrinkAltCall((AltCall) callee, calleeEquality);
							if (newSuperCall != callee) {
								if (newSuperCall == null) {
									// Shortcut
									superCalls.remove(callee);
									i--;
								} else {
									if (newSuperCall instanceof AltCall) {
										addAltMethodIfNeeded(model, (AltCall) newSuperCall);
									} else if (newSuperCall instanceof CertainCall) {
										// Method from AltCall is not (assured to be) defined in certain Interface,
										// so convert to be certain.
										Method newMethod = getCertainMethod(model, (CertainCall) newSuperCall);
										if (newMethod != null) {
											((CertainCall) newSuperCall).setName(newMethod);
										} else {
											String message = "Failed to get/create certain Method: " + uConnector.getName();
											throw new ModelErrorException(message);
										}
									}
									superCalls.set(i, newSuperCall);
								}
								uBehaviorModified = true;
								modified = true;
							}
						}
					}
				}

				if (uBehaviorModified) {
					// Try to convert to certain Behavior.
					Behavior behavior = createCertainBehavior(uBehavior);
					if (behavior != null) {
						Connector connector = uConnector.getSuperInterface();
						addBehavior(connector, behavior);
						uBehaviorIt.remove();
					} else {
						// Check duplication.
						for (UncertainBehavior ub: uConnector.getU_behaviors()) {
							if (ub != uBehavior && ArchModelUtils.sameUncertainBehavior(ub, uBehavior)) {
								uBehaviorIt.remove();
							}
						}
					}
				}
			}
		}

		return modified;
	}
	private SuperCall shrinkAltCall(AltCall altCall, MethodEquality equality)
			throws ModelErrorException {
		AltCall newAltCall = ArchDSLFactory.eINSTANCE.createAltCall();
		newAltCall.setOpt(altCall.isOpt());

		// Check first Method.
		SuperMethod firstSuperMethod = altCall.getName();
		if (!(firstSuperMethod instanceof Method)) {
			String message = "AltCall contains other than certain Method.";
			throw new ModelErrorException(message);
		}
		if (equality.match((Method) firstSuperMethod)) {
			// Remove first Method.
			EList<SuperMethod> superMethods = altCall.getA_name();
			if (superMethods.size() == 0) {
				// Wired but shortcut
				return null;
			} else if (superMethods.size() == 1) {
				// Convert to CertainCall
				return ArchModelUtils.createCertainCallElement(superMethods.get(0));
			} else {
				// Set popped SuperMethod as first.
				newAltCall.setName(superMethods.get(0));
				for (int i = 1; i < superMethods.size(); ++i) {
					newAltCall.getA_name().add(superMethods.get(i));
				}
				return newAltCall;
			}
		}
		newAltCall.setName(firstSuperMethod);

		EList<SuperMethod> superMethods = altCall.getA_name();
		for (int i = 0; i < superMethods.size(); ++i) {
			SuperMethod superMethod = superMethods.get(i);
			if (!(superMethod instanceof Method)) {
				String message = "AltCall contains other than certain Method.";
				throw new ModelErrorException(message);
			}
			if (equality.match((Method) superMethod)) {
				// Add rest.
				for (int j = i + 1; j < superMethods.size(); ++j) {
					newAltCall.getA_name().add(superMethods.get(j));
				}
				if (newAltCall.getA_name().size() == 0) {
					// Convert to CertainCall
					return ArchModelUtils.createCertainCallElement(altCall.getName());
				} else {
					return newAltCall;
				}
			} else {
				newAltCall.getA_name().add(superMethod);
			}
		}

		// Not changed.
		return altCall;
	}
	private AltMethod addAltMethodIfNeeded(Model model, AltCall altCall) throws ModelErrorException {
		SuperMethod firstSuperMethod = altCall.getName();
		if (!(firstSuperMethod instanceof Method)) {
			String message = "AltCall contains other than certain Method.";
			throw new ModelErrorException(message);
		}
		String className = ArchModelUtils.getClassName((Method) firstSuperMethod);

		AltMethod altMethod = findAltMethod(model, className, altCall);
		if (altMethod == null) {
			altMethod = ArchModelUtils.createAltMethodElement(altCall);
			if (altMethod == null) {
				String message = "AltCall contains other than certain Method.";
				throw new ModelErrorException(message);
			}
			Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
			if (cInterface != null) {
				altMethod = addAltMethod(model, cInterface, altMethod);
			} else {
				String message = "interface is not found in Archcode: " + className;
				throw new ModelErrorException(message);
			}
		}
		return altMethod;
	}
	private AltMethod findAltMethod(Model model, String className, AltCall altCall) {
		List<MethodEquality> equalities = MethodEqualityUtils.createAltMethodEquality(className, altCall);
		for (UncertainInterface uInterface: ArchModelUtils.searchUncertainInterfaceBySuperName(model, className)) {
			for (AltMethod altMethod: uInterface.getAltmethods()) {
				if (MethodEqualityUtils.matchAltMethod(equalities, altMethod)) {
					return altMethod;
				}
			}
		}
		return null;
	}
	/**
	 * Get certain Method from SuperMethod within given CertainCall.
	 * If the certain Method does not exist, create it and add to Interface automatically.
	 * @return Found or created Method, or null if failed by some reason.
	 */
	private Method getCertainMethod(Model model, CertainCall certainCall) {
		SuperMethod superMethod = certainCall.getName();
		if (superMethod instanceof Method) {
			Method method = (Method) superMethod;
			MethodEquality equality = MethodEqualityUtils.createMethodEquality(method);
			String className = ArchModelUtils.getClassName(method);
			Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
			if (cInterface != null) {
				for (Method m: cInterface.getMethods()) {
					if (equality.match(m)) {
						return m;
					}
				}
				// Copy and add.
				Method newMethod = ArchModelUtils.createMethodElement(method);
				cInterface.getMethods().add(newMethod);
				return newMethod;
			}
		}
		return null;
	}

	private boolean removeCertainCall(Model model, MethodEquality callerEquality, MethodEquality calleeEquality) {
		boolean modified = false;

		for (Connector connector: model.getConnectors()) {
			Iterator<Behavior> behaviorIt = connector.getBehaviors().iterator();
			while (behaviorIt.hasNext()) {
				EList<Method> methods = behaviorIt.next().getCall();
				for (int i = 0; i < methods.size(); ++i) {
					Method caller = (i > 0) ? methods.get(i - 1) : null;
					Method callee = methods.get(i);

					if (callerEquality.match(caller) && calleeEquality.match(callee)) {
						behaviorIt.remove();
						modified = true;
						break;
					}
				}
			}
		}

		return modified;
	}

	private boolean removeUnusedMethod(Model model, Interface cInterface, MethodEquality equality) {
		boolean modified = false;
		String className = cInterface.getName();

		Iterator<Method> methodIt = cInterface.getMethods().iterator();
		while (methodIt.hasNext()) {
			Method method = methodIt.next();
			if (equality.match(method)) {
				MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(className, method);
				if (!isUsedMethod(model, methodEquality)) {
					methodIt.remove();
					modified = true;
				}
			}
		}

		return modified;
	}

	@Override
	protected void operateCodeMethodInvocation(ExecutionEvent event, MethodInvocationInfo invInfo,
			ArchModel archModel) throws ExecutionException {
		String declClassName = invInfo.callerTypeName();
		String declName = invInfo.callerMethodName();
		String invClassName = invInfo.calleeTypeName();
		String invName = invInfo.calleeMethodName();
		Model model = archModel.getModel();

		boolean archModelModified = false;
		try {
			MethodEquality declEquality = null;
			if (invInfo.callerInheritingTypes != null) {
				String[] declClassNames = new String[invInfo.callerInheritingTypes.length];
				for (int i = 0; i < declClassNames.length; ++i) {
					declClassNames[i] = invInfo.callerInheritingTypes[i].getElementName();
				}
				declEquality = MethodEqualityUtils.createMethodEquality(declClassNames, declName);
			} else {
				declEquality = MethodEqualityUtils.createMethodEquality(declClassName, declName);
			}

			if (invInfo.calleeInheritingTypes != null) {
				for (IType type : invInfo.calleeInheritingTypes) {
					String inhClassName = type.getElementName();
					MethodEquality invEquality = MethodEqualityUtils.createMethodEquality(inhClassName, invName);
					archModelModified |= removeAsUnnecessary(inhClassName, declEquality, invEquality,
							invInfo.calleeBinding, model);
				}
			} else {
				MethodEquality invEquality = MethodEqualityUtils.createMethodEquality(invClassName, invName);
				archModelModified |= removeAsUnnecessary(invClassName, declEquality, invEquality,
						invInfo.calleeBinding, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsUnnecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean removeAsUnnecessary(String className, MethodEquality declEquality, MethodEquality invEquality,
			IMethodBinding calleeBinding, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		modified |= modifyUncertainCallAsUnnecesary(model, declEquality, invEquality);
		if (modified) {
			modified |= removeCertainCall(model, declEquality, invEquality);
		}

		if (modified) {
			MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(calleeBinding);
			modified |= removeUnusedUncertainMethod(model, className, methodEquality, false);
			modified |= removeUnusedMethod(model, cInterface, methodEquality);
		}

		return modified;
	}

	@Override
	protected void operateDiagramOperation(ExecutionEvent event,
			OperationInfo operationInfo, ArchModel archModel) throws ExecutionException {
		String className = operationInfo.typeName();
		String methodName = operationInfo.methodName();
		Model model = archModel.getModel();

		// Stop if Operation is certain.
		if (operationInfo.isCertain()) {
			System.out.println("RemoveUncertaintyAsUnnecessaryHandler: Operation is not uncertain: " + methodName);
			return;
		}

		// TODO: Assume that first method is to be unnecessary.
		String unnecessaryMethodName = operationInfo.methodNames().get(0);
		boolean archModelModified = false;
		try {
			MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(className, unnecessaryMethodName);
			archModelModified |= modifyUncertainCallAsUnnecesary(model, MethodEqualityUtils.anyMethod, nameEquality);
			if (archModelModified) {
				archModelModified |= removeCertainCall(model, MethodEqualityUtils.anyMethod, nameEquality);
			}

			MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(unnecessaryMethodName);
			archModelModified |= removeUnusedUncertainMethod(model, className, methodEquality, false);
			archModelModified |= removeUnusedMethod(model, operationInfo.cInterface, methodEquality);
		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsUnnecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("RemoveUncertaintyAsUnnecessaryHandler: failed to save Archfile.");
			} else {
				boolean diagramModified =
						DiagramUtils.changeOperationToUnnecessary(operationInfo.operation, unnecessaryMethodName);
				if (!diagramModified) {
					System.out.println("RemoveUncertaintyAsUnnecessaryHandler: failed to modify Class diagram.");
				}
			}
		}
	}

	@Override
	protected void operateDiagramMessage(ExecutionEvent event,
			MessageInfo messageInfo, ArchModel archModel) throws ExecutionException {
		String invClassName = messageInfo.calleeTypeName();
		String invName = messageInfo.calleeMethodName();
		Model model = archModel.getModel();

		// Stop if Message is certain.
		if (messageInfo.isCertain()) {
			System.out.println("RemoveUncertaintyAsUnnecessaryHandler: Message is not uncertain: " + invName);
			return;
		}

		// Unnecessary method is invName, which is selected by User.
		String unnecessaryMethodName = invName;
		boolean archModelModified = false;
		try {
			MethodEquality invEquality = MethodEqualityUtils.createMethodEquality(invClassName, unnecessaryMethodName);
			MethodEquality declEquality = messageInfo.callerMethodEquality();
			archModelModified |= modifyUncertainCallAsUnnecesary(model, declEquality, invEquality);
			if (archModelModified) {
				archModelModified |= removeCertainCall(model, declEquality, invEquality);
			}

			if (archModelModified) {
				MethodEquality methodEquality = MethodEqualityUtils.createMethodEquality(unnecessaryMethodName);
				archModelModified |= removeUnusedUncertainMethod(model, invClassName, methodEquality, false);
				archModelModified |= removeUnusedMethod(model, messageInfo.cInterface, methodEquality);
			}
		} catch (ModelErrorException e) {
			System.out.println("RemoveUncertaintyAsUnnecessaryHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("RemoveUncertaintyAsUnnecessaryHandler: failed to save Archfile.");
			} else {
				boolean diagramModified =
						DiagramUtils.changeMessageToUnnecessary(messageInfo.calleeMessage, unnecessaryMethodName);
				if (!diagramModified) {
					System.out.println("RemoveUncertaintyAsUnnecessaryHandler: failed to modify Class diagram.");
				}
			}
		}
	}
}
