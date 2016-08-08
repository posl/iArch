package jp.ac.kyushu.iarch.checkplugin.utils;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.classdiagram.features.CreateAlternativeOperationFeature;
import jp.ac.kyushu.iarch.classdiagram.features.CreateOperationFeature;
import jp.ac.kyushu.iarch.classdiagram.features.CreateOptionalOperationFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateAlternativeMessageFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateMessageFeature;
import jp.ac.kyushu.iarch.sequencediagram.features.CreateOptionalMessageFeature;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IDeleteFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.impl.CreateContext;
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.DeleteContext;
import org.eclipse.graphiti.features.context.impl.MultiDeleteInfo;
import org.eclipse.graphiti.internal.datatypes.impl.LocationImpl;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.platform.IDiagramBehavior;
import org.eclipse.graphiti.platform.IDiagramContainer;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.ILinkService;
import org.eclipse.graphiti.ui.platform.GraphitiConnectionEditPart;
import org.eclipse.graphiti.ui.platform.GraphitiShapeEditPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import umlClass.AlternativeOperation;
import umlClass.Operation;
import umlClass.OptionalOperation;
import behavior.Actor;
import behavior.AlternativeMessage;
import behavior.BehavioredClassifier;
import behavior.Lifeline;
import behavior.Message;
import behavior.MessageEnd;
import behavior.MessageOccurrenceSpecification;
import behavior.OptionalMessage;

@SuppressWarnings("restriction")
public class DiagramUtils {
	public static EObject getBusinessObject(ISelection selection) {
		PictogramElement pictogramElement = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection stSelection = (IStructuredSelection) selection;
			if (stSelection.size() == 1) {
				Object selected = stSelection.getFirstElement();
				if (selected instanceof GraphitiShapeEditPart) {
					pictogramElement = ((GraphitiShapeEditPart) selected).getPictogramElement();
				} else if (selected instanceof GraphitiConnectionEditPart) {
					pictogramElement = ((GraphitiConnectionEditPart) selected).getPictogramElement();
				}
			}
		}

		if (pictogramElement != null) {
			ILinkService linkService = Graphiti.getLinkService();
			return linkService.getBusinessObjectForLinkedPictogramElement(pictogramElement);
		} else {
			return null;
		}
	}

	/**
	 * If given message is a child of AlternativeMessage, returns the parent.
	 * Otherwise returns given message (unless Resource is not found).
	 */
	// Assume that all Messages are direct childlen of diagram Resource.
	public static Message getTargetMessage(Message message) {
		Resource resource = message.eResource();
		if (resource == null) {
			return null;
		}

		for (EObject eObj : resource.getContents()) {
			if (eObj instanceof AlternativeMessage) {
				AlternativeMessage am = (AlternativeMessage) eObj;
				for (Message m : am.getMessages()) {
					if (m == message) {
						return am;
					}
				}
			}
		}
		return message;
	}

	// Assume that all Messages are direct childlen of diagram Resource.
	public static Message getCaller(Message message) {
		Resource resource = message.eResource();
		if (resource == null) {
			return null;
		}

		int baseOrder = message.getMessageOrder();
		AlternativeMessage am = null;
		if (message instanceof AlternativeMessage) {
			am = (AlternativeMessage) message;
			baseOrder = am.getMessages().get(0).getMessageOrder();
		}

		// Find previous Message
		int prevOrder = -1;
		Message prevMessage = null;
		for (EObject eObj : resource.getContents()) {
			if (eObj instanceof Message) {
				Message m = (Message) eObj;
				int order = m.getMessageOrder();
				if (prevOrder < order && order < baseOrder) {
					boolean canSet = true;
					// Skip if Message is child of given AlternativeMessage
					if (am != null) {
						for (Message child : am.getMessages()) {
							if (child == m) {
								canSet = false;
								break;
							}
						}
					}
					if (canSet) {
						prevOrder = order;
						prevMessage = m;
					}
				}
			}
		}

		return prevMessage != null ? getTargetMessage(prevMessage) : null;
	}

	// TODO: Based upon functions from sequencediagram project. Should integrate later.
	private static Lifeline getLifeline(MessageEnd messageEnd) {
		if (messageEnd instanceof MessageOccurrenceSpecification) {
			return ((MessageOccurrenceSpecification) messageEnd).getCovered().get(0);
		}
		return null;
	}
	private static behavior.Object getReceiveBehaviorObject(Message message) {
		Lifeline lifeline = getLifeline(message.getReceiveEvent());
		if (lifeline != null) {
			BehavioredClassifier actor = lifeline.getActor();
			if (actor instanceof behavior.Object) {
				return (behavior.Object) actor;
			}
		}
		return null;
	}

	public static String getMessageClassName(Message message) {
		if (message instanceof AlternativeMessage) {
			return getMessageClassName(((AlternativeMessage) message).getMessages().get(0));
		}
		behavior.Object obj = getReceiveBehaviorObject(message);
		return obj != null ? obj.getName() : null;
	}

	private static IDiagramContainer getDiagramContainer() {
		IWorkbench wb = PlatformUI.getWorkbench();
		if (wb != null) {
			IWorkbenchWindow wbw = wb.getActiveWorkbenchWindow();
			if (wbw != null) {
				IWorkbenchPage wbp = wbw.getActivePage();
				if (wbp != null) {
					IEditorPart editor = wbp.getActiveEditor();
					if (editor instanceof IDiagramContainer) {
						return (IDiagramContainer) editor;
					}
				}
			}
		}
		return null;
	}

	private static Shape getLinkedShape(Diagram diagram, EObject eObj) {
		for (PictogramElement pe :
			Graphiti.getLinkService().getPictogramElements(diagram, eObj)) {
			if (pe instanceof Shape) {
				return (Shape) pe;
			}
		}
		return null;
	}
	private static Connection getLinkedConnection(Diagram diagram, EObject eObj) {
		for (PictogramElement pe :
			Graphiti.getLinkService().getPictogramElements(diagram, eObj)) {
			if (pe instanceof Connection) {
				return (Connection) pe;
			}
		}
		return null;
	}

	private static class ModifyDiagramOperation {
		private IDiagramContainer container;
		private IFeatureProvider featureProvider;
		private IDiagramBehavior diagramBehavior;
		private Operation operation;
		private String name;

		private ModifyDiagramOperation(Operation operation, String name) {
			container = getDiagramContainer();
			featureProvider = container.getDiagramTypeProvider().getFeatureProvider();
			diagramBehavior = container.getDiagramBehavior();
			this.operation = operation;
			this.name = name;
		}

		public boolean execute() {
			if (container == null) {
				return false;
			}

			// Get PictogramElement
			Diagram diagram = container.getDiagramTypeProvider().getDiagram();
			Shape operationShape = getLinkedShape(diagram, operation);
			if (operationShape == null) {
				return false;
			}
			ContainerShape classContainer = operationShape.getContainer();
			if (classContainer == null) {
				return false;
			}

			// Invoke delete/create features
			delete(operationShape);
			create(classContainer);

			return true;
		}

		private void delete(Shape operationShape) {
			DeleteContext context = new DeleteContext(operationShape);
			context.setMultiDeleteInfo(new MultiDeleteInfo(false, false, 1));
			IDeleteFeature feature = featureProvider.getDeleteFeature(context);
			diagramBehavior.executeFeature(feature, context);
		}

		private void create(ContainerShape classContainer) {
			ICreateFeature feature = getCreateFeature(featureProvider);
			if (feature != null) {
				CreateContext context = new CreateContext();
				context.setTargetContainer(classContainer);
				if (name != null) {
					context.putProperty("name", name);
				}
				diagramBehavior.executeFeature(feature, context);
			}
		}
		protected ICreateFeature getCreateFeature(IFeatureProvider featureProvider) {
			return null;
		}
	}
	private static class ModifyDiagramOperationToCertain extends ModifyDiagramOperation {
		private ModifyDiagramOperationToCertain(Operation operation, String name) {
			super(operation, name);
		}
		@Override
		protected ICreateFeature getCreateFeature(IFeatureProvider featureProvider) {
			return new CreateOperationFeature(featureProvider);
		}
	};
	private static class ModifyDiagramOperationToOptional extends ModifyDiagramOperation {
		private ModifyDiagramOperationToOptional(Operation operation, String name) {
			super(operation, name);
		}
		@Override
		protected ICreateFeature getCreateFeature(IFeatureProvider featureProvider) {
			return new CreateOptionalOperationFeature(featureProvider);
		}
	}
	private static class ModifyDiagramOperationToAlternative extends ModifyDiagramOperation {
		private ModifyDiagramOperationToAlternative(Operation operation, String name) {
			super(operation, name);
		}
		@Override
		protected ICreateFeature getCreateFeature(IFeatureProvider featureProvider) {
			return new CreateAlternativeOperationFeature(featureProvider);
		}
	}

	public static boolean changeOperationToOptional(Operation operation) {
		String operationName = operation.getName();
		return new ModifyDiagramOperationToOptional(operation, operationName).execute();
	}

	public static boolean changeOperationToAlternative(Operation operation,
			List<String> alternatives) {
		StringBuilder sb = new StringBuilder();
		for (String name : alternatives) {
			sb.append(name).append(" ");
		}
		String altNames = sb.deleteCharAt(sb.length() - 1).toString();
		return new ModifyDiagramOperationToAlternative(operation, altNames).execute();
	}

	public static boolean changeOperationToNecessary(Operation operation,
			String necessaryMethodName) {
		return new ModifyDiagramOperationToCertain(operation, necessaryMethodName).execute();
	}

	public static boolean changeOperationToUnnecessary(Operation operation,
			String unnecessaryMethodName) {
		if (operation instanceof OptionalOperation) {
			// Delete but not create.
			return new ModifyDiagramOperation(operation, null).execute();

		} else if (operation instanceof AlternativeOperation) {
			ArrayList<String> alternatives = new ArrayList<String>();
			for (Operation op : ((AlternativeOperation) operation).getOperations()) {
				String name = op.getName();
				if (!unnecessaryMethodName.equals(name)) {
					alternatives.add(name);
				}
			}

			if (alternatives.size() >= 2) {
				// Change to AlternativeOperation.
				StringBuilder sb = new StringBuilder();
				for (String name : alternatives) {
					sb.append(name).append(" ");
				}
				String altNames = sb.deleteCharAt(sb.length() - 1).toString();
				return new ModifyDiagramOperationToAlternative(operation, altNames).execute();
			} else if (alternatives.size() == 1) {
				// Change to certain Operation.
				String methodName = alternatives.get(0);
				return new ModifyDiagramOperationToCertain(operation, methodName).execute();
			} else {
				// Delete but not create (this is the case which should not happen).
				return new ModifyDiagramOperation(operation, null).execute();
			}
		}
		return false;
	}

	// TODO: Based upon functions from sequencediagram project. Should integrate later. 
	private static int getLocationX(Lifeline lifeline, Diagram diagram, int defaultValue) {
		ILinkService linkService = Graphiti.getLinkService();

		BehavioredClassifier bc = lifeline.getActor();
		if (bc instanceof Actor || bc instanceof behavior.Object) {
			for (PictogramElement pe: linkService.getPictogramElements(diagram, bc)) {
				GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
				if (ga instanceof Rectangle) {
					return ga.getX() + ga.getWidth() / 2;
				}
			}
		}
		return defaultValue;
	}

	private static class ModifyDiagramMessage {
		private IDiagramContainer container;
		private IFeatureProvider featureProvider;
		private IDiagramBehavior diagramBehavior;
		private Message message;
		private String name;

		private ModifyDiagramMessage(Message message, String name) {
			container = getDiagramContainer();
			featureProvider = container.getDiagramTypeProvider().getFeatureProvider();
			diagramBehavior = container.getDiagramBehavior();
			this.message = message;
			this.name = name;
		}

		public boolean execute() {
			if (container == null) {
				return false;
			}

			// Get PictogramElement
			Diagram diagram = container.getDiagramTypeProvider().getDiagram();
			ArrayList<PictogramElement> deletePes = new ArrayList<PictogramElement>();

			Shape fragmentShape = getLinkedShape(diagram, message);
			if (fragmentShape != null) {
				deletePes.add(fragmentShape);
			}
			if (message instanceof AlternativeMessage) {
				for (Message m : ((AlternativeMessage) message).getMessages()) {
					pushDeleteElements(deletePes, diagram, m);
				}
			} else {
				pushDeleteElements(deletePes, diagram, message);
			}

			Message connectionMessage = message;
			if (message instanceof AlternativeMessage) {
				connectionMessage = ((AlternativeMessage) message).getMessages().get(0);
			}
			Lifeline sourceLifeline = getLifeline(connectionMessage.getSendEvent());
			Lifeline targetLifeline = getLifeline(connectionMessage.getReceiveEvent());
			if (sourceLifeline == null || targetLifeline == null) {
				return false;
			}
			Connection sourceConnection = getLinkedConnection(diagram, sourceLifeline);
			Connection targetConnection = getLinkedConnection(diagram, targetLifeline);
			if (sourceConnection == null || targetConnection == null) {
				return false;
			}

			Shape startMos = getLinkedShape(diagram, connectionMessage.getSendEvent());
			if (startMos == null) {
				return false;
			}
			int baseY = startMos.getGraphicsAlgorithm().getY();
			int sourceX = getLocationX(sourceLifeline, diagram, 0);
			int targetX = getLocationX(sourceLifeline, diagram, 0);
			LocationImpl sourceLocation = new LocationImpl(sourceX, baseY);
			LocationImpl targetLocation = new LocationImpl(targetX, baseY);

			// Invoke delete/create features
			delete(deletePes);
			create(sourceConnection, sourceLocation, targetConnection, targetLocation);

			return true;
		}

		private void pushDeleteElements(List<PictogramElement> pes, Diagram diagram, Message m) {
			Connection connection = getLinkedConnection(diagram, m);
			if (connection != null) {
				pes.add(connection);
			}
			// Delete MOSs too.
			Shape startMos = getLinkedShape(diagram, m.getSendEvent());
			pes.add(startMos);
			Shape endMos = getLinkedShape(diagram, m.getReceiveEvent());
			pes.add(endMos);
		}

		private void delete(List<PictogramElement> pes) {
			MultiDeleteInfo deleteInfo = new MultiDeleteInfo(false, false, 1);
			for (PictogramElement pe : pes) {
				DeleteContext context = new DeleteContext(pe);
				context.setMultiDeleteInfo(deleteInfo);
				IDeleteFeature feature = featureProvider.getDeleteFeature(context);
				diagramBehavior.executeFeature(feature, context);
			}
		}

		private void create(Connection source, ILocation sourceLocation,
				Connection target, ILocation targetLocation) {
			ICreateConnectionFeature feature = getCreateConnectionFeature(featureProvider);
			if (feature != null) {
				CreateConnectionContext context = new CreateConnectionContext();
				context.setSourcePictogramElement(source);
				context.setSourceLocation(sourceLocation);
				context.setTargetPictogramElement(target);
				context.setTargetLocation(targetLocation);
				if (name != null) {
					context.putProperty("name", name);
				}
				diagramBehavior.executeFeature(feature, context);
			}
		}
		protected ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider featureProvider) {
			return null;
		}
	}
	private static class ModifyDiagramMessageToCertain extends ModifyDiagramMessage {
		private ModifyDiagramMessageToCertain(Message message, String name) {
			super(message, name);
		}
		@Override
		protected ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider featureProvider) {
			return new CreateMessageFeature(featureProvider);
		}
	}
	private static class ModifyDiagramMessageToOptional extends ModifyDiagramMessage {
		private ModifyDiagramMessageToOptional(Message message, String name) {
			super(message, name);
		}
		@Override
		protected ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider featureProvider) {
			return new CreateOptionalMessageFeature(featureProvider);
		}
	}
	private static class ModifyDiagramMessageToAlternative extends ModifyDiagramMessage {
		private ModifyDiagramMessageToAlternative(Message message, String name) {
			super(message, name);
		}
		@Override
		protected ICreateConnectionFeature getCreateConnectionFeature(IFeatureProvider featureProvider) {
			return new CreateAlternativeMessageFeature(featureProvider);
		}
	}

	public static boolean changeMessageToOptional(Message message) {
		String messageName = message.getName();
		return new ModifyDiagramMessageToOptional(message, messageName).execute();
	}

	public static boolean changeMessageToAlternative(Message message,
			List<String> alternatives) {
		StringBuilder sb = new StringBuilder();
		for (String name : alternatives) {
			sb.append(name).append(" ");
		}
		String altNames = sb.deleteCharAt(sb.length() - 1).toString();
		return new ModifyDiagramMessageToAlternative(message, altNames).execute();
	}

	public static boolean changeMessageToNecessary(Message message,
			String necessaryMethodName) {
		return new ModifyDiagramMessageToCertain(message, necessaryMethodName).execute();
	}

	public static boolean changeMessageToUnnecessary(Message message,
			String unnecessaryMethodName) {
		if (message instanceof OptionalMessage) {
			// Delete but not create.
			return new ModifyDiagramMessage(message, null).execute();

		} else if (message instanceof AlternativeMessage) {
			ArrayList<String> alternatives = new ArrayList<String>();
			for (Message m : ((AlternativeMessage) message).getMessages()) {
				String name = m.getName();
				if (!unnecessaryMethodName.equals(name)) {
					alternatives.add(name);
				}
			}

			if (alternatives.size() >= 2) {
				// Change to AlternativeMessage.
				StringBuilder sb = new StringBuilder();
				for (String name : alternatives) {
					sb.append(name).append(" ");
				}
				String altNames = sb.deleteCharAt(sb.length() - 1).toString();
				return new ModifyDiagramMessageToAlternative(message, altNames).execute();
			} else if (alternatives.size() == 1) {
				// Change to certain Message.
				String methodName = alternatives.get(0);
				return new ModifyDiagramMessageToCertain(message, methodName).execute();
			} else {
				// Delete but not create (this is the case which should not happen).
				return new ModifyDiagramMessage(message, null).execute();
			}
		}
		return false;
	}
}
