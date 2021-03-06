package jp.ac.kyushu_u.iarch.classdiagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.AbstractUpdateFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

import jp.ac.kyushu_u.iarch.classdiagram.utils.OptionalOperationUtils;
import umlClass.OptionalOperation;

public class UpdateOptionalOperationFeature extends AbstractUpdateFeature {

	public UpdateOptionalOperationFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canUpdate(IUpdateContext context) {
		// return true, if linked business object is a EClass
		Object bo = getBusinessObjectForPictogramElement(context.getPictogramElement());
		return (bo instanceof OptionalOperation);
	}

	private String retrieveBusinessName(IUpdateContext context) {
		PictogramElement pictogramElement = context.getPictogramElement();
		String businessName = null;
		Object bo = getBusinessObjectForPictogramElement(pictogramElement);
		if (bo instanceof OptionalOperation) {
			businessName = OptionalOperationUtils.getLabel((OptionalOperation) bo);
		}
		return businessName;
	}

	public IReason updateNeeded(IUpdateContext context) {
		// retrieve name from pictogram model
		String pictogramName = null;
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof Shape) {
			Shape shape = (Shape) pictogramElement;
			Text text = (Text) shape.getGraphicsAlgorithm();
			pictogramName = text.getValue();
		}

		// retrieve name from business model
		String businessName = retrieveBusinessName(context);

		// update needed, if names are different
		boolean updateNameNeeded =
				((pictogramName == null && businessName != null) ||
						(pictogramName != null && !pictogramName.equals(businessName)));
		if (updateNameNeeded) {
			return Reason.createTrueReason("Name is out of date");
		} else {
			return Reason.createFalseReason();
		}
	}

	public boolean update(IUpdateContext context) {
		// retrieve name from business model
		String businessName = retrieveBusinessName(context);

		// Set name in pictogram model
		PictogramElement pictogramElement = context.getPictogramElement();
		if (pictogramElement instanceof Shape) {
			Shape shape = (Shape) pictogramElement;
			Text text = (Text) shape.getGraphicsAlgorithm();
			text.setValue(businessName);
			return true;
		}
		return false;
	}
}