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

import behavior.AlternativeMessage;
import behavior.BehaviorFactory;
import behavior.Lifeline;
import behavior.Message;
import jp.ac.kyushu_u.iarch.sequencediagram.utils.AlternativeMessageUtils;
import jp.ac.kyushu_u.iarch.sequencediagram.utils.LifelineUtils;
import jp.ac.kyushu_u.iarch.sequencediagram.utils.MessageUtils;

public class CreateAlternativeMessageFeature extends AbstractCreateConnectionFeature {
	private static final String DIALOG_TITLE = "Create AlternativeMessage";
	private static final String DIALOG_MESSAGE = "Enter new AlternativeMessage names. (separated by space)";

	public CreateAlternativeMessageFeature(IFeatureProvider fp) {
		super(fp, "AlternativeMessage", "add AlternativeMessage");
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
			String[] messageNames = null;
			String name = (String) context.getProperty("name");
			if (name != null) {
				messageNames = AlternativeMessageUtils.splitNames(name);
			} else {
				// Ask message names.
				messageNames = AlternativeMessageUtils.askMessageNames(DIALOG_TITLE, DIALOG_MESSAGE);
			}
			if (messageNames == null) {
				return null;
			}

			// create new business object
			AlternativeMessage altMessage = BehaviorFactory.eINSTANCE.createAlternativeMessage();
			altMessage.setArchpoint(true);
			altMessage.setName("[Alternative]");
			getDiagram().eResource().getContents().add(altMessage);
			for (String messageName : messageNames) {
				Message childMessage = MessageUtils.createElement(messageName, false,
						getDiagram(), source, target);
				altMessage.getMessages().add(childMessage);
			}

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
			addContext.setNewObject(altMessage);
			newConnection = (Connection) getFeatureProvider().addIfPossible(addContext);
			getFeatureProvider().getDirectEditingInfo().setActive(true);
		}
		return newConnection;
	}
}
