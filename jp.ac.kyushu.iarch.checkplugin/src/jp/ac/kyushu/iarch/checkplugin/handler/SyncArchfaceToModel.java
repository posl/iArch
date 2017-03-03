package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainBehavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainConnector;
import jp.ac.kyushu.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu.iarch.basefunction.controller.GraphitiModelManager;
import jp.ac.kyushu.iarch.basefunction.exception.ProjectNotFoundException;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu.iarch.basefunction.utils.MessageDialogUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.ArchModelUtils;
import jp.ac.kyushu.iarch.checkplugin.utils.DiagramUtils;
import jp.ac.kyushu.iarch.classdiagram.features.CreateAlternativeOperationFeature;
import jp.ac.kyushu.iarch.classdiagram.features.CreateClassFeature;
import jp.ac.kyushu.iarch.classdiagram.features.CreateOperationFeature;
import jp.ac.kyushu.iarch.classdiagram.features.CreateOptionalOperationFeature;
import jp.ac.kyushu.iarch.classdiagram.utils.ClassUtils;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.impl.CreateContext;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.ui.services.GraphitiUi;

public class SyncArchfaceToModel implements IHandler {

	private static final String HANDLER_TITLE = "Sync: iArch -> Model";

	private static final String CLASS_DIAGRAM_TYPE_ID = "ClassDiagram";
	private static final String CLASS_DIAGRAM_PRIVIDER_ID = "jp.ac.kyushu.iarch.classdiagram.diagram.DiagramTypeProvider";
	private static final String SEQUENCE_DIAGRAM_TYPE_ID = "SequenceDiagram";
	private static final String SEQUENCE_DIAGRAM_PROVIDER_ID = "jp.ac.kyushu.iarch.sequencediagram.diagram.SequenceDiagramTypeProvider";

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

		// Get Archface model.
		IResource archfile = new XMLreader(project).getArchfileResource();
		if (archfile == null) {
			MessageDialogUtils.showError(HANDLER_TITLE, "Failed to get the archfile resource.");
			return null;
		}
		ArchModel archModel = new ArchModel(archfile);
		Model model = archModel.getModel();

		generateDiagramFromModel(model, project);

		return null;
	}

	private void generateDiagramFromModel(Model model, IProject project) {
		IFolder folder = project.getFolder("diagrams-gen");
		if (!folder.exists()) {
			try {
				folder.create(false, true, null);
			} catch (CoreException e) {
				MessageDialogUtils.showError(HANDLER_TITLE, "Failed create a directory to generate codes.");
				return;
			}
		}

		// Interface(s) -> Class diagram
		generateClassDiagram(model, folder);
		// Connector(s) -> Sequence diagram(s)
		generateSequenceDiagram(model, folder);
	}

	/**
	 * Open diagram resource, or create if not exist.
	 * @param file
	 * @param diagramTypeId
	 * @param diagramName
	 * @return
	 */
	private Resource openDiagramResource(IFile file, String diagramTypeId, String diagramName) {
		Resource resource = null;
		Diagram diagram = null;
		if (file.exists()) {
			resource = GraphitiModelManager.getGraphitiModel(file);
			diagram = findDiagram(resource);
		} else {
			resource = GraphitiModelManager.createGraphitiModel(file);
		}
		if (diagram == null) {
			diagram = Graphiti.getPeCreateService().createDiagram(diagramTypeId, diagramName, true);
			resource.getContents().add(diagram);
		}
		return resource;
	}
	private Diagram findDiagram(Resource resource) {
		for (EObject eObj : resource.getContents()) {
			if (eObj instanceof Diagram) {
				return (Diagram) eObj;
			}
		}
		return null;
	}

	private void generateClassDiagram(Model model, IFolder folder) {
		if (model.getInterfaces().isEmpty() && model.getU_interfaces().isEmpty()) {
			return;
		}

		// Open a class diagram
		String diagramName = "GenClass";
		String diagramFilename = diagramName + ".diagram";
		IFile diagramFile = folder.getFile(diagramFilename);
		Resource resource = openDiagramResource(diagramFile, CLASS_DIAGRAM_TYPE_ID, diagramName);
		Diagram diagram = findDiagram(resource);
		boolean modified = false;

		IDiagramTypeProvider dtp = GraphitiUi.getExtensionManager()
				.createDiagramTypeProvider(diagram, CLASS_DIAGRAM_PRIVIDER_ID);
		IFeatureProvider fp = dtp.getFeatureProvider();

		int classX = 50;

		for (Interface cInterface : model.getInterfaces()) {
			String ifName = cInterface.getName();

			// Find or create Class component
			umlClass.Class uClass = findClass(resource, ifName);
			if (uClass == null) {
				createClass(diagram, fp, ifName, classX);
				uClass = findClass(resource, ifName);
				if (uClass == null) {
					continue;
				}
				classX += 200;
				modified = true;
			}
			ContainerShape classContainer = DiagramUtils.getLinkedContainerShape(diagram, uClass);
			if (classContainer == null) {
				continue;
			}

			for (Method method : cInterface.getMethods()) {
				// Find or create Operation component
				String methodName = method.getName();
				if (!ClassUtils.hasCertainName(uClass, methodName)) {
					createOperation(classContainer, fp, methodName);
					modified = true;
				}
			}
		}

		for (UncertainInterface uInterface : model.getU_interfaces()) {
			Interface cInterface = uInterface.getSuperInterface();
			if (cInterface != null) {
				String ifName = cInterface.getName();

				// Find or create Class component
				umlClass.Class uClass = findClass(resource, ifName);
				if (uClass == null) {
					createClass(diagram, fp, ifName, classX);
					uClass = findClass(resource, ifName);
					if (uClass == null) {
						continue;
					}
					classX += 200;
					modified = true;
				}
				ContainerShape classContainer = DiagramUtils.getLinkedContainerShape(diagram, uClass);
				if (classContainer == null) {
					continue;
				}

				for (OptMethod optMethod : uInterface.getOptmethods()) {
					// Create OptionalMethod component
					String methodName = optMethod.getMethod().getName();
					if (!ClassUtils.hasOptName(uClass, methodName)) {
						createOptionalOperation(classContainer, fp, methodName);
						modified = true;
					}
				}
				for (AltMethod altMethod : uInterface.getAltmethods()) {
					// Create AlternativeMethod component
					EList<Method> methods = altMethod.getMethods();
					StringBuilder sb = new StringBuilder();
					String[] methodNames = new String[methods.size()];
					for (int i = 0; i < methods.size(); ++i) {
						String methodName = methods.get(i).getName();
						if (i > 0) {
							sb.append(" ");
						}
						sb.append(methodName);
						methodNames[i] = methodName;
					}
					if (!ClassUtils.hasAltName(uClass, methodNames)) {
						createAlternativeOperation(classContainer, fp, sb.toString());
						modified = true;
					}
				}
			}
		}

		if (modified) {
			try {
				resource.save(null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private umlClass.Class findClass(Resource resource, String className) {
		for (EObject eObj : resource.getContents()) {
			if (eObj instanceof umlClass.Class) {
				umlClass.Class c = (umlClass.Class) eObj;
				if (className.equals(c.getName())) {
					return c;
				}
			}
		}
		return null;
	}
	private void createClass(ContainerShape container, IFeatureProvider fp, String className, int x) {
		CreateContext ctx = new CreateContext();
		ctx.setTargetContainer(container);
		// TODO: find appropriate position.
		ctx.setX(x);
		ctx.setY(50);
		ctx.setWidth(150);
		ctx.setHeight(50);
		ctx.putProperty("name", className);
		CreateClassFeature cf = new CreateClassFeature(fp);
		if (cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}

	private void createOperation(ContainerShape container, IFeatureProvider fp, String opName) {
		CreateContext ctx = new CreateContext();
		ctx.setTargetContainer(container);
		ctx.putProperty("name", opName);
		CreateOperationFeature cf = new CreateOperationFeature(fp);
		if (cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}
	private void createOptionalOperation(ContainerShape container, IFeatureProvider fp, String opName) {
		CreateContext ctx = new CreateContext();
		ctx.setTargetContainer(container);
		ctx.putProperty("name", opName);
		CreateOptionalOperationFeature cf = new CreateOptionalOperationFeature(fp);
		if (cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}
	private void createAlternativeOperation(ContainerShape container, IFeatureProvider fp, String opNames) {
		CreateContext ctx = new CreateContext();
		ctx.setTargetContainer(container);
		ctx.putProperty("name", opNames);
		CreateAlternativeOperationFeature cf = new CreateAlternativeOperationFeature(fp);
		if (cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}

	private void generateSequenceDiagram(Model model, IFolder folder) {
		int behaviorCount = 1;
		for (Behavior behavior : model.getBehaviors()) {
			String seqName = "_Sequence_" + (behaviorCount++);
			generateSequenceDiagram(behavior, folder, seqName);
		}

		for (Connector connector : model.getConnectors()) {
			String connName = connector.getName();
			behaviorCount = 1;
			for (Behavior behavior : connector.getBehaviors()) {
				String seqName = connName + "_" + (behaviorCount++);
				generateSequenceDiagram(behavior, folder, seqName);
			}
		}

		for (UncertainConnector uConnector : model.getU_connectors()) {
			String connName = uConnector.getName();
			behaviorCount = 1;
			for (UncertainBehavior uBehavior : uConnector.getU_behaviors()) {
				String seqName = connName + "_" + (behaviorCount++);
				generateSequenceDiagram(uBehavior, folder, seqName);
			}
		}
	}
	private void generateSequenceDiagram(Behavior behavior, IFolder folder, String seqName) {
		// Open a Sequence diagram

		List<String> classes = new ArrayList<String>();

		// Create Actor component
		Interface sInterface = behavior.getInterface();
		String sifName = sInterface.getName();
		classes.add(sifName);

		for (Method method : behavior.getCall()) {
			String ifName = ArchModelUtils.getClassName(method);
			if (!classes.contains(ifName)) {
				// Create Actor component
				classes.add(ifName);
			}

			// Create Message connection
			String methodName = method.getName();
			
		}

		// Save diagram
	}
	private void generateSequenceDiagram(UncertainBehavior uBehavior, IFolder folder, String seqName) {
		// Open a Sequence diagram

		List<String> classes = new ArrayList<String>();

		// Create Actor component
		String sifName = uBehavior.getName();
		classes.add(sifName);

		for (SuperCall superCall : uBehavior.getCall()) {
			if (superCall instanceof CertainCall) {
				Method method = ArchModelUtils.getMethodIfCertain(superCall);
				String ifName = ArchModelUtils.getClassName(method);

				if (!classes.contains(ifName)) {
					// Create Actor component
					classes.add(ifName);
				}

				// Create Message connection
				String methodName = method.getName();

			} else if (superCall instanceof OptCall) {
				SuperMethod superMethod = ((OptCall) superCall).getName();
				if (superMethod instanceof Method) {
					Method method = (Method) superMethod;
					String ifName = ArchModelUtils.getClassName(method);

					if (!classes.contains(ifName)) {
						// Create Actor component
						classes.add(ifName);
					}

					// Create OptionalMessage connection
					String methodName = method.getName();

				}
			} else if (superCall instanceof AltCall) {
				AltCall altCall = (AltCall) superCall;
				SuperMethod superMethod = altCall.getName();
				if (superMethod instanceof Method) {
					Method method = (Method) superMethod;
					String ifName = ArchModelUtils.getClassName(method);

					if (!classes.contains(ifName)) {
						// Create Actor component
						classes.add(ifName);
					}

					// Create AlternativeMessage connection
					List<String> methodNames = new ArrayList<String>();
					methodNames.add(method.getName());
					for (SuperMethod sm : altCall.getA_name()) {
						if (sm instanceof Method) {
							methodNames.add(((Method) sm).getName());
						}
					}

				}
			}
		}

		// Save diagram
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
