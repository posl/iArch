package jp.ac.kyushu_u.iarch.sequencediagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.AddConnectionContext;
import org.eclipse.graphiti.features.impl.AbstractCreateConnectionFeature;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeCreateService;

import behavior.BehaviorFactory;
import behavior.Lifeline;
import behavior.OptionalMessage;
import jp.ac.kyushu_u.iarch.sequencediagram.utils.LifelineUtils;
import jp.ac.kyushu_u.iarch.sequencediagram.utils.MessageUtils;

public class CreateOptionalMessageFeature extends AbstractCreateConnectionFeature {
	private static final String DIALOG_TITLE = "Create OptionalMessage";
	private static final String DIALOG_MESSAGE = "Enter new OptionalMessage name";

	public CreateOptionalMessageFeature(IFeatureProvider fp) {
		super(fp, "OptionalMessage", "add OptionalMessage");
	}

	@Override
	public boolean canStartConnection(ICreateConnectionContext context) {
		return LifelineUtils.getLifeline(context.getSourcePictogramElement()) != null;
	}

	@Override
	public boolean canCreate(ICreateConnectionContext context) {
		return LifelineUtils.getLifeline(context.getSourcePictogramElement()) != null
				&& LifelineUtils.getLifeline(context.getTargetPictogramElement()) != null;
	}

	@Override
	public Connection create(ICreateConnectionContext context) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		Connection newConnection = null;

		// get Lifelines which should be connected
		Lifeline source = LifelineUtils.getLifeline(context.getSourcePictogramElement());
		Lifeline target = LifelineUtils.getLifeline(context.getTargetPictogramElement());
		if (source != null && target != null) {
			String messageName = (String) context.getProperty("name");
			if (messageName == null) {
				// Ask message name.
				messageName = MessageUtils.askMessageName(DIALOG_TITLE, DIALOG_MESSAGE);
			}
			if (messageName == null) {
				return null;
			}

			// create new business object
			OptionalMessage optMessage = BehaviorFactory.eINSTANCE.createOptionalMessage();
			optMessage.setArchpoint(true);
			optMessage.setName(messageName);
			getDiagram().eResource().getContents().add(optMessage);
			MessageUtils.createChildElements(optMessage, getDiagram(), source, target);

			Anchor sourceAnchor = context.getSourceAnchor();
			Anchor targetAnchor = context.getTargetAnchor();
			// To pass source/target point to AddFeature, create dummy shape.
			if (sourceAnchor == null) {
				Shape dummySourceShape = MessageUtils.createDummyShape(getDiagram(), context.getSourceLocation());
				sourceAnchor = peCreateService.createChopboxAnchor(dummySourceShape);
			}
			if (targetAnchor == null) {
				Shape dummyTargetShape = MessageUtils.createDummyShape(getDiagram(), context.getTargetLocation());
				targetAnchor = peCreateService.createChopboxAnchor(dummyTargetShape);
			}

			AddConnectionContext addContext =
					new AddConnectionContext(sourceAnchor, targetAnchor);
			addContext.setNewObject(optMessage);
			newConnection = (Connection) getFeatureProvider().addIfPossible(addContext);
			getFeatureProvider().getDirectEditingInfo().setActive(true);
		}
		return newConnection;
	}
}
