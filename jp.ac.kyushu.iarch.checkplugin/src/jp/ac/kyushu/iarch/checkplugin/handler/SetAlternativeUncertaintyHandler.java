package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.DiagramUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.MethodEquality;
import jp.ac.kyushu.iarch.checkplugin.utils.MethodEqualityUtils;
import jp.ac.kyushu.iarch.checkplugin.view.SelectAlternativeDialog;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class SetAlternativeUncertaintyHandler extends AbstractUncertaintyOperationHandler {
	@Override
	protected void operateCodeMethodDeclaration(ExecutionEvent event, MethodDeclarationInfo declInfo,
			ArchModel archModel) throws ExecutionException {
		String className = declInfo.typeName();
		String methodName = declInfo.methodName();
		Model model = archModel.getModel();

		Method baseMethod = ArchModelUtils.createMethodElement(declInfo.decl);
		List<Method> alternatives = getAlternativeMethods(event, model, declInfo.cInterface, baseMethod);
		if (alternatives == null) {
			System.out.println("SetAlternativeUncertaintyHandler: cannot create alternative methods.");
			return;
		}

		// Stop if OptMethod already exists.
		if (checkExistAltMethod(model, className, alternatives)) {
			System.out.println("SetAlternativeUncertaintyHandler: alternative method already exists: " + methodName);
			return;
		}

		boolean archModelModified = false;
		try {
			if (declInfo.inheritingTypes != null) {
				for (IType type : declInfo.inheritingTypes) {
					String inhClassName = type.getElementName();
					MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(inhClassName, methodName);
					// Duplicate alternatives.
					ArrayList<Method> inhAlternatives = new ArrayList<Method>();
					for (Method m : alternatives) {
						inhAlternatives.add(ArchModelUtils.createMethodElement(m));
					}
					archModelModified |= setAlternative(inhClassName, nameEquality, inhAlternatives, model);
				}
			} else {
				MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(className, methodName);
				archModelModified |= setAlternative(className, nameEquality, alternatives, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("SetAlternativeUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean setAlternative(String className, MethodEquality nameEquality,
			List<Method> alternatives, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		// Create AltMethod
		AltMethod newAltMethod = ArchModelUtils.createAltMethodElement(alternatives);
		// Try to add.
		AltMethod altMethod = addAltMethod(model, cInterface, newAltMethod);
		modified |= altMethod == newAltMethod;

		modified |= modifyCertainCallToAlt(model, MethodEqualityUtils.anyMethod, nameEquality, altMethod);
		modified |= generateUncertainBehaviorAlt(model, MethodEqualityUtils.anyMethod, nameEquality, altMethod);

		return modified;
	}

	private String[] getAlternativeMethodName(ExecutionEvent event, Model model, String methodName) {
		Shell shell = HandlerUtil.getActiveShell(event);
		SelectAlternativeDialog dialog = new SelectAlternativeDialog(shell, model, methodName);
		if (dialog.open() == MessageDialog.OK) {
			return dialog.getInput();
		} else {
			return null;
		}
	}

	private List<Method> getAlternativeMethods(ExecutionEvent event,
			Model model, Interface cInterface, Method baseMethod) {
		String[] altNames = getAlternativeMethodName(event, model, baseMethod.getName());
		if (altNames == null) {
			return null;
		}
		ArrayList<Method> alternatives = new ArrayList<Method>();
		UncertainInterface uInterface = getAutoUncertainInterface(model, cInterface);
		if (uInterface == null) {
			// Create all.
			alternatives.add(baseMethod);
			for (String altName: altNames) {
				// Type and Params are copied from baseMethod.
				Method newMethod = ArchModelUtils.createMethodElement(baseMethod);
				newMethod.setName(altName);
				alternatives.add(newMethod);
			}
		} else {
			// Try to find.
			MethodEquality baseEquality = MethodEqualityUtils.createMethodEquality(baseMethod);
			Method baseUMethod = findMethodFromUncertainInterface(uInterface, baseEquality);
			if (baseUMethod != null) {
				alternatives.add(baseUMethod);
			} else {
				alternatives.add(baseMethod);
			}
			for (String altName: altNames) {
				MethodEquality equality = MethodEqualityUtils.createMethodEquality(altName);
				Method uMethod = findMethodFromUncertainInterface(uInterface, equality);
				if (uMethod != null) {
					alternatives.add(uMethod);
				} else {
					Method newMethod = ArchModelUtils.createMethodElement(baseMethod);
					newMethod.setName(altName);
					alternatives.add(newMethod);
				}
			}
		}
		return alternatives;
	}
	private Method findMethodFromUncertainInterface(UncertainInterface uInterface,
			MethodEquality equality) {
		for (AltMethod am: uInterface.getAltmethods()) {
			if (equality.match(am)) {
				for (Method m: am.getMethods()) {
					if (equality.match(m)) {
						return m;
					}
				}
			}
		}
		for (OptMethod om: uInterface.getOptmethods()) {
			if (equality.match(om)) {
				return om.getMethod();
			}
		}
		return null;
	}

	private boolean checkExistAltMethod(Model model, String className, List<Method> alternatives) {
		List<MethodEquality> equalities = MethodEqualityUtils.createAltMethodEquality(alternatives, false);
		for (UncertainInterface uInterface:
			ArchModelUtils.searchUncertainInterfaceBySuperName(model, className)) {
			if (getAltMethod(uInterface, equalities) != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void operateCodeMethodInvocation(ExecutionEvent event, MethodInvocationInfo invInfo,
			ArchModel archModel) throws ExecutionException {
		String declClassName = invInfo.callerTypeName();
		String declName = invInfo.callerMethodName();
		String invClassName = invInfo.calleeTypeName();
		String invName = invInfo.calleeMethodName();
		Model model = archModel.getModel();

		Method baseMethod = ArchModelUtils.createMethodElement(invInfo.calleeBinding);
		List<Method> alternatives = getAlternativeMethods(event, model, invInfo.cInterface, baseMethod);
		if (alternatives == null) {
			System.out.println("SetAlternativeUncertaintyHandler: cannot create alternative methods.");
			return;
		}

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
					// Duplicate alternatives.
					ArrayList<Method> inhAlternatives = new ArrayList<Method>();
					for (Method m : alternatives) {
						inhAlternatives.add(ArchModelUtils.createMethodElement(m));
					}
					archModelModified |= setAlternative(inhClassName, declEquality, invEquality,
							inhAlternatives, model);
				}
			} else {
				MethodEquality invEquality = MethodEqualityUtils.createMethodEquality(invClassName, invName);
				archModelModified |= setAlternative(invClassName, declEquality, invEquality,
						alternatives, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("SetAlternativeUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean setAlternative(String className, MethodEquality declEquality, MethodEquality invEquality,
			List<Method> alternatives, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		AltMethod altMethod = getAltMethodFromAuto(model, cInterface, alternatives);
		boolean altMethodCreated = false;
		// If AltMethod does not exist in UncertainInterface, create it.
		if (altMethod == null) {
			AltMethod newAltMethod = ArchModelUtils.createAltMethodElement(alternatives);
			// Try to add.
			altMethod = addAltMethod(model, cInterface, newAltMethod);
			// Do not turn on modification flag
			// because new AltMethod is needed only when behaviors are modified.
			altMethodCreated = true;
		}

		modified |= modifyCertainCallToAlt(model, declEquality, invEquality, altMethod);
		modified |= generateUncertainBehaviorAlt(model, declEquality, invEquality, altMethod);

		if (altMethodCreated && !modified) {
			// Discard unused AltMethod.
			EObject container = altMethod.eContainer();
			if (container instanceof UncertainInterface) {
				((UncertainInterface) container).getAltmethods().remove(altMethod);
			} else {
				throw new ModelErrorException("Failed to discard unused AltMethod: " + className);
			}
		}

		return modified;
	}

	private AltMethod getAltMethodFromAuto(Model model, Interface cInterface,
			List<Method> alternatives) {
		UncertainInterface uInterface = getAutoUncertainInterface(model, cInterface);
		if (uInterface != null) {
			List<MethodEquality> equalities = MethodEqualityUtils.createAltMethodEquality(alternatives, false);
			return getAltMethod(uInterface, equalities);
		}
		return null;
	}

	@Override
	protected void operateDiagramOperation(ExecutionEvent event,
			OperationInfo operationInfo, ArchModel archModel) throws ExecutionException {
		String className = operationInfo.typeName();
		String methodName = operationInfo.methodName();
		Model model = archModel.getModel();

		// Stop if Operation is not certain.
		if (!operationInfo.isCertain()) {
			System.out.println("SetAlternativeUncertaintyHandler: Operation is not certain: " + methodName);
			return;
		}

		Method baseMethod = null;
		Method originalMethod = ArchModelUtils.findMethodByName(operationInfo.cInterface, methodName);
		if (originalMethod == null) {
			baseMethod = ArchModelUtils.createMethodElement(methodName);
		} else {
			baseMethod = ArchModelUtils.createMethodElement(originalMethod);
		}
		List<Method> alternatives = getAlternativeMethods(event, model, operationInfo.cInterface, baseMethod);
		if (alternatives == null) {
			System.out.println("SetAlternativeUncertaintyHandler: cannot create alternative methods.");
			return;
		}

		// Stop if OptMethod already exists.
		if (checkExistAltMethod(model, className, alternatives)) {
			System.out.println("SetAlternativeUncertaintyHandler: alternative method already exists: " + methodName);
			return;
		}

		boolean archModelModified = false;
		try {
			// Create AltMethod
			AltMethod newAltMethod = ArchModelUtils.createAltMethodElement(alternatives);
			// Try to add.
			AltMethod altMethod = addAltMethod(model, operationInfo.cInterface, newAltMethod);
			archModelModified |= altMethod == newAltMethod;

			MethodEquality nameEquality = operationInfo.methodEquality();
			archModelModified |= modifyCertainCallToAlt(model, MethodEqualityUtils.anyMethod, nameEquality, altMethod);
			archModelModified |= generateUncertainBehaviorAlt(model, MethodEqualityUtils.anyMethod, nameEquality, altMethod);

		} catch (ModelErrorException e) {
			System.out.println("SetAlternativeUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("SetAlternativeUncertaintyHandler: failed to save Archfile.");
			} else {
				ArrayList<String> names = new ArrayList<String>();
				for (Method m : alternatives) {
					names.add(m.getName());
				}
				boolean diagramModified =
						DiagramUtils.changeOperationToAlternative(operationInfo.operation, names);
				if (!diagramModified) {
					System.out.println("SetAlternativeUncertaintyHandler: failed to modify Class diagram.");
				}
			}
		}
	}

	@Override
	protected void operateDiagramMessage(ExecutionEvent event,
			MessageInfo messageInfo, ArchModel archModel) throws ExecutionException {
		String invName = messageInfo.calleeMethodName();
		Model model = archModel.getModel();

		// Stop if Operation is not certain.
		if (!messageInfo.isCertain()) {
			System.out.println("SetAlternativeUncertaintyHandler: Operation is not certain: " + invName);
			return;
		}

		Method baseMethod = ArchModelUtils.createMethodElement(invName);
		List<Method> alternatives = getAlternativeMethods(event, model, messageInfo.cInterface, baseMethod);
		if (alternatives == null) {
			System.out.println("SetAlternativeUncertaintyHandler: cannot create alternative methods.");
			return;
		}

		boolean archModelModified = false;
		try {
			AltMethod altMethod = getAltMethodFromAuto(model, messageInfo.cInterface, alternatives);
			// If AltMethod does not exist in UncertainInterface, create it.
			if (altMethod == null) {
				AltMethod newAltMethod = ArchModelUtils.createAltMethodElement(alternatives);
				// Try to add.
				altMethod = addAltMethod(model, messageInfo.cInterface, newAltMethod);
				// Do not turn on modification flag
				// because new AltMethod is needed only when behaviors are modified.
			}

			MethodEquality invEquality = messageInfo.calleeMethodEquality();
			MethodEquality declEquality = messageInfo.callerMethodEquality();
			archModelModified |= modifyCertainCallToAlt(model, declEquality, invEquality, altMethod);
			archModelModified |= generateUncertainBehaviorAlt(model, declEquality, invEquality, altMethod);

		} catch (ModelErrorException e) {
			System.out.println("SetAlternativeUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("SetAlternativeUncertaintyHandler: failed to save Archfile.");
			} else {
				ArrayList<String> names = new ArrayList<String>();
				for (Method m : alternatives) {
					names.add(m.getName());
				}
				boolean diagramModified =
						DiagramUtils.changeMessageToAlternative(messageInfo.calleeMessage, names);
				if (!diagramModified) {
					System.out.println("SetAlternativeUncertaintyHandler: failed to modify Sequence diagram.");
				}
			}
		}
	}
}
