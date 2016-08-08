package jp.ac.kyushu.iarch.sequencediagram.features;

import jp.ac.kyushu.iarch.sequencediagram.utils.LifelineUtils;
import jp.ac.kyushu.iarch.sequencediagram.utils.MessageUtils;

import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddConnectionContext;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
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

import behavior.BehaviorExecutionSpecification;
import behavior.Lifeline;
import behavior.MessageOccurrenceSpecification;
import behavior.OptionalMessage;

public class AddOptionalMessageFeature extends AbstractAddFeature {
	private static final IColorConstant MESSAGE_FOREGROUND = new ColorConstant(0, 0, 0);
	private static final IColorConstant TEXT_FOREGROUND = IColorConstant.BLACK;
	private static final IColorConstant MOS_FOREGROUND = MESSAGE_FOREGROUND;
	private static final IColorConstant MOS_BACKGROUND = MESSAGE_FOREGROUND;

	public AddOptionalMessageFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		if (context instanceof IAddConnectionContext
				&& context.getNewObject() instanceof OptionalMessage) {
			return true;
		}
		return false;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		IAddConnectionContext connectionContext = (IAddConnectionContext) context;
		OptionalMessage addedMessage = (OptionalMessage) context.getNewObject();

		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		// Get other business objects.
		MessageOccurrenceSpecification startMos =
				(MessageOccurrenceSpecification) addedMessage.getSendEvent();
		MessageOccurrenceSpecification endMos =
				(MessageOccurrenceSpecification) addedMessage.getReceiveEvent();
		Lifeline source = startMos.getCovered().get(0);
		Lifeline target = endMos.getCovered().get(0);
		BehaviorExecutionSpecification sourceBes = source.getBehaviorExecution().get(0);
		BehaviorExecutionSpecification targetBes = target.getBehaviorExecution().get(0);

		// Check the Actor or Object to set the location of
		// BehaviorExecutionSpecification
		Diagram diagram = getDiagram();
		int sourceLocationX = LifelineUtils.getLocationX(source, diagram, 0);
		int targetLocationX = LifelineUtils.getLocationX(target, diagram, 0);
		int locationY = connectionContext.getSourceAnchor().getParent().getGraphicsAlgorithm().getY();

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
		polyline.setX(context.getX());
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
		link(ffConnection, addedMessage);

		// add dynamic text decorator for the association name
		ConnectionDecorator textDecorator =
				peCreateService.createConnectionDecorator(ffConnection, true, 0.5, true);
		Text text = gaService.createDefaultText(getDiagram(), textDecorator);
		text.setForeground(manageColor(TEXT_FOREGROUND));
		gaService.setLocation(text, 10, 0);
		// set reference name in the text decorator
		text.setValue(addedMessage.getName());

		// add static graphical decorator (composition and navigable)
		ConnectionDecorator cd =
				peCreateService.createConnectionDecorator(ffConnection, false, 1.0, true);
		MessageUtils.createArrow(cd, manageColor(MESSAGE_FOREGROUND));

		// Add combined fragment.
		ContainerShape optFragment = createOptionalFragment(diagram, ffConnection);
		link(optFragment, addedMessage);

		MessageUtils.setMessageOrders(diagram);

		// It must returns Connection object.
		return ffConnection;
	}

	private ContainerShape createOptionalFragment(Diagram diagram, Connection connection) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		// Outer rectangle
		ContainerShape optFragment = peCreateService.createContainerShape(diagram, true);
		Rectangle outerRect = gaService.createRectangle(optFragment);
		outerRect.setFilled(false);
		// Set size and location.
		setOptionalFragmentLocationAndSize(optFragment, connection);

		// Text Label
		Shape labelShape = peCreateService.createShape(optFragment, false);
		Rectangle labelRect = gaService.createRectangle(labelShape);
		labelRect.setFilled(true);
		labelRect.setBackground(manageColor(IColorConstant.WHITE));
		gaService.setLocationAndSize(labelRect, 0, 0, 40, 20);

		// Text
		Shape textShape = peCreateService.createShape(optFragment, false);
		Text text = gaService.createDefaultText(diagram, textShape, "u-opt");
		text.setForeground(manageColor(IColorConstant.BLACK));
		text.setHorizontalAlignment(Orientation.ALIGNMENT_CENTER);
		text.setVerticalAlignment(Orientation.ALIGNMENT_TOP);
		gaService.setLocationAndSize(text, 0, 0, 40, 20);

		return optFragment;
	}

	private void setOptionalFragmentLocationAndSize(ContainerShape optFragment, Connection connection) {
		IGaService gaService = Graphiti.getGaService();

		// Get left and top
		GraphicsAlgorithm startGa = connection.getStart().getParent().getGraphicsAlgorithm();
		GraphicsAlgorithm endGa = connection.getEnd().getParent().getGraphicsAlgorithm();
		int left = Math.min(startGa.getX() + startGa.getWidth() / 2,
				endGa.getX() + endGa.getWidth() / 2) - 30;
		int top = Math.min(startGa.getY() + startGa.getHeight() / 2,
				endGa.getY() + endGa.getHeight() / 2) - 35;

		IDimension dim = gaService.calculateSize(connection.getGraphicsAlgorithm());
		int width = Math.max(dim.getWidth(), 40) + 60;
		int height = dim.getHeight() + 55;

		gaService.setLocationAndSize(optFragment.getGraphicsAlgorithm(), left, top, width, height);
	}
}
