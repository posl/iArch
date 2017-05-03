package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.IOException;

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
import jp.ac.kyushu.iarch.sequencediagram.features.CreateActorFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateAlternativeMessageFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateLifelineFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateMessageFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateObjectFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateOptionalMessageFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.MoveDestructionEventFeature;

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
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.CreateContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.internal.datatypes.impl.LocationImpl;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.ui.services.GraphitiUi;

import behavior.Lifeline;

@SuppressWarnings("restriction")
public class SyncArchfaceToModel implements IHandler {

	private static final String HANDLER_TITLE = "Sync: iArch -> Model";

	private static final String CLASS_DIAGRAM_TYPE_ID = "ClassDiagram";
	private static final String CLASS_DIAGRAM_PROVIDER_ID = "jp.ac.kyushu.iarch.classdiagram.diagram.DiagramTypeProvider";
	private static final String SEQUENCE_DIAGRAM_TYPE_ID = "SequenceDiagram";
	private static final String SEQUENCE_DIAGRAM_PROVIDER_ID = "jp.ac.kyushu.iarch.sequencediagram.diagram.SequenceDiagramTypeProvider";

	private static final String CLASS_DIAGRAM_NAME = "Class";

	private class FindOrCreateResult<T> {
		public boolean created;
		public T result;
		private FindOrCreateResult(boolean created, T result) {
			this.created = created;
			this.result = result;
		}
	}

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
		IFile diagramFile = folder.getFile(CLASS_DIAGRAM_NAME + ".diagram");
		Resource resource = openDiagramResource(diagramFile, CLASS_DIAGRAM_TYPE_ID, CLASS_DIAGRAM_NAME);
		Diagram diagram = findDiagram(resource);

		IDiagramTypeProvider dtp = GraphitiUi.getExtensionManager()
				.createDiagramTypeProvider(diagram, CLASS_DIAGRAM_PROVIDER_ID);
		IFeatureProvider fp = dtp.getFeatureProvider();

		boolean modified = false;
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
					// TODO: find appropriate position.
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
			Connector connector = uConnector.getSuperInterface();
			String connName = connector != null ? connector.getName() : uConnector.getName();
			behaviorCount = 1;
			for (UncertainBehavior uBehavior : uConnector.getU_behaviors()) {
				String seqName = connName + "_u" + (behaviorCount++);
				generateSequenceDiagram(uBehavior, folder, seqName);
			}
		}
	}
	private void generateSequenceDiagram(Behavior behavior, IFolder folder, String seqName) {
		// Open a Sequence diagram
		Resource resource = openSequenceDiagramResource(folder, seqName);
		Diagram diagram = findDiagram(resource);

		IDiagramTypeProvider dtp = GraphitiUi.getExtensionManager()
				.createDiagramTypeProvider(diagram, SEQUENCE_DIAGRAM_PROVIDER_ID);
		IFeatureProvider fp = dtp.getFeatureProvider();

		boolean modified = false;
		int lifelineX = 50;
		int messageY = 130;
		Connection lastConnection = null;

		// Find or create Actor component
		FindOrCreateResult<behavior.Object> actor =
				findOrCreateActor(resource, diagram, fp, "Actor", lifelineX); 
		if (actor.result == null) {
			return;
		}
		if (actor.created) {
			lifelineX += 200;
			modified = true;
		}
		// Find or create Lifeline
		FindOrCreateResult<Lifeline> actorLifeline =
				findOrCreateLifeline(resource, diagram, fp, actor.result);
		if (actorLifeline.result == null) {
			return;
		}
		if (actorLifeline.created) {
			modified = true;
		}
		lastConnection = DiagramUtils.getLinkedConnection(diagram, actorLifeline.result);

		for (Method method : behavior.getCall()) {
			// Find or create Object component
			String ifName = ArchModelUtils.getClassName(method);
			FindOrCreateResult<behavior.Object> bObject =
					findOrCreateBObject(resource, diagram, fp, ifName, lifelineX); 
			if (bObject.result == null) {
				continue;
			}
			if (bObject.created) {
				lifelineX += 200;
				modified = true;
			}
			// Find or create Lifeline
			FindOrCreateResult<Lifeline> objectLifeline =
					findOrCreateLifeline(resource, diagram, fp, bObject.result);
			if (objectLifeline.result == null) {
				continue;
			}
			if (objectLifeline.created) {
				modified = true;
			}
			Connection connection = DiagramUtils.getLinkedConnection(diagram, objectLifeline.result);

			// Create Message connection
			String methodName = method.getName();
			// TODO: how to "synchronize"?
			createMessage(lastConnection, connection, fp, methodName, messageY);
			messageY += 80;
			modified = true;

			lastConnection = connection;
		}

		// Save diagram
		if (modified) {
			adjustLifelineEnd(diagram, lastConnection, fp, messageY);
			try {
				resource.save(null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private void generateSequenceDiagram(UncertainBehavior uBehavior, IFolder folder, String seqName) {
		// Open a Sequence diagram
		Resource resource = openSequenceDiagramResource(folder, seqName);
		Diagram diagram = findDiagram(resource);

		IDiagramTypeProvider dtp = GraphitiUi.getExtensionManager()
				.createDiagramTypeProvider(diagram, SEQUENCE_DIAGRAM_PROVIDER_ID);
		IFeatureProvider fp = dtp.getFeatureProvider();

		boolean modified = false;
		int lifelineX = 50;
		int messageY = 130;
		Connection lastConnection = null;

		// Create Actor component
		// Find or create Actor component
		FindOrCreateResult<behavior.Object> actor =
				findOrCreateActor(resource, diagram, fp, "Actor", lifelineX); 
		if (actor.result == null) {
			return;
		}
		if (actor.created) {
			lifelineX += 200;
			modified = true;
		}
		// Find or create Lifeline
		FindOrCreateResult<Lifeline> actorLifeline =
				findOrCreateLifeline(resource, diagram, fp, actor.result);
		if (actorLifeline.result == null) {
			return;
		}
		if (actorLifeline.created) {
			modified = true;
		}
		lastConnection = DiagramUtils.getLinkedConnection(diagram, actorLifeline.result);

		for (SuperCall superCall : uBehavior.getCall()) {
			Method method = null;
			String ifName = null;
			if (superCall instanceof CertainCall) {
				method = ArchModelUtils.getMethodIfCertain(superCall);
				ifName = ArchModelUtils.getClassName(method);
			} else if (superCall instanceof OptCall) {
				SuperMethod superMethod = ((OptCall) superCall).getName();
				if (superMethod instanceof Method) {
					method = (Method) superMethod;
					ifName = ArchModelUtils.getClassName(method);
				}
			} else if (superCall instanceof AltCall) {
				AltCall altCall = (AltCall) superCall;
				SuperMethod superMethod = altCall.getName();
				if (superMethod instanceof Method) {
					method = (Method) superMethod;
					ifName = ArchModelUtils.getClassName(method);
				}
			}
			if (ifName == null) {
				continue;
			}

			FindOrCreateResult<behavior.Object> bObject =
					findOrCreateBObject(resource, diagram, fp, ifName, lifelineX); 
			if (bObject.result == null) {
				continue;
			}
			if (bObject.created) {
				lifelineX += 200;
				modified = true;
			}
			// Find or create Lifeline
			FindOrCreateResult<Lifeline> objectLifeline =
					findOrCreateLifeline(resource, diagram, fp, bObject.result);
			if (objectLifeline.result == null) {
				continue;
			}
			if (objectLifeline.created) {
				modified = true;
			}
			Connection connection = DiagramUtils.getLinkedConnection(diagram, objectLifeline.result);

			if (superCall instanceof CertainCall) {
				// Create Message connection
				String methodName = method.getName();
				createMessage(lastConnection, connection, fp, methodName, messageY);
				messageY += 80;
				modified = true;

			} else if (superCall instanceof OptCall) {
				// Create OptionalMessage connection
				String methodName = method.getName();
				createOptionalMessage(lastConnection, connection, fp, methodName, messageY);
				messageY += 80;
				modified = true;

			} else if (superCall instanceof AltCall) {
				// Create AlternativeMessage connection
				StringBuilder sb = new StringBuilder();
				sb.append(method.getName());
				int addY = 80;
				for (SuperMethod sm : ((AltCall) superCall).getA_name()) {
					if (sm instanceof Method) {
						sb.append(" ").append(((Method) sm).getName());
						addY += 40;
					}
				}
				createAlternativeMessage(lastConnection, connection, fp, sb.toString(), messageY);
				messageY += addY;
				modified = true;

			}

			lastConnection = connection;
		}

		// Save diagram
		if (modified) {
			adjustLifelineEnd(diagram, lastConnection, fp, messageY);
			try {
				resource.save(null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private Resource openSequenceDiagramResource(IFolder folder, String seqName) {
		String diagramFilename = seqName + ".diagram";
		IFile diagramFile = folder.getFile(diagramFilename);
		// Delete old contents.(for now)
		// TODO: find a way to "synchronize".
		if (diagramFile.exists()) {
			try {
				diagramFile.delete(true, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return openDiagramResource(diagramFile, SEQUENCE_DIAGRAM_TYPE_ID, seqName);
	}

	private FindOrCreateResult<behavior.Object> findOrCreateActor(Resource resource, ContainerShape container,
			IFeatureProvider fp, String className, int x) {
		behavior.Object actor = findBObject(resource, className);
		if (actor != null) {
			return new FindOrCreateResult<behavior.Object>(false, actor);
		} else {
			// TODO: find appropriate position.
			createActor(container, fp, className, x);
			actor = findBObject(resource, className);
			if (actor == null) {
				return new FindOrCreateResult<behavior.Object>(false, null);
			} else {
				return new FindOrCreateResult<behavior.Object>(true, actor);
			}
		}
	}
	private FindOrCreateResult<behavior.Object> findOrCreateBObject(Resource resource, ContainerShape container,
			IFeatureProvider fp, String className, int x) {
		behavior.Object actor = findBObject(resource, className);
		if (actor != null) {
			return new FindOrCreateResult<behavior.Object>(false, actor);
		} else {
			// TODO: find appropriate position.
			createBObject(container, fp, className, x);
			actor = findBObject(resource, className);
			if (actor == null) {
				return new FindOrCreateResult<behavior.Object>(false, null);
			} else {
				return new FindOrCreateResult<behavior.Object>(true, actor);
			}
		}
	}
	// It can find an Actor because Actor extends Object.
	private behavior.Object findBObject(Resource resource, String className) {
		for (EObject eObj : resource.getContents()) {
			if (eObj instanceof behavior.Object) {
				behavior.Object bObject = (behavior.Object) eObj;
				if (className.equals(bObject.getName())) {
					return bObject;
				}
			}
		}
		return null;
	}
	private void createActor(ContainerShape container, IFeatureProvider fp, String className, int x) {
		CreateContext ctx = new CreateContext();
		ctx.setTargetContainer(container);
		// TODO: find appropriate position.
		ctx.setX(x);
		ctx.putProperty("name", className);
		CreateActorFeature cf = new CreateActorFeature(fp);
		if (cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}
	private void createBObject(ContainerShape container, IFeatureProvider fp, String className, int x) {
		CreateContext ctx = new CreateContext();
		ctx.setTargetContainer(container);
		// TODO: find appropriate position.
		ctx.setX(x);
		ctx.putProperty("name", className);
		CreateObjectFeature cf = new CreateObjectFeature(fp);
		if (cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}

	private FindOrCreateResult<Lifeline> findOrCreateLifeline(Resource resource, Diagram diagram,
			IFeatureProvider fp, behavior.Object bObject) {
		Lifeline lifeline = findLifeline(resource, bObject);
		if (lifeline != null) {
			return new FindOrCreateResult<Lifeline>(false, lifeline);
		} else {
			ContainerShape container = DiagramUtils.getLinkedContainerShape(diagram, bObject);
			createLifeline(container, fp);
			// TODO: set appropriate height.
			lifeline = findLifeline(resource, bObject);
			if (lifeline == null) {
				return new FindOrCreateResult<Lifeline>(false, null);
			} else {
				return new FindOrCreateResult<Lifeline>(true, lifeline);
			}
		}
	}
	private Lifeline findLifeline(Resource resource, behavior.Object bObject) {
		for (EObject eObj : resource.getContents()) {
			if (eObj instanceof Lifeline) {
				Lifeline lifeline = (Lifeline) eObj;
				if (lifeline.getActor() == bObject) {
					return lifeline;
				}
			}
		}
		return null;
	}
	private void createLifeline(ContainerShape container, IFeatureProvider fp) {
		CreateConnectionContext ctx = new CreateConnectionContext();
		ctx.setSourcePictogramElement(container);
		if (container.getAnchors().isEmpty()) {
			IPeCreateService peCreateService = Graphiti.getPeCreateService();
			ctx.setSourceAnchor(peCreateService.createChopboxAnchor(container));
		} else {
			ctx.setSourceAnchor(container.getAnchors().get(0));
		}
		CreateLifelineFeature cf = new CreateLifelineFeature(fp);
		if (cf.canStartConnection(ctx) && cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}

	private void createMessage(Connection source, Connection target, IFeatureProvider fp,
			String messageName, int y) {
		CreateConnectionContext ctx = new CreateConnectionContext();
		ctx.setSourcePictogramElement(source);
		ctx.setTargetPictogramElement(target);
		ctx.setSourceLocation(new LocationImpl(0, y));
		ctx.putProperty("name", messageName);
		CreateMessageFeature cf = new CreateMessageFeature(fp);
		if (cf.canStartConnection(ctx) && cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}
	private void createOptionalMessage(Connection source, Connection target, IFeatureProvider fp,
			String messageName, int y) {
		CreateConnectionContext ctx = new CreateConnectionContext();
		ctx.setSourcePictogramElement(source);
		ctx.setTargetPictogramElement(target);
		ctx.setSourceLocation(new LocationImpl(0, y));
		ctx.setTargetLocation(new LocationImpl(0, y));
		ctx.putProperty("name", messageName);
		CreateOptionalMessageFeature cf = new CreateOptionalMessageFeature(fp);
		if (cf.canStartConnection(ctx) && cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}
	private void createAlternativeMessage(Connection source, Connection target, IFeatureProvider fp,
			String messageNames, int y) {
		CreateConnectionContext ctx = new CreateConnectionContext();
		ctx.setSourcePictogramElement(source);
		ctx.setTargetPictogramElement(target);
		ctx.setSourceLocation(new LocationImpl(0, y));
		ctx.setTargetLocation(new LocationImpl(0, y));
		ctx.putProperty("name", messageNames);
		CreateAlternativeMessageFeature cf = new CreateAlternativeMessageFeature(fp);
		if (cf.canStartConnection(ctx) && cf.canCreate(ctx) && cf.canExecute(ctx)) {
			cf.execute(ctx);
		}
	}

	private void adjustLifelineEnd(Diagram diagram, Connection connection, IFeatureProvider fp, int y) {
		Anchor endAnchor = connection.getEnd();
		AnchorContainer container = endAnchor.getParent();
		if (container instanceof Shape) {
			int endY = container.getGraphicsAlgorithm().getY();
			if (endY < y) {
				MoveShapeContext ctx = new MoveShapeContext((Shape) container);
				ctx.setSourceContainer(diagram);
				ctx.setTargetContainer(diagram);
				ctx.setDeltaX(0);
				ctx.setDeltaY(y - endY);
				MoveDestructionEventFeature mf = new MoveDestructionEventFeature(fp);
				if (mf.canMoveShape(ctx) && mf.canExecute(ctx)) {
					mf.execute(ctx);
				}
			}
		}
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
