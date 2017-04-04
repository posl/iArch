package jp.ac.kyushu.iarch.checkplugin.handler;

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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class SetOptionalUncertaintyHandler extends AbstractUncertaintyOperationHandler {
	@Override
	protected void operateCodeMethodDeclaration(ExecutionEvent event, MethodDeclarationInfo declInfo,
			ArchModel archModel) throws ExecutionException {
		String className = declInfo.typeName();
		String methodName = declInfo.methodName();
		Model model = archModel.getModel();

		// Stop if OptMethod already exists.
		if (checkExistOptMethod(model, className, declInfo.decl)) {
			System.out.println("SetOptionalUncertaintyHandler: optional method already exists: " + methodName);
			return;
		}

		boolean archModelModified = false;
		try {
			if (declInfo.inheritingTypes != null) {
				for (IType type : declInfo.inheritingTypes) {
					String inhClassName = type.getElementName();
					MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(inhClassName, methodName);
					archModelModified |= setOptional(inhClassName, nameEquality, declInfo.decl, model);
				}
			} else {
				MethodEquality nameEquality = MethodEqualityUtils.createMethodEquality(className, methodName);
				archModelModified |= setOptional(className, nameEquality, declInfo.decl, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("SetOptionalUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean setOptional(String className, MethodEquality nameEquality,
			MethodDeclaration decl, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		// Create new OptMethod.
		OptMethod newOptMethod = ArchModelUtils.createOptMethodElement(decl);
		// Try to add.
		OptMethod optMethod = addOptMethod(model, cInterface, newOptMethod);
		modified |= optMethod == newOptMethod;

		// Substitute Call.
		modified |= modifyCertainCallToOpt(model, MethodEqualityUtils.anyMethod, nameEquality, optMethod);
		modified |= generateUncertainBehaviorOpt(model, MethodEqualityUtils.anyMethod, nameEquality, optMethod);

		return modified;
	}

	private boolean checkExistOptMethod(Model model, String className, MethodDeclaration decl) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(decl);
		return checkExistOptMethod(model, className, equality);
	}
	private boolean checkExistOptMethod(Model model, String className, MethodEquality equality) {
		for (UncertainInterface uInterface:
			ArchModelUtils.searchUncertainInterfaceBySuperName(model, className)) {
			if (getOptMethod(uInterface, equality) != null) {
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
					archModelModified |= setOptional(inhClassName, declEquality, invEquality,
							invInfo.calleeBinding, model);
				}
			} else {
				MethodEquality invEquality = MethodEqualityUtils.createMethodEquality(invClassName, invName);
				archModelModified |= setOptional(invClassName, declEquality, invEquality,
						invInfo.calleeBinding, model);
			}
		} catch (ModelErrorException e) {
			System.out.println("SetOptionalUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			saveArchModel(archModel);
		}
	}
	private boolean setOptional(String className, MethodEquality declEquality, MethodEquality invEquality,
			IMethodBinding calleeBinding, Model model) throws ModelErrorException {
		Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
		if (cInterface == null) {
			throw new ModelErrorException("Target interface does not exist: " + className);
		}
		boolean modified = false;

		OptMethod optMethod = getOptMethodFromAuto(model, cInterface, calleeBinding);
		boolean optMethodCreated = false;
		// If OptMethod does not exist in UncertainInterface, create it.
		if (optMethod == null) {
			OptMethod newOptMethod = ArchModelUtils.createOptMethodElement(calleeBinding);
			// Try to add.
			optMethod = addOptMethod(model, cInterface, newOptMethod);
			// Do not turn on modification flag
			// because new OptMethod is needed only when behaviors are modified.
			optMethodCreated = true;
		}

		// Substitute Call.
		modified |= modifyCertainCallToOpt(model, declEquality, invEquality, optMethod);
		modified |= generateUncertainBehaviorOpt(model, declEquality, invEquality, optMethod);

		if (optMethodCreated && !modified) {
			// Discard unused OptMethod.
			UncertainInterface uInterface = ArchModelUtils.getUncertainInterface(optMethod);
			if (uInterface != null) {
				uInterface.getAltmethods().remove(optMethod);
			} else {
				throw new ModelErrorException("Failed to discard unused OptMethod: " + className);
			}
		}

		return modified;
	}

	private OptMethod getOptMethodFromAuto(Model model, Interface cInterface, IMethodBinding binding) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(binding);
		return getOptMethodFromAuto(model, cInterface, equality);
	}
	private OptMethod getOptMethodFromAuto(Model model, Interface cInterface, MethodEquality equality) {
		UncertainInterface uInterface = getAutoUncertainInterface(model, cInterface);
		if (uInterface != null) {
			return getOptMethod(uInterface, equality);
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
			System.out.println("SetOptionalUncertaintyHandler: Operation is not certain: " + methodName);
			return;
		}

		// Stop if OptMethod already exists.
		if (checkExistOptMethod(model, className, methodName)) {
			System.out.println("SetOptionalUncertaintyHandler: optional method already exists: " + methodName);
			return;
		}

		boolean archModelModified = false;
		try {
			// Create new OptMethod.
			OptMethod newOptMethod = null;
			Method originalMethod = ArchModelUtils.findMethodByName(operationInfo.cInterface, methodName);
			if (originalMethod != null) {
				// If Method found, use copy of it.
				Method innerMethod = ArchModelUtils.createMethodElement(originalMethod);
				newOptMethod = ArchModelUtils.createOptMethodElement(innerMethod);
			} else {
				newOptMethod = ArchModelUtils.createOptMethodElement(methodName);
			}

			// Try to add.
			OptMethod optMethod = addOptMethod(model, operationInfo.cInterface, newOptMethod);
			archModelModified |= optMethod == newOptMethod;

			// Substitute Call.
			MethodEquality nameEquality = operationInfo.methodEquality();
			archModelModified |= modifyCertainCallToOpt(model, MethodEqualityUtils.anyMethod, nameEquality, optMethod);
			archModelModified |= generateUncertainBehaviorOpt(model, MethodEqualityUtils.anyMethod, nameEquality, optMethod);

		} catch (ModelErrorException e) {
			System.out.println("SetOptionalUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("SetOptionalUncertaintyHandler: failed to save Archfile.");
			} else {
				boolean diagramModified = DiagramUtils.changeOperationToOptional(operationInfo.operation);
				if (!diagramModified) {
					System.out.println("SetOptionalUncertaintyHandler: failed to modify Class diagram.");
				}
			}
		}
	}

	private boolean checkExistOptMethod(Model model, String className, String methodName) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(methodName);
		return checkExistOptMethod(model, className, equality);
	}

	@Override
	protected void operateDiagramMessage(ExecutionEvent event,
			MessageInfo messageInfo, ArchModel archModel) throws ExecutionException {
		String invName = messageInfo.calleeMethodName();
		Model model = archModel.getModel();

		// Stop if Message is not certain.
		if (!messageInfo.isCertain()) {
			System.out.println("SetOptionalUncertaintyHandler: Message is not certain: " + invName);
			return;
		}

		boolean archModelModified = false;
		try {
			OptMethod optMethod = getOptMethodFromAuto(model, messageInfo.cInterface, invName);
			// If OptMethod does not exist in UncertainInterface, create it.
			if (optMethod == null) {
				OptMethod newOptMethod = ArchModelUtils.createOptMethodElement(invName);
				// Try to add.
				optMethod = addOptMethod(model, messageInfo.cInterface, newOptMethod);
				// Do not turn on modification flag
				// because new OptMethod is needed only when behaviors are modified.
			}

			// Substitute Call.
			MethodEquality invEquality = messageInfo.calleeMethodEquality();
			MethodEquality declEquality = messageInfo.callerMethodEquality();
			archModelModified |= modifyCertainCallToOpt(model, declEquality, invEquality, optMethod);
			archModelModified |= generateUncertainBehaviorOpt(model, declEquality, invEquality, optMethod);

		} catch (ModelErrorException e) {
			System.out.println("SetOptionalUncertaintyHandler: " + e.getMessage());
			return;
		}

		if (archModelModified) {
			boolean saved = saveArchModel(archModel);
			if (!saved) {
				System.out.println("SetOptionalUncertaintyHandler: failed to save Archfile.");
			} else {
				boolean diagramModified = DiagramUtils.changeMessageToOptional(messageInfo.calleeMessage);
				if (!diagramModified) {
					System.out.println("SetOptionalUncertaintyHandler: failed to modify Sequence diagram.");
				}
			}
		}
	}

	private OptMethod getOptMethodFromAuto(Model model, Interface cInterface, String methodName) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(methodName);
		return getOptMethodFromAuto(model, cInterface, equality);
	}
}
