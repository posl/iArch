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
					problemViewManager.createWarningMarker(diagramFile, msg, className);
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
								String msg = "Certain method is not defined.";
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);

								System.out.println("ERROR: " + methodName + ": Not defined in Class model.");
							} else if (operation instanceof OptionalOperation) {
								String msg = "Certain method is defined as Optional.";
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);

								System.out.println("ERROR: " + methodName + ": Not defined as Certain.");
							} else {
								System.out.println("INFO: " + methodName + ": Defined as Certain.");
							}
						} else if (mm.isOptional()) {
							// If the method in archface is optional (not overridden by alt),
							// corresponding operation in model should be defined as optional.
							String optMethodName = "opt[" + methodName + "]";
							Operation operation = findNonAltOperation(eClass, methodName);
							if (operation == null) {
								String msg = "Optional method is not defined.";
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);

								System.out.println("ERROR: " + optMethodName + ": Not defined in Class model.");
							} else if (operation instanceof OptionalOperation) {
								System.out.println("INFO: " + optMethodName + ": Defined as Optional.");
							} else {
								String msg = "Optional method is defined as Certain.";
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);

								System.out.println("ERROR: " + optMethodName + ": Not defined as Optional.");
							}
						}
					}

					// For each alternative method in component model,
					for (AltMethod am : ctcModel.getAltMethods()) {
						String altMethodName = joinAltNames(am, " ", "alt{", "}");
						String classAltMethodName = joinAltNames(am, ",", className + ".{", "}");

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
								String msg = "Candidate of Alternative method is defined as Certain.";
								problemViewManager.createErrorMarker(diagramFile, msg, classAltMethodName);
								definedAsCertain = true;

								System.out.println("ERROR: " + altMethodName + ": Defined as Certain: " + methodName);
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
								String msg = "Alternative method is not defined.";
								problemViewManager.createErrorMarker(diagramFile, msg, classAltMethodName);

								System.out.println("ERROR: " + altMethodName + ": Not defined in Class model.");
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
								String msg = "Optional operation is not defined in Archcode.";
								String classMethodName = className + "." + methodName;
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);

								String optMethodName = "opt[" + methodName + "]";
								System.out.println("ERROR: " + optMethodName + ": Not defined in Archcode.");
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
								String msg = "Alternative operation is not defined in Archcode.";
								String classMethodName = joinAltNames((AlternativeOperation) op, ",", className + ".{", "}");
								problemViewManager.createErrorMarker(diagramFile, msg, classMethodName);

								String altMethodName = joinAltNames((AlternativeOperation) op, " ", "alt{", "}");
								System.out.println("ERROR: " + altMethodName + ": Not defined in Archcode.");
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
