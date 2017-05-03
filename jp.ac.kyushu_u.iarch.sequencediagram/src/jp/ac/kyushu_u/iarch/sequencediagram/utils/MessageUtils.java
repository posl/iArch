package jp.ac.kyushu_u.iarch.sequencediagram.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.ILinkService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import behavior.AlternativeMessage;
import behavior.BehaviorExecutionSpecification;
import behavior.BehaviorFactory;
import behavior.BehavioredClassifier;
import behavior.InteractionFragment;
import behavior.Lifeline;
import behavior.Message;
import behavior.MessageEnd;
import behavior.MessageOccurrenceSpecification;

public class MessageUtils {
	private static final Pattern methodNamePattern = Pattern.compile("[a-zA-Z_]\\w*");

	public static boolean validName(String name) {
		return methodNamePattern.matcher(name).matches();
	}

	public static String askString(String title, String message, String initialValue, IInputValidator validator) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		InputDialog inputDialog = new InputDialog(shell, title, message, initialValue, validator);
		if (inputDialog.open() == Window.OK) {
			return inputDialog.getValue();
		} else {
			return null;
		}
	}
	public static String askMessageName(String title, String message) {
		return askString(title, message, "", new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText.length() == 0) {
					return "";
				} else if (!validName(newText)) {
					return "Invalid message name.";
				}
				return null;
			}
		});
	}

	public static Message createElement(String name, boolean isArchpoint,
			Diagram diagram, Lifeline source, Lifeline target) {
		Message message = BehaviorFactory.eINSTANCE.createMessage();
		message.setArchpoint(isArchpoint);
		message.setName(name);
		diagram.eResource().getContents().add(message);
		createChildElements(message, diagram, source, target);
		return message;
	}

	public static void createChildElements(Message message,
			Diagram diagram, Lifeline source, Lifeline target) {
		BehaviorExecutionSpecification sourceBes =
				BehaviorFactory.eINSTANCE.createBehaviorExecutionSpecification();
		BehaviorExecutionSpecification targetBes =
				BehaviorFactory.eINSTANCE.createBehaviorExecutionSpecification();

		MessageOccurrenceSpecification startMos =
				BehaviorFactory.eINSTANCE.createMessageOccurrenceSpecification();
		MessageOccurrenceSpecification endMos =
				BehaviorFactory.eINSTANCE.createMessageOccurrenceSpecification();
		diagram.eResource().getContents().add(startMos);
		diagram.eResource().getContents().add(endMos);

		// Set references
		// MOS -> Message
		startMos.setMessage(message);
		endMos.setMessage(message);
		// Message -> MOSs
		message.setSendEvent(startMos);
		message.setReceiveEvent(endMos);
		// BES -> MOS
		sourceBes.setStart(startMos);
		targetBes.setStart(endMos);
		// BES -> Lifeline
		sourceBes.getCovered().add(source);
		targetBes.getCovered().add(target);
		// MOS -> Lifeline
		startMos.getCovered().add(source);
		endMos.getCovered().add(target);
		// Lifeline -> BES
		source.getBehaviorExecution().add(sourceBes);
		target.getBehaviorExecution().add(targetBes);
	}

	public static Shape createDummyShape(ContainerShape parent, ILocation location) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		Shape dummyShape = peCreateService.createShape(parent, false);
		Rectangle rect = gaService.createRectangle(dummyShape);
		gaService.setLocationAndSize(rect, location.getX(), location.getY(), 1, 1);
		return dummyShape;
	}

	public static Shape getBesShape(Lifeline lifeline, Diagram diagram) {
		ILinkService linkService = Graphiti.getLinkService();

		for (InteractionFragment iFragment: lifeline.getCoveredBy()) {
			if (iFragment instanceof BehaviorExecutionSpecification) {
				for (PictogramElement pe: linkService.getPictogramElements(diagram, iFragment)) {
					if (pe instanceof Shape) {
						GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
						if (ga instanceof Rectangle) {
							return (ContainerShape) pe;
						}
					}
				}
			}
		}
		return null;
	}

	public static Shape createBesShape(ContainerShape parent, int baseX, int baseY) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		// Create ContainerShape because Message's BES links to it.
		Shape shape = peCreateService.createContainerShape(parent, true);
		Rectangle rect = gaService.createRectangle(shape);
		gaService.setLocationAndSize(rect, baseX - 10, baseY, 21, 100);
		return shape;
	}

	public static Shape createMosShape(ContainerShape parent, int baseX, int baseY,
			Color fgColor, Color bgColor) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		// Create ContainerShape because Message's MOS links to it.
		Shape shape = peCreateService.createContainerShape(parent, true);
		Rectangle rect = gaService.createRectangle(shape);
		rect.setForeground(fgColor);
		rect.setBackground(bgColor);
		gaService.setLocationAndSize(rect, baseX, baseY, 10, 10);
		return shape;
	}

	public static Polyline createArrow(GraphicsAlgorithmContainer gaContainer,
			Color fgColor) {
		IGaService gaService = Graphiti.getGaService();

		Polyline polyline = gaService.createPolyline(gaContainer,
				new int[] { -10, 7, 0, 0, -10, -7 });
		polyline.setForeground(fgColor);
		polyline.setLineWidth(2);
		return polyline;
	}

	public static void setMessageOrders(Diagram diagram) {
		ILinkService linkService = Graphiti.getLinkService();

		// Collect shapes which have link with Message's start MOS.
		ArrayList<PictogramElement> shapes = new ArrayList<PictogramElement>();
		for (EObject eobj : diagram.eResource().getContents()) {
			if (eobj instanceof Message) {
				MessageEnd mos = ((Message) eobj).getSendEvent();
				if (mos instanceof MessageOccurrenceSpecification) {
					for (PictogramElement pe : linkService.getPictogramElements(diagram, mos)) {
						if (pe instanceof Shape) {
							shapes.add(pe);
							break;
						}
					}
				}
			}
		}

		// Sort them by Y coordinates.
		Collections.sort(shapes, new Comparator<PictogramElement>() {
			@Override
			public int compare(PictogramElement arg0, PictogramElement arg1) {
				return arg0.getGraphicsAlgorithm().getY() - arg1.getGraphicsAlgorithm().getY();
			}
		});

		// Set order.
		int order = 0;
		for (PictogramElement pe : shapes) {
			EObject eobj = linkService.getBusinessObjectForLinkedPictogramElement(pe);
			if (eobj instanceof MessageOccurrenceSpecification) {
				MessageOccurrenceSpecification mos = (MessageOccurrenceSpecification) eobj;
				mos.getMessage().setMessageOrder(order++);
				System.out.println("Message:" + String.valueOf(order - 1) + " " + mos.getMessage().getName());
			}
		}
	}

	/**
	 * Collect ordered Messages from diagram.
	 * Note that Messages are assumed to:<br>
	 * 1. be direct children of the root<br>
	 * 2. have their order numbers based upon Y coordinates (if not AlternativeMessage)
	 * @param diagram
	 * @return
	 */
	public static List<Message> collectMessages(Diagram diagram) {
		return collectMessages(diagram.eResource());
	}
	public static List<Message> collectMessages(Resource diagramResource) {
		// Collect all messages from diagram and sort them.
		TreeMap<Integer, Message> messageMap = new TreeMap<Integer, Message>();
		for (EObject obj : diagramResource.getContents()) {
			if (obj instanceof Message && !(obj instanceof AlternativeMessage)) {
				Message message = (Message) obj;
				messageMap.put(message.getMessageOrder(), message);
			}
		}

		// Remove Messages which are possessed by AlternativeMessage.
		for (EObject obj : diagramResource.getContents()) {
			if (obj instanceof AlternativeMessage) {
				AlternativeMessage altMessage = (AlternativeMessage) obj;
				int altOrder = 0;
				boolean first = true;
				for (Message message : altMessage.getMessages()) {
					int order = message.getMessageOrder();
					altOrder = first ? order : Math.min(altOrder, order);
					first = false;
					messageMap.remove(order);
				}
				messageMap.put(altOrder, altMessage);
			}
		}

		// Convert to list.
		ArrayList<Message> messages = new ArrayList<Message>();
		messages.addAll(messageMap.values());
		return messages;
	}

	/**
	 * Get "Object" which receives the specified message.
	 * @return Receiving Object(Class or Actor). it can be null.
	 */
	public static behavior.Object getReceivingObject(Message message) {
		MessageEnd recvEvent = message.getReceiveEvent();
		if (recvEvent instanceof MessageOccurrenceSpecification) {
			Lifeline lifeline =
					((MessageOccurrenceSpecification) recvEvent).getCovered().get(0);
			if (lifeline != null) {
				BehavioredClassifier actor = lifeline.getActor();
				if (actor instanceof behavior.Object) {
					return (behavior.Object) actor;
				}
			}
		}
		return null;
	}
}
