package jp.ac.kyushu.iarch.classdiagram.features;

import java.util.HashSet;
import java.util.Set;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.model.ComponentTypeCheckModel;
import jp.ac.kyushu.iarch.basefunction.model.ComponentTypeCheckModel.MethodModel;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;

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
		// Get Archface model within the project.
		IProject project = ProjectReader.getProject();
		if (project == null) {
			System.out.println("TypeCheckFeature: failed to get active project.");
			return;
		}
		IResource archfile = new XMLreader(project).getArchfileResource();
		ArchModel archModel = new ArchModel(archfile);
		Model model = archModel.getModel();

		doTypeCheck(model, getDiagram());
	}

	private void doTypeCheck(Model model, Diagram diagram) {
		// For each Class object in diagram
		// (Note that objects are assumed to be direct children of the root.)
		for (EObject obj : diagram.eResource().getContents()) {
			if (obj instanceof umlClass.Class) {
				umlClass.Class eClass = (umlClass.Class) obj;
				String className = eClass.getName();
				System.out.println("[Class " + className + "]");

				ComponentTypeCheckModel ctcModel =
						ComponentTypeCheckModel.getTypeCheckModel(model, className);
				if (ctcModel == null) {
					System.out.println("Not defined in Archcode.");
				} else {
					for (MethodModel mm : ctcModel.getMethodModels()) {
						String methodName = mm.getMethod().getName();
						if (mm.isCertain()) {
							// Should be defined as certain.
							Operation operation = findNonAltOperation(eClass, methodName);
							if (operation == null) {
								System.out.println("ERROR: " + methodName + ": Not defined in Class model.");
							} else if (operation instanceof OptionalOperation) {
								System.out.println("ERROR: " + methodName + ": Not defined as Certain.");
							} else {
								System.out.println("INFO: " + methodName + ": Defined as Certain.");
							}
						} else if (mm.isOptional()) {
							String optMethodName = "opt[" + methodName + "]";
							Operation operation = findNonAltOperation(eClass, methodName);
							if (operation == null) {
								System.out.println("ERROR: " + optMethodName + ": Not defined in Class model.");
							} else if (operation instanceof OptionalOperation) {
								System.out.println("INFO: " + optMethodName + ": Defined as Optional.");
							} else {
								System.out.println("ERROR: " + optMethodName + ": Not defined as Optional.");
							}
						}
					}

					for (AltMethod am : ctcModel.getAltMethods()) {
						StringBuilder altMethodNameSB = new StringBuilder("alt{");
						for (Method m : am.getMethods()) {
							altMethodNameSB.append(m.getName()).append(" ");
						}
						altMethodNameSB.deleteCharAt(altMethodNameSB.length() - 1).append("}");
						String altMethodName = altMethodNameSB.toString();

						boolean definedAsCertain = false;
						for (Method m : am.getMethods()) {
							String methodName = m.getName();
							Operation operation = findNonAltOperation(eClass, methodName);
							if (operation == null) {
								// NOP
							} else if (operation instanceof OptionalOperation) {
								// NOP but checked afterward
							} else {
								System.out.println("ERROR: " + altMethodName + ": Defined as Certain: " + methodName);
								definedAsCertain = true;
							}
						}

						if (!definedAsCertain) {
							HashSet<String> methodNames = new HashSet<String>();
							for (Method m : am.getMethods()) {
								methodNames.add(m.getName());
							}
							AlternativeOperation altOperation = findAltOperation(eClass, methodNames);
							if (altOperation == null) {
								System.out.println("ERROR: " + altMethodName + ": Not defined in Class model.");
							} else {
								System.out.println("INFO: " + altMethodName + ": Defined as Alternative.");
							}
						}
					}

					for (Operation op : eClass.getOwnedOperation()) {
						String methodName = op.getName();
						if (op instanceof OptionalOperation) {
							String optMethodName = "opt[" + op.getName() + "]";

							MethodModel mm = ctcModel.getMethodModel(methodName);
							if (mm == null) {
								System.out.println("ERROR: " + optMethodName + ": Not defined in Archcode.");
							}
						} else if (op instanceof AlternativeOperation) {
							HashSet<String> altMethodNames = new HashSet<String>();
							for (Operation childOp : ((AlternativeOperation) op).getOperations()) {
								altMethodNames.add(childOp.getName());
							}
							AltMethod am = ctcModel.getAltMethod(altMethodNames);
							if (am == null) {
								StringBuilder altMethodNameSB = new StringBuilder("alt{");
								for (Operation childOp : ((AlternativeOperation) op).getOperations()) {
									altMethodNameSB.append(childOp.getName()).append(" ");
								}
								altMethodNameSB.deleteCharAt(altMethodNameSB.length() - 1).append("}");
								String altMethodName = altMethodNameSB.toString();

								System.out.println("ERROR: " + altMethodName + ": Not defined in Archcode.");
							}
						} else {
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

	@Override
	public boolean hasDoneChanges() {
		// If results of type check modify objects, it should return true.
		return false;
	}
}
