package jp.ac.kyushu_u.iarch.sequencediagram.features;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.algorithms.styles.LineStyle;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.algorithms.styles.Point;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;

import behavior.AlternativeMessage;
import behavior.BehaviorExecutionSpecification;
import behavior.Lifeline;
import behavior.Message;
import behavior.MessageOccurrenceSpecification;
import jp.ac.kyushu_u.iarch.sequencediagram.utils.LifelineUtils;
import jp.ac.kyushu_u.iarch.sequencediagram.utils.MessageUtils;

public class AddAlternativeMessageFeature extends AbstractAddFeature {
	private static final IColorConstant MESSAGE_FOREGROUND = new ColorConstant(0, 0, 0);
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant MOS_FOREGROUND = MESSAGE_FOREGROUND;
	private static final IColorConstant MOS_BACKGROUND = MESSAGE_FOREGROUND;

	public AddAlternativeMessageFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context instanceof IAddConnectionContext
				&& context.getNewObject() instanceof AlternativeMessage) {
			return true;
		}
		return false;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		IAddConnectionContext connectionContext = (IAddConnectionContext) context;
		AlternativeMessage addedMessage = (AlternativeMessage) context.getNewObject();
		Diagram diagram = getDiagram();

		GraphicsAlgorithm sourceGa = connectionContext.getSourceAnchor().getParent().getGraphicsAlgorithm();
		int locationX = sourceGa.getX();
		int locationY = sourceGa.getY();

		ArrayList<Connection> connections = new ArrayList<Connection>();
		for (Message childMessage : addedMessage.getMessages()) {
			FreeFormConnection ffConnection = createMessageConnection(childMessage,
					locationX, locationY);
			connections.add(ffConnection);
			locationY += 40;
		}

		// Add combined fragment.
		ContainerShape altFragment = createAlternativeFragment(diagram, connections);
		link(altFragment, addedMessage);

		MessageUtils.setMessageOrders(diagram);

		// It must returns Connection object.
		return connections.get(0);
	}

	private FreeFormConnection createMessageConnection(Message message,
			int locationX, int locationY) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		// Get other business objects.
		MessageOccurrenceSpecification startMos =
				(MessageOccurrenceSpecification) message.getSendEvent();
		MessageOccurrenceSpecification endMos =
				(MessageOccurrenceSpecification) message.getReceiveEvent();
		Lifeline source = startMos.getCovered().get(0);
		Lifeline target = endMos.getCovered().get(0);
		BehaviorExecutionSpecification sourceBes = source.getBehaviorExecution().get(0);
		BehaviorExecutionSpecification targetBes = target.getBehaviorExecution().get(0);

		// Check the Actor or Object to set the location of
		// BehaviorExecutionSpecification
		Diagram diagram = getDiagram();
		int sourceLocationX = LifelineUtils.getLocationX(source, diagram, 0);
		int targetLocationX = LifelineUtils.getLocationX(target, diagram, 0);

		// Check whether there have been a Rectangle stand for each
		// BehaviorExecutionSpecification
		Shape sourceBesShape = MessageUtils.getBesShape(source, diagram);
		Shape targetBesShape = MessageUtils.getBesShape(target, diagram);
		if (sourceBesShape == null) {
			sourceBesShape = MessageUtils.createBesShape(diagram, sourceLocationX, locationY);
		}
		if (targetBesShape == null) {
			targetBesShape = MessageUtils.createBesShape(diagram, targetLocationX, locationY);
		}
		// link ContainerShape to MessageEnd
		link(sourceBesShape, sourceBes);
		link(targetBesShape, targetBes);

		// Check whether the message is from left to right,
		// if not, then change the way to count location of each
		// MessageOccurreceSpecification
		Shape startMosShape = null;
		Shape endMosShape = null;
		Color mosFgColor = manageColor(MOS_FOREGROUND);
		Color mosBgColor = manageColor(MOS_BACKGROUND);
		if (sourceLocationX < targetLocationX) {
			startMosShape = MessageUtils.createMosShape(diagram,
					sourceLocationX, locationY, mosFgColor, mosBgColor);
			endMosShape = MessageUtils.createMosShape(diagram,
					targetLocationX - 10, locationY, mosFgColor, mosBgColor);
		} else if (sourceLocationX == targetLocationX) {
			startMosShape = MessageUtils.createMosShape(diagram,
					sourceLocationX, locationY, mosFgColor, mosBgColor);
			endMosShape = MessageUtils.createMosShape(diagram,
					targetLocationX, locationY + 20, mosFgColor, mosBgColor);
		} else {
			startMosShape = MessageUtils.createMosShape(diagram,
					sourceLocationX - 10, locationY, mosFgColor, mosBgColor);
			endMosShape = MessageUtils.createMosShape(diagram,
					targetLocationX, locationY, mosFgColor, mosBgColor);
		}
		link(startMosShape, startMos);
		link(endMosShape, endMos);

		// CONNECTION WITH POLYLINE
		Anchor sourceAnchor = peCreateService.createChopboxAnchor(startMosShape);
		Anchor targetAnchor = peCreateService.createChopboxAnchor(endMosShape);
		FreeFormConnection ffConnection = peCreateService.createFreeFormConnection(diagram);
		ffConnection.setStart(sourceAnchor);
		ffConnection.setEnd(targetAnchor);

		GraphicsAlgorithm startMosShapeGa = startMosShape.getGraphicsAlgorithm();
		GraphicsAlgorithm endMosShapeGa = endMosShape.getGraphicsAlgorithm();
		int[] polylinePoints = new int[] {
				startMosShapeGa.getX() + startMosShapeGa.getWidth() / 2,
				startMosShapeGa.getY() + startMosShapeGa.getHeight() / 2,
				endMosShapeGa.getX() + endMosShapeGa.getWidth() / 2,
				endMosShapeGa.getY() + endMosShapeGa.getHeight() / 2 };

		Polyline polyline = gaService.createPolyline(ffConnection, polylinePoints);
		polyline.setLineWidth(2);
		polyline.setForeground(manageColor(MESSAGE_FOREGROUND));
		polyline.setFilled(true);
		polyline.setX(locationX);
		// If in the same lifeline, add two bend points.
		if (source == target) {
			// Get the position
			Point bendPoint1 = gaService.createPoint(startMosShapeGa.getX() + 30,
					startMosShapeGa.getY() + 4);
			Point bendPoint2 = gaService.createPoint(endMosShapeGa.getX() + 30,
					endMosShapeGa.getY() + 4);
			// Add bend points.
			ffConnection.getBendpoints().add(bendPoint1);
			ffConnection.getBendpoints().add(bendPoint2);
		}

		// create link and wire it
		link(ffConnection, message);

		// add dynamic text decorator for the association name
		ConnectionDecorator textDecorator =
				peCreateService.createConnectionDecorator(ffConnection, true, 0.5, true);
		Text text = gaService.createDefaultText(diagram, textDecorator);
		text.setForeground(manageColor(TEXT_FOREGROUND));
		gaService.setLocation(text, 10, 0);
		// set reference name in the text decorator
		text.setValue(message.getName());

		// add static graphical decorator (composition and navigable)
		ConnectionDecorator cd =
				peCreateService.createConnectionDecorator(ffConnection, false, 1.0, true);
		MessageUtils.createArrow(cd, manageColor(MESSAGE_FOREGROUND));

		return ffConnection;
	}

	// TEST
	private ContainerShape createAlternativeFragment(Diagram diagram, List<Connection> connections) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		// Outer rectangle
		ContainerShape altFragment = peCreateService.createContainerShape(diagram, true);
		Rectangle outerRect = gaService.createRectangle(altFragment);
		outerRect.setFilled(false);
		// Set size and location.
		setAlternativeFragmentLocationAndSize(altFragment, connections);

		// Text Label
		Shape labelShape = peCreateService.createShape(altFragment, false);
		Rectangle labelRect = gaService.createRectangle(labelShape);
		labelRect.setFilled(true);
		labelRect.setBackground(manageColor(IColorConstant.WHITE));
		gaService.setLocationAndSize(labelRect, 0, 0, 40, 20);

		// Text
		Shape textShape = peCreateService.createShape(altFragment, false);
		Text text = gaService.createDefaultText(diagram, textShape, "u-alt");
		text.setForeground(manageColor(IColorConstant.BLACK));
		text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
		text.setVerticalAlignment(Orientation.ALIGNMENT_TOP);
		gaService.setLocationAndSize(text, 0, 0, 40, 20);

		// Separators
		GraphicsAlgorithm fragmentGa = altFragment.getGraphicsAlgorithm();
		for (int i = 1; i < connections.size(); ++i) {
			GraphicsAlgorithm prevGa = connections.get(i - 1).getEnd().getParent().getGraphicsAlgorithm();
			GraphicsAlgorithm nextGa = connections.get(i).getStart().getParent().getGraphicsAlgorithm();
			int prevY = prevGa.getY() + prevGa.getHeight() / 2;
			int nextY = nextGa.getY() + nextGa.getHeight() / 2;
			int separatorY = (prevY + nextY) / 2 - fragmentGa.getY();

			Shape separatorShape = peCreateService.createShape(altFragment, false);
			Polyline separator = gaService.createPolyline(separatorShape, new int[] {
					0, separatorY, fragmentGa.getWidth(), separatorY
			});
			separator.setLineStyle(LineStyle.DASH);
		}

		return altFragment;
	}

	private void setAlternativeFragmentLocationAndSize(ContainerShape altFragment, List<Connection> connections) {
		IGaService gaService = Graphiti.getGaService();

		// Get bounding box
		int left = 0, right = 0, top = 0, bottom = 0;
		for (int i = 0; i < connections.size(); ++i) {
			Connection connection = connections.get(i);
			GraphicsAlgorithm startGa = connection.getStart().getParent().getGraphicsAlgorithm();
			int startX = startGa.getX() + startGa.getWidth() / 2;
			int startY = startGa.getY() + startGa.getHeight() / 2;
			if (i == 0) {
				left = startX;
				right = startX;
				top = startY;
				bottom = startY;
			} else {
				left = Math.min(left, startX);
				right = Math.max(right, startX);
				top = Math.min(top, startY);
				bottom = Math.max(bottom, startY);
			}
			GraphicsAlgorithm endGa = connection.getEnd().getParent().getGraphicsAlgorithm();
			int endX = endGa.getX() + endGa.getWidth() / 2;
			int endY = endGa.getY() + endGa.getHeight() / 2;
			left = Math.min(left, endX);
			right = Math.max(right, endX);
			top = Math.min(top, endY);
			bottom = Math.max(bottom, endY);
		}

		int width = Math.max(right - left, 40) + 60;
		int height = (bottom - top) + 55;
		gaService.setLocationAndSize(altFragment.getGraphicsAlgorithm(),
				left - 30, top - 35, width, height);
	}
}
