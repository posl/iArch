package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.ArchDSLFactory;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu.iarch.basefunction.utils.MessageDialogUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.CodeXMLUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.CodeXMLUtils.FindVisitor;
import jp.ac.kyushu.iarch.checkplugin.utils.DiagramUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.MethodEquality;
import jp.ac.kyushu.iarch.checkplugin.utils.MethodEqualityUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.ui.editor.IDiagramEditorInput;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import behavior.AlternativeMessage;
import behavior.Message;
import behavior.OptionalMessage;
import umlClass.AlternativeOperation;
import umlClass.Operation;
import umlClass.OptionalOperation;

public abstract class AbstractUncertaintyOperationHandler extends AbstractHandler {
	// IDs for target editors.
	private static final String CODE_EDITOR_ID = "org.eclipse.jdt.ui.CompilationUnitEditor";
	private static final String DIAGRAM_EDITOR_ID = "org.eclipse.graphiti.ui.editor.DiagramEditor";

	// Flag to consider superclasses/subclasses as targets of operations.
	private static final boolean CONSIDER_INHERITANCE = true;

	/**
	 * Exception to signal errors from operations on ArchModel such as:<br>
	 * 1. UncertainInterface/UncertainConnector to which objects are added do not extend appropriate Interface/Connector.<br>
	 * 2. SuperCall contains other than pure Method.
	 */
	protected class ModelErrorException extends Exception {
		// Auto generated serial version UID.
		private static final long serialVersionUID = -2076198670476386635L;

		protected ModelErrorException(String message) {
			super(message);
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String partId = HandlerUtil.getActivePartId(event);
		if (CODE_EDITOR_ID.equals(partId)) {
			operateCodeEditor(event);
		} else if (DIAGRAM_EDITOR_ID.equals(partId)) {
			operateDiagramEditor(event);
		} else {
			if (partId == null) {
				partId = "null";
			}
			String className = this.getClass().getSimpleName();
			System.out.println(className + ": active part is out of targets: " + partId);
		}
		return null;
	}

	private IFile getEditorInputFile(ExecutionEvent event) {
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		IEditorInput editorInput = editorPart.getEditorInput();
		if (editorInput instanceof IFileEditorInput) {
			// Code editor.
			return ((IFileEditorInput) editorInput).getFile();
		} else if (editorInput instanceof IDiagramEditorInput) {
			// Diagram editor.
			URI uri = ((IDiagramEditorInput) editorInput).getUri();
			if (uri.isPlatform()) {
				Path path = new Path(uri.trimFragment().toPlatformString(true));
				return ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			}
		}
		return null;
	}
	private int getCursorLineNumber(ExecutionEvent event) {
		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
		if (editorPart instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) editorPart;
			ISelection selection = editor.getSelectionProvider().getSelection();
			if (selection != null && selection instanceof ITextSelection) {
				// returned value of getStartLine is 0-origin.
				return ((ITextSelection) selection).getStartLine() + 1;
			}
		}
		return -1;
	}

	protected boolean saveArchModel(ArchModel archModel) {
		boolean result = false;
		try {
			archModel.save();
			result = true;
		} catch (IOException e) {
			e.printStackTrace();
			String className = this.getClass().getSimpleName();
			MessageDialogUtils.showError(className, "Failed to save Archfile.");
			System.out.println(className + ": failed to save Archfile.");
		} catch (RuntimeException e) {
			// Model error falls here.
			String className = this.getClass().getSimpleName();
			String m = e.getMessage();
			StringBuilder sb = new StringBuilder("Model validation failed.\n");
			sb.append(m != null ? m : "Unknown reason.");
			MessageDialogUtils.showError(className, sb.toString());
		}
		return result;
	}

	protected class MethodDeclarationInfo {
		public TypeDeclaration typeDecl;
		public MethodDeclaration decl;
		public Interface cInterface;
		public IType[] inheritingTypes;

		private MethodDeclarationInfo(TypeDeclaration typeDecl, MethodDeclaration decl, Interface cInterface) {
			this(typeDecl, decl, cInterface, null);
		}
		private MethodDeclarationInfo(TypeDeclaration typeDecl, MethodDeclaration decl,
				Interface cInterface, IType[] inheritingTypes) {
			this.typeDecl = typeDecl;
			this.decl = decl;
			this.cInterface = cInterface;
			this.inheritingTypes = inheritingTypes;
		}

		public String typeName() {
			return typeDecl != null ? typeDecl.getName().toString() : null;
		}
		public String methodName() {
			return decl != null ? decl.getName().toString() : null;
		}
	}

	protected class MethodInvocationInfo {
		public TypeDeclaration callerTypeDecl;
		public MethodDeclaration callerDecl;
		public IMethodBinding calleeBinding;
		public Interface cInterface;
		public IType[] callerInheritingTypes;
		public IType[] calleeInheritingTypes;

		private MethodInvocationInfo(TypeDeclaration callerTypeDecl, MethodDeclaration callerDecl,
				IMethodBinding calleeBinding, Interface cInterface) {
			this(callerTypeDecl, callerDecl, calleeBinding, cInterface, null, null);
		}
		private MethodInvocationInfo(TypeDeclaration callerTypeDecl, MethodDeclaration callerDecl,
				IMethodBinding calleeBinding, Interface cInterface,
				IType[] callerInheritingTypes, IType[] calleeInheritingTypes) {
			this.callerTypeDecl = callerTypeDecl;
			this.callerDecl = callerDecl;
			this.calleeBinding = calleeBinding;
			this.cInterface = cInterface;
			this.callerInheritingTypes = callerInheritingTypes;
			this.calleeInheritingTypes = calleeInheritingTypes;
		}

		public String callerTypeName() {
			return callerTypeDecl != null ? callerTypeDecl.getName().toString() : null;
		}
		public String callerMethodName() {
			return callerDecl != null ? callerDecl.getName().toString() : null;
		}
		public String calleeTypeName() {
			return cInterface != null ? cInterface.getName() : null;
		}
		public String calleeMethodName() {
			return calleeBinding != null ? calleeBinding.getName() : null;
		}
	}

	private ASTNode findAncestor(ASTNode node, Class<?> clazz) {
		for (ASTNode parent = node.getParent(); parent != null; parent = parent.getParent()) {
			if (clazz.isInstance(parent)) {
				return parent;
			}
		}
		return null;
	}

	private IType[] getInheritingTypes(MethodDeclaration decl, Model model) {
		IMethodBinding methodBinding = decl.resolveBinding();
		if (methodBinding != null) {
			return getInheritingTypes(methodBinding, null, model);
		}
		return null;
	}
	private IType[] getInheritingTypes(IMethodBinding methodBinding, ITypeBinding expTypeBinding, Model model) {
		ITypeBinding declTypeBinding = methodBinding.getDeclaringClass();
		if (declTypeBinding != null) {
			IJavaElement methodElement = methodBinding.getJavaElement();
			if (methodElement instanceof IMethod) {
				IMethod method = (IMethod) methodElement;

				// Decide start type so as to avoid searching vastly.
				IType startType = null;
				if (expTypeBinding != null) {
					ITypeBinding startTypeBinding = null;
					ITypeBinding tb = expTypeBinding;
					while (tb != null && tb.isFromSource()) {
						startTypeBinding = tb;
						if (tb.isEqualTo(declTypeBinding)) {
							break;
						}
						tb = tb.getSuperclass();
					}
					if (startTypeBinding != null) {
						IJavaElement je = startTypeBinding.getJavaElement();
						if (je instanceof IType) {
							startType = (IType) je;
						}
					}
				}
				if (startType == null) {
					IJavaElement je = declTypeBinding.getJavaElement();
					if (je instanceof IType) {
						startType = (IType) je;
					}
				}

				if (startType != null) {
					try {
						ITypeHierarchy hierarchy = startType.newTypeHierarchy(new NullProgressMonitor());
						ArrayList<IType> results = new ArrayList<IType>();
						fetchInheritingTypes(hierarchy, startType, method, model, results);
						return results.toArray(new IType[results.size()]);
					} catch (JavaModelException e) {
					}
				}
			}
		}
		return null;
	}
	private void fetchInheritingTypes(ITypeHierarchy hierarchy,
			IType type, IMethod method, Model model, List<IType> results) {
		// If model has interface for type, add to results.
		String typeName = type.getElementName();
		if (ArchModelUtils.findInterfaceByName(model, typeName) != null) {
			results.add(type);
		}

		// Search direct subtypes.
		for (IType subtype : hierarchy.getSubtypes(type)) {
			// If subtypes does not override specified method,
			if (subtype.findMethods(method) == null) {
				// Search for subtype's subtypes.
				fetchInheritingTypes(hierarchy, subtype, method, model, results);
			}
		}
	}

	private void operateCodeEditor(ExecutionEvent event) throws ExecutionException {
		String className = this.getClass().getSimpleName();

		// Get target file and line number.
		IFile file = getEditorInputFile(event);
		int startLine = getCursorLineNumber(event);
		if (file == null || startLine <= 0) {
			System.out.println(className + ": failed to obtain target.");
			return;
		}

		// Get Archface model from the path given by Config.xml
		IResource archfile = new XMLreader(file.getProject()).getArchfileResource();
		if (archfile == null) {
			System.out.println(className + ": failed to get the archfile resource.");
			return;
		}
		ArchModel archModel = new ArchModel(archfile);
		Model model = archModel.getModel();

		// Check if cursor line is on MethodDeclaration or MethodInvocation.
		ICompilationUnit compilationUnit = JavaCore.createCompilationUnitFrom(file);
		if (compilationUnit == null) {
			System.out.println(className + ": failed to get Java compilcation unit.");
			return;
		}

		FindVisitor visitor = new FindVisitor(null) {
			@Override
			public boolean visit(MethodDeclaration node) {
				return visitCommon(node);
			}
			@Override
			public boolean visit(MethodInvocation node) {
				return visitCommon(node);
			}
		};
		ASTNode node = CodeXMLUtils.findNodeByLine(compilationUnit, startLine, visitor);

		if (node != null) {
			if (node instanceof MethodDeclaration) {
				MethodDeclaration decl = (MethodDeclaration) node;

				// Get class node.
				TypeDeclaration typeDecl = (TypeDeclaration) findAncestor(decl, TypeDeclaration.class);
				if (typeDecl == null) {
					// AST may be incorrect.
					String methodName = decl.getName().toString();
					System.out.println(className + ": class is not found in code: " + methodName);

				} else {
					// Find Interface by name.
					String typeName = typeDecl.getName().toString();
					Interface cInterface = ArchModelUtils.findInterfaceByName(model, typeName);
					if (cInterface == null) {
						System.out.println(className + ": interface is not found in Archcode: " + typeName);

					} else {
						MethodDeclarationInfo declInfo = null;
						if (CONSIDER_INHERITANCE) {
							IType[] inheritingTypes = getInheritingTypes(decl, model);
							if (inheritingTypes == null) {
								System.out.println(className + ": failed to get types which inherit the method: " + typeName);
								// Keep going.
							}
							declInfo = new MethodDeclarationInfo(typeDecl, decl, cInterface, inheritingTypes);
						} else {
							declInfo = new MethodDeclarationInfo(typeDecl, decl, cInterface);
						}

						operateCodeMethodDeclaration(event, declInfo, archModel);
					}
				}

			} else if (node instanceof MethodInvocation) {
				MethodInvocation inv = (MethodInvocation) node;
				String invName = inv.getName().toString();

				// Get caller nodes.
				MethodDeclaration decl = (MethodDeclaration) findAncestor(inv, MethodDeclaration.class);
				if (decl == null) {
					// AST may be incorrect.
					System.out.println(className + ": method is not found in code: " + invName);

				} else {
					TypeDeclaration typeDecl = (TypeDeclaration) findAncestor(decl, TypeDeclaration.class);
					if (typeDecl == null) {
						// AST may be incorrect.
						System.out.println(className + ": class is not found in code: " + invName);

					} else {
						// Get class name to which the invocated method belongs.
						IMethodBinding invBinding = inv.resolveMethodBinding();
						if (invBinding == null) {
							System.out.println(className + ": failed to resolve invocated method: " + invName);

						} else {
							ITypeBinding invClassBinding = invBinding.getDeclaringClass();
							if (invClassBinding == null) {
								System.out.println(className + ": failed to resolve class of invocated method: " + invName);

//							} else if (!invClassBinding.isFromSource()) {
//								System.out.println(className + ": invocated method is not defined on source codes: " + invName);

							} else {
								ITypeBinding invExpClassBinding = null;
								Expression invExp = inv.getExpression();
								if (invExp != null) {
									invExpClassBinding = invExp.resolveTypeBinding();
								} else {
									invExpClassBinding = typeDecl.resolveBinding();
								}

								// Find Interface by name.
								String invClassName = null;
								if (invExpClassBinding != null) {
									invClassName = invExpClassBinding.getName();
								} else {
									invClassName = invClassBinding.getName();
								}
								Interface cInterface = ArchModelUtils.findInterfaceByName(model, invClassName);
								if (cInterface == null) {
									System.out.println(className + ": interface is not found in Archcode: " + invClassName);

								} else {
									MethodInvocationInfo invInfo = null;
									if (CONSIDER_INHERITANCE) {
										IType[] callerInheritingTypes = getInheritingTypes(decl, model);
										IType[] calleeInheritingTypes = getInheritingTypes(invBinding, invExpClassBinding, model);
										if (callerInheritingTypes == null) {
											System.out.println(className + ": failed to get types which inherit the method: " + decl.getName().toString());
											// Keep going.
										}
										if (calleeInheritingTypes == null) {
											System.out.println(className + ": failed to get types which inherit the method: " + invName);
											// Keep going.
										}
										invInfo = new MethodInvocationInfo(typeDecl, decl,
												invBinding, cInterface, callerInheritingTypes, calleeInheritingTypes);
									} else {
										invInfo = new MethodInvocationInfo(typeDecl, decl, invBinding, cInterface);
									}

									operateCodeMethodInvocation(event, invInfo, archModel);
								}
							}
						}
					}
				}

			} else {
				System.out.println(className + ": cursor is not on Method declaration nor invocation.");
			}
		} else {
			System.out.println(className + ": cursor is not on Method declaration nor invocation.");
		}
	}
	protected abstract void operateCodeMethodDeclaration(ExecutionEvent event,
			MethodDeclarationInfo declInfo, ArchModel archModel) throws ExecutionException;
	protected abstract void operateCodeMethodInvocation(ExecutionEvent event,
			MethodInvocationInfo invInfo, ArchModel archModel) throws ExecutionException;

	protected class OperationInfo {
		public String className;
		public Operation operation;
		public Interface cInterface;

		private OperationInfo(String className, Operation operation, Interface cInterface) {
			this.className = className;
			this.operation = operation;
			this.cInterface = cInterface;
		}

		public String typeName() {
			return className;
		}
		public String methodName() {
			return operation.getName();
		}
		public boolean isCertain() {
			return !(isOptional() || isAlternative());
		}
		public boolean isOptional() {
			return operation instanceof OptionalOperation;
		}
		public boolean isAlternative() {
			return operation instanceof AlternativeOperation;
		}

		public List<String> methodNames() {
			ArrayList<String> names = new ArrayList<String>();
			if (operation instanceof AlternativeOperation) {
				for (Operation op : ((AlternativeOperation) operation).getOperations()) {
					names.add(op.getName());
				}
			} else {
				names.add(operation.getName());
			}
			return names;
		}

		public MethodEquality methodEquality() {
			if (operation instanceof AlternativeOperation) {
				ArrayList<MethodEquality> equalities = new ArrayList<MethodEquality>();
				for (Operation op : ((AlternativeOperation) operation).getOperations()) {
					equalities.add(MethodEqualityUtils.createMethodEquality(className, op.getName()));
				}
				return MethodEqualityUtils.createMethodEqualityForAlt(equalities);
			} else {
				MethodEquality equality =
						MethodEqualityUtils.createMethodEquality(className, operation.getName());
				if (operation instanceof OptionalOperation) {
					return MethodEqualityUtils.createMethodEqualityForOpt(equality);
				} else {
					return equality;
				}
			}
		}
	}

	protected class MessageInfo {
		public String callerClassName;
		public Message callerMessage;
		public String calleeClassName;
		public Message calleeMessage;
		public String calleeMethodName;
		public Interface cInterface;

		private MessageInfo(String callerClassName, Message callerMessage,
				String calleeClassName, Message calleeMessage, String calleeMethodName,
				Interface cInterface) {
			this.callerClassName = callerClassName;
			this.callerMessage = callerMessage;
			this.calleeClassName = calleeClassName;
			this.calleeMessage = calleeMessage;
			this.calleeMethodName = calleeMethodName;
			this.cInterface = cInterface;
		}

		public String callerTypeName() {
			return callerClassName;
		}
		public String callerMethodName() {
			return callerMessage == null ? null : callerMessage.getName();
		}
		public String calleeTypeName() {
			return calleeClassName;
		}
		public String calleeMethodName() {
			return calleeMethodName;
		}
		public boolean isCertain() {
			return !(isOptional() || isAlternative());
		}
		public boolean isOptional() {
			return calleeMessage instanceof OptionalMessage;
		}
		public boolean isAlternative() {
			return calleeMessage instanceof AlternativeMessage;
		}

		private MethodEquality methodEquality(String className, Message message) {
			if (message instanceof AlternativeMessage) {
				ArrayList<MethodEquality> equalities = new ArrayList<MethodEquality>();
				for (Message m : ((AlternativeMessage) message).getMessages()) {
					equalities.add(MethodEqualityUtils.createMethodEquality(className, m.getName()));
				}
				return MethodEqualityUtils.createMethodEqualityForAlt(equalities);
			} else {
				MethodEquality equality =
						MethodEqualityUtils.createMethodEquality(className, message.getName());
				if (message instanceof OptionalMessage) {
					return MethodEqualityUtils.createMethodEqualityForOpt(equality);
				} else {
					return equality;
				}
			}
		}
		public MethodEquality callerMethodEquality() {
			if (callerClassName == null || callerMessage == null) {
				return MethodEqualityUtils.nullMethod;
			} else {
				return methodEquality(callerClassName, callerMessage);
			}
		}
		public MethodEquality calleeMethodEquality() {
			return methodEquality(calleeClassName, calleeMessage);
		}
	}

	private void operateDiagramEditor(ExecutionEvent event) throws ExecutionException {
		String className = this.getClass().getSimpleName();

		// Get target file.
		IFile file = getEditorInputFile(event);
		if (file == null) {
			System.out.println(className + ": failed to obtain target.");
			return;
		}

		// Get Archface model from the path given by Config.xml
		IResource archfile = new XMLreader(file.getProject()).getArchfileResource();
		if (archfile == null) {
			System.out.println(className + ": failed to get the archfile resource.");
			return;
		}
		ArchModel archModel = new ArchModel(archfile);
		Model model = archModel.getModel();

		// Get business object from selection.
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		EObject businessObject = DiagramUtils.getBusinessObject(selection);
		if (businessObject == null) {
			System.out.println(className + ": failed to get business object from selection.");
			return;
		}

		// Check if business object is umlClass.Operation or behavior.Message
		if (businessObject instanceof Operation) {
			Operation operation = (Operation) businessObject;
			String methodName = operation.getName();

			umlClass.Class eClass = operation.getClass_();
			if (eClass == null) {
				System.out.println(className + ": failed to get class name:" + methodName);

			} else {
				// Find Interface by name.
				String invClassName = eClass.getName();
				Interface cInterface = ArchModelUtils.findInterfaceByName(model, invClassName);
				if (cInterface == null) {
					System.out.println(className + ": interface is not found in Archcode: " + invClassName);

				} else {
					OperationInfo operationInfo = new OperationInfo(invClassName, operation, cInterface);
					operateDiagramOperation(event, operationInfo, archModel);
				}
			}

		} else if (businessObject instanceof Message) {
			Message message = (Message) businessObject;
			Message calleeMessage = DiagramUtils.getTargetMessage(message);
			if (calleeMessage == null) {
				System.out.println(className + ": failed to get callee Message.");

			} else {
				String calleeClassName = DiagramUtils.getMessageClassName(calleeMessage);
				if (calleeClassName == null) {
					System.out.println(className + ": failed to get callee class name.");

				} else {
					// Find Interface by name.
					Interface cInterface = ArchModelUtils.findInterfaceByName(model, calleeClassName);
					if (cInterface == null) {
						System.out.println(className + ": interface is not found in Archcode: " + calleeClassName);

					} else {
						// callerMessage/callerClassName can be null.
						Message callerMessage = DiagramUtils.getCaller(calleeMessage);
						String callerClassName = callerMessage == null ? null
								: DiagramUtils.getMessageClassName(callerMessage);

						MessageInfo messageInfo = new MessageInfo(callerClassName, callerMessage,
								calleeClassName, calleeMessage, message.getName(), cInterface);
						operateDiagramMessage(event, messageInfo, archModel);
					}
				}
			}

		} else {
			System.out.println(className + ": selected object is not Operation nor Message.");
		}
	}
	protected abstract void operateDiagramOperation(ExecutionEvent event,
			OperationInfo operationInfo, ArchModel archModel) throws ExecutionException;
	protected abstract void operateDiagramMessage(ExecutionEvent event,
			MessageInfo messageInfo, ArchModel archModel) throws ExecutionException;

	protected UncertainInterface getAutoUncertainInterface(Model model, Interface cInterface) {
		String uClassName = ArchModelUtils.getAutoUncertainInterfaceName(cInterface.getName());
		return ArchModelUtils.findUncertainInterfaceByName(model, uClassName);
	}
	protected UncertainConnector getAutoUncertainConnector(Model model, Connector connector) {
		String uConnectorName = ArchModelUtils.getAutoUncertainConnectorName(connector.getName());
		return ArchModelUtils.findUncertainConnectorByName(model, uConnectorName);
	}

	private UncertainInterface createAutoUncertainInterface(Interface cInterface) {
		String uClassName = ArchModelUtils.getAutoUncertainInterfaceName(cInterface.getName());
		return ArchModelUtils.createUncertainInterfaceElement(uClassName, cInterface);
	}
	private UncertainConnector createAutoUncertainConnector(Connector connector) {
		String uConnectorName = ArchModelUtils.getAutoUncertainConnectorName(connector.getName());
		return ArchModelUtils.createUncertainConnectorElement(uConnectorName, connector);
	}

	protected Method getMethod(Interface cInterface, MethodEquality equality) {
		for (Method method: cInterface.getMethods()) {
			if (equality.match(method)) {
				return method;
			}
		}
		return null;
	}
	protected OptMethod getOptMethod(UncertainInterface uInterface, MethodEquality equality) {
		for (OptMethod optMethod: uInterface.getOptmethods()) {
			if (equality.match(optMethod.getMethod())) {
				return optMethod;
			}
		}
		return null;
	}
	protected AltMethod getAltMethod(UncertainInterface uInterface, List<MethodEquality> equalities) {
		for (AltMethod altMethod: uInterface.getAltmethods()) {
			if (MethodEqualityUtils.matchAltMethod(equalities, altMethod)) {
				return altMethod;
			}
		}
		return null;
	}

	/**
	 * Try to add specified OptMethod to UncertainInterface.
	 * If UncertainInterface does not exist, create it automatically.
	 * @param model
	 * @param cInterface
	 * @param optMethod
	 * @return OptMethod actually added. when other object than optMethod is returned, adding is not performed.
	 * @throws ModelErrorException is thrown when target UncertainInterface does not extend appropriate Interface.
	 */
	protected OptMethod addOptMethod(Model model, Interface cInterface, OptMethod optMethod)
			throws ModelErrorException {
		// Find auto-generated uncertain interface.
		UncertainInterface uInterface = getAutoUncertainInterface(model, cInterface);

		boolean uInterfaceCreated = false;
		if (uInterface != null) {
			// Check if it extends given interface.
			String className = cInterface.getName();
			Interface superInterface = uInterface.getSuperInterface();
			if (superInterface == null || !superInterface.getName().equals(className)) {
				String uClassName = uInterface.getName();
				String message = "Target uncertain component does not extend appropriate interface: " + uClassName;
				throw new ModelErrorException(message);
			}
		} else {
			// Create UncertainInterface
			uInterface = createAutoUncertainInterface(cInterface);
			uInterfaceCreated = true;
		}

		// Add to UncertainInterface
		OptMethod addedOptMethod = addOptMethod(uInterface, optMethod);
		if (addedOptMethod == optMethod && uInterfaceCreated) {
			model.getU_interfaces().add(uInterface);
		}
		return addedOptMethod;
	}
	private OptMethod addOptMethod(UncertainInterface uInterface, OptMethod optMethod) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(optMethod.getMethod());
		for (OptMethod om: uInterface.getOptmethods()) {
			// Check duplication.
			if (equality.match(om)) {
				return om;
			}
		}
		uInterface.getOptmethods().add(optMethod);
		return optMethod;
	}

	/**
	 * Try to add specified AltMethod to UncertainInterface.
	 * If UncertainInterface does not exist, create it automatically.
	 * @param model
	 * @param cInterface
	 * @param altMethod
	 * @return AltMethod actually added. when other object than altMethod is returned, adding is not performed.
	 * @throws ModelErrorException is thrown when target UncertainInterface does not extend appropriate Interface.
	 */
	protected AltMethod addAltMethod(Model model, Interface cInterface, AltMethod altMethod)
			throws ModelErrorException {
		// Find auto-generated uncertain interface.
		UncertainInterface uInterface = getAutoUncertainInterface(model, cInterface);

		boolean uInterfaceCreated = false;
		if (uInterface != null) {
			// Check if it extends given interface.
			String className = cInterface.getName();
			Interface superInterface = uInterface.getSuperInterface();
			if (superInterface == null || !superInterface.getName().equals(className)) {
				String uClassName = uInterface.getName();
				String message = "Target uncertain component does not extend appropriate interface: " + uClassName;
				throw new ModelErrorException(message);
			}
		} else {
			// Create UncertainInterface
			uInterface = createAutoUncertainInterface(cInterface);
			uInterfaceCreated = true;
		}

		// Add to UncertainInterface
		AltMethod addedAltMethod = addAltMethod(uInterface, altMethod);
		if (addedAltMethod == altMethod && uInterfaceCreated) {
			model.getU_interfaces().add(uInterface);
		}
		return addedAltMethod;
	}
	private AltMethod addAltMethod(UncertainInterface uInterface, AltMethod altMethod) {
		List<MethodEquality> equalities = MethodEqualityUtils.createAltMethodEquality(altMethod, false);
		for (AltMethod am: uInterface.getAltmethods()) {
			// Check duplication.
			if (MethodEqualityUtils.matchAltMethod(equalities, am)) {
				return am;
			}
		}
		uInterface.getAltmethods().add(altMethod);
		return altMethod;
	}

	private interface SuperCallFactory {
		public SuperCall create(Method callee);
	}
	private class SuperCallFactoryFromOpt implements SuperCallFactory {
		private OptMethod optMethod;
		private SuperCallFactoryFromOpt(OptMethod optMethod) {
			this.optMethod = optMethod;
		}
		public SuperCall create(Method callee) {
			return ArchModelUtils.createOptCallElement(optMethod != null ? optMethod.getMethod() : callee);
		}
	};
	private class SuperCallFactoryFromAlt implements SuperCallFactory {
		private AltMethod altMethod;
		private SuperCallFactoryFromAlt(AltMethod altMethod) {
			this.altMethod = altMethod;
		}
		public SuperCall create(Method callee) {
			return ArchModelUtils.createAltCallElement(altMethod);
		}
	};

	/**
	 * Generate UncertainBehavior from Behavior which contains a call which matches equality conditions.<br>
	 * In generating UncertainBehavior, OptCall which has given optMethod substitutes the concerned call.<br>
	 * If generated UncertainBehavior equals to the other, generated one is not added.
	 * @return true if any UncertainBehavior is generated.
	 * @throws ModelErrorException is thrown when target UncertainConnector does not extend appropriate Connector.
	 */
	protected boolean generateUncertainBehaviorOpt(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality, OptMethod optMethod)
			throws ModelErrorException {
		SuperCallFactory factory = new SuperCallFactoryFromOpt(optMethod);
		return generateUncertainBehavior(model, callerEquality, calleeEquality, factory);
	}
	/**
	 * Generate UncertainBehavior from Behavior which contains a call which matches equality conditions.<br>
	 * In generating UncertainBehavior, AltCall which has given altMethod substitutes the concerned call.<br>
	 * If generated UncertainBehavior equals to the other, generated one is not added.
	 * @return true if any UncertainBehavior is generated.
	 * @throws ModelErrorException is thrown when target UncertainConnector does not extend appropriate Connector.
	 */
	protected boolean generateUncertainBehaviorAlt(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality, AltMethod altMethod)
			throws ModelErrorException {
		SuperCallFactory factory = new SuperCallFactoryFromAlt(altMethod);
		return generateUncertainBehavior(model, callerEquality, calleeEquality, factory);
	}
	private boolean generateUncertainBehavior(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality, SuperCallFactory factory)
			throws ModelErrorException {
		boolean modified = false;

		for (Connector connector: model.getConnectors()) {
			// Find UncertainConnector to which uncertain behaviors are added.
			UncertainConnector uConnector = getAutoUncertainConnector(model, connector);

			boolean uConnectorCreated = false;
			if (uConnector != null) {
				// Check if it extends given interface.
				String connectorName = connector.getName();
				Connector superConnector = uConnector.getSuperInterface();
				if (superConnector == null || !superConnector.getName().equals(connectorName)) {
					String uConnectorName = uConnector.getName();
					String message = "Target uncertain connector does not extend appropriate connector: " + uConnectorName;
					throw new ModelErrorException(message);
				}
			}

			boolean uConnectorModified = false;
			for (Behavior behavior: connector.getBehaviors()) {
				EList<Method> methods = behavior.getCall();

				// Create UncertainBehavior instance.
				UncertainBehavior uBehavior = ArchDSLFactory.eINSTANCE.createUncertainBehavior();

				boolean uBehaviorGenerated = false;
				for (int i = 0; i < methods.size(); ++i) {
					Method caller = (i > 0) ? methods.get(i - 1) : null;
					Method callee = methods.get(i);

					// Add CertainCall or SuperCall by factory to UncertainBehavior.
					SuperCall superCall = null;
					if (callerEquality.match(caller) && calleeEquality.match(callee)) {
						superCall = factory.create(callee);
						uBehaviorGenerated = true;
					} else {
						superCall = ArchModelUtils.createCertainCallElement(callee);
					}
					uBehavior.getCall().add(superCall);
				}

				if (uBehaviorGenerated) {
					if (uConnector == null) {
						uConnector = createAutoUncertainConnector(connector);
						uConnectorCreated = true;
					}

					// Why name property is defined?
					String name = ArchModelUtils.generateUncertainBehaviorName(uConnector);
					uBehavior.setName(name);
					uBehavior.setEnd(behavior.getEnd());

					UncertainBehavior addedUBehavior = addUncertainBehavior(uConnector, uBehavior);
					uConnectorModified |= addedUBehavior == uBehavior;
				}
				// Otherwise, uBehavior is discarded.
			}

			if (uConnectorModified) {
				if (uConnectorCreated) {
					model.getU_connectors().add(uConnector);
				}
				modified = true;
			}
		}
		return modified;
	}
	private UncertainBehavior addUncertainBehavior(UncertainConnector uConnector, UncertainBehavior uBehavior) {
		for (UncertainBehavior ub: uConnector.getU_behaviors()) {
			// Check duplication
			if (sameUncertainBehavior(ub, uBehavior)) {
				return ub;
			}
		}
		uConnector.getU_behaviors().add(uBehavior);
		return uBehavior;
	}
	protected boolean sameUncertainBehavior(UncertainBehavior ub1, UncertainBehavior ub2) {
		// Name is not concerned.
		Interface end1 = ub1.getEnd();
		Interface end2 = ub2.getEnd();
		if (end1 == null) {
			if (end2 != null) {
				return false;
			}
		} else {
			if (end2 == null) {
				return false;
			}
			if (!end1.getName().equals(end2.getName())) {
				return false;
			}
		}
		EList<SuperCall> call1 = ub1.getCall();
		EList<SuperCall> call2 = ub2.getCall();
		if (call1.size() != call2.size()) {
			return false;
		}
		for (int i = 0; i < call1.size(); ++i) {
			if (!MethodEqualityUtils.sameSuperCall(call1.get(i), call2.get(i), true)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Modify UncertainBehavior which contains a CertainCall whose call matches equality conditions.<br>
	 * In the modification, OptCall which has given optMethod substitutes the concerned call.<br>
	 * If modificated UncertainBehavior equals to the other, modificated one is removed.
	 * @return true if any modification occurs.
	 */
	protected boolean modifyCertainCallToOpt(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality, OptMethod optMethod) {
		SuperCallFactory factory = new SuperCallFactoryFromOpt(optMethod);
		return modifyCertainCallTo(model, callerEquality, calleeEquality, factory);
	}
	/**
	 * Modify UncertainBehavior which contains a CertainCall whose call matches equality conditions.<br>
	 * In the modification, AltCall which has given altMethod substitutes the concerned call.<br>
	 * If modificated UncertainBehavior equals to the other, modificated one is removed.
	 * @return true if any modification occurs.
	 */
	protected boolean modifyCertainCallToAlt(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality, AltMethod altMethod) {
		SuperCallFactory factory = new SuperCallFactoryFromAlt(altMethod);
		return modifyCertainCallTo(model, callerEquality, calleeEquality, factory);
	}
	private boolean modifyCertainCallTo(Model model,
			MethodEquality callerEquality, MethodEquality calleeEquality, SuperCallFactory factory) {
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

					Method calleeMethod = ArchModelUtils.getMethodIfCertain(callee);
					if (calleeMethod != null && calleeEquality.match(calleeMethod)
							&& callerEquality.match(caller)) {
						// Change CertainCall to SuperCall
						superCalls.set(i, factory.create(calleeMethod));
						modified = true;
					}
				}

				if (uBehaviorModified) {
					for (UncertainBehavior ub: uConnector.getU_behaviors()) {
						if (ub != uBehavior && sameUncertainBehavior(ub, uBehavior)) {
							uBehaviorIt.remove();
						}
					}
				}
			}
		}

		return modified;
	}

	/**
	 * Create Behavior from UncertainBehavior if it has CertainCall only.
	 * @return Created Behavior or null if cannot.
	 * @throws ModelErrorException is thrown when failed by the problem other than uncertainty.
	 */
	protected Behavior createCertainBehavior(UncertainBehavior uBehavior) throws ModelErrorException {
		if (ArchModelUtils.hasUncertainty(uBehavior)) {
			return null;
		}
		Behavior behavior = ArchModelUtils.createBehaviorElement(uBehavior);
		if (behavior == null) {
			String message = "Failed to convert to certain Behavior.";
			throw new ModelErrorException(message);
		}
		return behavior;
	}

	protected Behavior addBehavior(Connector connector, Behavior behavior) {
		// Add only if the same behavior does not exist.
		for (Behavior b: connector.getBehaviors()) {
			if (sameBehavior(behavior, b)) {
				// returns existing one instead.
				return b;
			}
		}
		connector.getBehaviors().add(behavior);
		return behavior;
	}
	private boolean sameBehavior(Behavior b1, Behavior b2) {
		if (!b1.getInterface().getName().equals(b2.getInterface().getName())) {
			return false;
		}
		Interface end1 = b1.getEnd();
		Interface end2 = b2.getEnd();
		if (end1 == null) {
			if (end2 != null) {
				return false;
			}
		} else {
			if (end2 == null) {
				return false;
			}
			if (!end1.getName().equals(end2.getName())) {
				return false;
			}
		}
		EList<Method> call1 = b1.getCall();
		EList<Method> call2 = b2.getCall();
		if (call1.size() != call2.size()) {
			return false;
		}
		for (int i = 0; i < call1.size(); ++i) {
			if (!MethodEqualityUtils.sameMethod(call1.get(i), call2.get(i), true)) {
				return false;
			}
		}
		return true;
	}

	protected boolean removeUnusedUncertainMethod(Model model, String className, MethodEquality equality,
			boolean excludeMethod) {
		boolean modified = false;
		ArrayList<Method> excludedAltMethods = new ArrayList<Method>();

		for (UncertainInterface uInterface:
			ArchModelUtils.searchUncertainInterfaceBySuperName(model, className)) {
			boolean optMethodChecked = false;
			boolean optMethodUsed = true;
			Iterator<OptMethod> optMethodIt = uInterface.getOptmethods().iterator();
			while (optMethodIt.hasNext()) {
				OptMethod optMethod = optMethodIt.next();
				if (equality.match(optMethod)) {
					// One check is sufficient for OptMethod.
					if (!optMethodChecked) {
						optMethodUsed = isUsedOptMethod(model, className, optMethod);
					}
					if (!optMethodUsed) {
						optMethodIt.remove();
						modified = true;
					}
				}
			}

			Iterator<AltMethod> altMethodIt = uInterface.getAltmethods().iterator();
			while (altMethodIt.hasNext()) {
				AltMethod altMethod = altMethodIt.next();
				if (equality.match(altMethod) && !isUsedAltMethod(model, className, altMethod)) {
					if (excludeMethod) {
						for (Method m: altMethod.getMethods()) {
							if (!equality.match(m)) {
								excludedAltMethods.add(m);
							}
						}
					}
					altMethodIt.remove();
					modified = true;
				}
			}
		}

		if (excludeMethod) {
			// Remove alternatives which no behavior uses.
			Interface cInterface = ArchModelUtils.findInterfaceByName(model, className);
			if (cInterface != null) {
				for (Method excluded: excludedAltMethods) {
					Method targetMethod = null;
					MethodEquality excludedEquality = MethodEqualityUtils.createMethodEquality(excluded);
					for (Method m: cInterface.getMethods()) {
						if (excludedEquality.match(m)) {
							targetMethod = m;
							break;
						}
					}
					if (targetMethod != null) {
						MethodEquality targetEquality = MethodEqualityUtils.createMethodEquality(className, targetMethod);
						if (!isUsedMethod(model, targetEquality)) {
							cInterface.getMethods().remove(targetMethod);
						}
					}
				}
			}
		}

		return modified;
	}

	private boolean isUsedOptMethod(Model model, String className, OptMethod optMethod) {
		MethodEquality equality = MethodEqualityUtils.createMethodEquality(className, optMethod.getMethod());
		for (UncertainConnector uConnector: model.getU_connectors()) {
			for (UncertainBehavior uBehavior: uConnector.getU_behaviors()) {
				for (SuperCall superCall: uBehavior.getCall()) {
					if (superCall instanceof OptCall
							&& equality.match(((OptCall) superCall).getName())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isUsedAltMethod(Model model, String className, AltMethod altMethod) {
		List<MethodEquality> equalities = MethodEqualityUtils.createAltMethodEquality(className, altMethod);
		for (UncertainConnector uConnector: model.getU_connectors()) {
			for (UncertainBehavior uBehavior: uConnector.getU_behaviors()) {
				for (SuperCall superCall: uBehavior.getCall()) {
					if (superCall instanceof AltCall
							&& MethodEqualityUtils.matchAltCall(equalities, (AltCall) superCall)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	protected boolean isUsedMethod(Model model, MethodEquality equality) {
		// Certain
		for (Connector connector: model.getConnectors()) {
			for (Behavior behavior: connector.getBehaviors()) {
				for (Method method: behavior.getCall()) {
					if (equality.match(method)) {
						return true;
					}
				}
			}
		}
		// Uncertain
		for (UncertainConnector uConnector: model.getU_connectors()) {
			for (UncertainBehavior uBehavior: uConnector.getU_behaviors()) {
				for (SuperCall superCall: uBehavior.getCall()) {
					if (equality.match(superCall)) {
						return true;
					}
				}
			}
		}
		// Direct
		for (Behavior behavior: model.getBehaviors()) {
			for (Method method: behavior.getCall()) {
				if (equality.match(method)) {
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.commands.IHandler#isHandled()
	 */
	@Override
	public boolean isHandled() {
		return true;
	}
}
