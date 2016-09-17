package jp.ac.kyushu.iarch.classdiagram.features;

import java.util.HashSet;
import java.util.Set;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.model.ComponentTypeCheckModel;
import jp.ac.kyushu.iarch.basefunction.model.ComponentTypeCheckModel.MethodModel;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu.iarch.basefunction.utils.PlatformUtils;
import jp.ac.kyushu.iarch.basefunction.utils.ProblemViewManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.pictograms.Diagram;

import umlClass.AlternativeOperation;
import umlClass.Operation;
import umlClass.OptionalOperation;

public class TypeCheckFeature extends AbstractCustomFeature {
	public TypeCheckFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public String getName() {
		return "Type Check";
	}

	@Override
	public String getDescription() {
		return "Performs type check.";
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		return true;
	}

	@Override
	public void execute(ICustomContext context) {
		// Get file of class diagram.
		IFile diagramFile = PlatformUtils.getActiveFile();

		// Get Archface model within the project.
		IProject project = diagramFile != null ? diagramFile.getProject() : null;
		if (project == null) {
			System.out.println("TypeCheckFeature: failed to get active project.");
			return;
		}
		IResource archfile = new XMLreader(project).getArchfileResource();
		ArchModel archModel = new ArchModel(archfile);
		Model model = archModel.getModel();

		doTypeCheck(diagramFile, model, getDiagram());
	}

	private void doTypeCheck(IResource diagramFile, Model model, Diagram diagram) {
		// TODO: Change marker type to unique one so as not to remove by other checkers.
		ProblemViewManager problemViewManager = ProblemViewManager.getInstance();
		// Remove previously added markers.
		problemViewManager.removeProblems(diagramFile, false);

		// For each Class object in diagram
		// (Note that objects are assumed to be direct children of the root.)
		for (EObject obj : diagram.eResource().getContents()) {
			if (obj instanceof umlClass.Class) {
				umlClass.Class eClass = (umlClass.Class) obj;
				String className = eClass.getName();
				System.out.println("[Class " + className + "]");

				// Construct a component model for type check from archface.
				ComponentTypeCheckModel ctcModel =
						ComponentTypeCheckModel.getTypeCheckModel(model, className);
				if (ctcModel == null) {
					String msg = "Not defined in Archcode.";
					System.out.println(msg);
					problemViewManager.createWarningMarker(diagramFile, "", className);
				} else {
					// For each method in component model,
					for (MethodModel mm : ctcModel.getMethodModels()) {
						String methodName = mm.getMethod().getName();
						String classMethodName = className + "." + methodName;

						if (mm.isCertain()) {
							// If the method in archface is certain (not overridden by opt/alt),
							// corresponding operation in model should be defined as certain.
							Operation operation = findNonAltOperation(eClass, methodName);
							if (operation == null) {
								String msg = "Not defined in Class model.";
								System.out.println("ERROR: " + methodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName, 10);
							} else if (operation instanceof OptionalOperation) {
								String msg = "Not defined as Certain.";
								System.out.println("ERROR: " + methodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);
							} else {
								System.out.println("INFO: " + methodName + ": Defined as Certain.");
							}
						} else if (mm.isOptional()) {
							// If the method in archface is optional (not overridden by alt),
							// corresponding operation in model should be defined as optional.
							String optMethodName = "opt[" + methodName + "]";
							Operation operation = findNonAltOperation(eClass, methodName);
							if (operation == null) {
								String msg = "Not defined in Class model.";
								System.out.println("ERROR: " + optMethodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);
							} else if (operation instanceof OptionalOperation) {
								System.out.println("INFO: " + optMethodName + ": Defined as Optional.");
							} else {
								String msg = "Not defined as Optional.";
								System.out.println("ERROR: " + optMethodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);
							}
						}
					}

					// For each alternative method in component model,
					for (AltMethod am : ctcModel.getAltMethods()) {
						String altMethodName = joinAltNames(am, " ", "alt{", "}");
						String classAltMethodName = joinAltNames(am, "/", className + ".", null);

						// 1. any alternatives should not be defined as certain.
						boolean definedAsCertain = false;
						for (Method m : am.getMethods()) {
							String methodName = m.getName();
							Operation operation = findNonAltOperation(eClass, methodName);
							if (operation == null) {
								// NOP
							} else if (operation instanceof OptionalOperation) {
								// NOP but checked afterward
							} else {
								String msg = "Defined as Certain: " + methodName;
								System.out.println("ERROR: " + altMethodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classAltMethodName);
								definedAsCertain = true;
							}
						}

						// 2. same alternative operation should be defined in class model.
						if (!definedAsCertain) {
							HashSet<String> methodNames = new HashSet<String>();
							for (Method m : am.getMethods()) {
								methodNames.add(m.getName());
							}
							AlternativeOperation altOperation = findAltOperation(eClass, methodNames);
							if (altOperation == null) {
								String msg = "Not defined in Class model.";
								System.out.println("ERROR: " + altMethodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classAltMethodName);
							} else {
								System.out.println("INFO: " + altMethodName + ": Defined as Alternative.");
							}
						}
					}

					// For each optional/alternative operation in class model,
					for (Operation op : eClass.getOwnedOperation()) {
						String methodName = op.getName();
						if (op instanceof OptionalOperation) {
							// If operation is optional,
							// corresponding optional method should exist.
							MethodModel mm = ctcModel.getMethodModel(methodName);
							if (mm == null) {
								String optMethodName = "opt[" + methodName + "]";
								String classMethodName = className + "." + methodName;

								String msg = "Not defined in Archcode.";
								System.out.println("ERROR: " + optMethodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);
							}
						} else if (op instanceof AlternativeOperation) {
							// If operation is alternative,
							// corresponding alternative method should exist.
							HashSet<String> altMethodNames = new HashSet<String>();
							for (Operation childOp : ((AlternativeOperation) op).getOperations()) {
								altMethodNames.add(childOp.getName());
							}
							AltMethod am = ctcModel.getAltMethod(altMethodNames);
							if (am == null) {
								String altMethodName = joinAltNames((AlternativeOperation) op, " ", "alt{", "}");
								String classMethodName = joinAltNames((AlternativeOperation) op, "/", className + ".", null);

								String msg = "Not defined in Archcode.";
								System.out.println("ERROR: " + altMethodName + ": " + msg);
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);
							}
						} else {
							// If operation is certain,
							// corresponding method may not exist.
							MethodModel mm = ctcModel.getMethodModel(methodName);
							if (mm == null) {
								System.out.println("INFO: " + methodName + ": Not defined in Archcode.");
							}
						}
					}
				}
			}
		}
	}

	private Operation findNonAltOperation(umlClass.Class eClass, String opName){
		for (Operation op : eClass.getOwnedOperation()) {
			if (!(op instanceof AlternativeOperation)
					&& op.getName().equals(opName)) {
				return op;
			}
		}
		return null;
	}
	private AlternativeOperation findAltOperation(umlClass.Class eClass, Set<String> opNames) {
		for (Operation op : eClass.getOwnedOperation()) {
			if (op instanceof AlternativeOperation) {
				AlternativeOperation aop = (AlternativeOperation) op;
				if (aop.getOperations().size() == opNames.size()) {
					boolean hasOperation = true;
					for (Operation childOp : aop.getOperations()) {
						if (!opNames.contains(childOp.getName())) {
							hasOperation = false;
							break;
						}
					}
					if (hasOperation) {
						return aop;
					}
				}
			}
		}
		return null;
	}

	private String joinAltNames(AltMethod altMethod, String sep, String pre, String post) {
		StringBuilder sb = new StringBuilder();
		if (pre != null) {
			sb.append(pre);
		}
		for (Method m : altMethod.getMethods()) {
			sb.append(m.getName()).append(sep);
		}
		sb.deleteCharAt(sb.length() - sep.length());
		if (post != null) {
			sb.append(post);
		}
		return sb.toString();
	}
	private String joinAltNames(AlternativeOperation altOperation, String sep, String pre, String post) {
		StringBuilder sb = new StringBuilder();
		if (pre != null) {
			sb.append(pre);
		}
		for (Operation op : altOperation.getOperations()) {
			sb.append(op.getName()).append(sep);
		}
		sb.deleteCharAt(sb.length() - sep.length());
		if (post != null) {
			sb.append(post);
		}
		return sb.toString();
	}

	@Override
	public boolean hasDoneChanges() {
		// If results of type check modify objects, it should return true.
		return false;
	}
}
