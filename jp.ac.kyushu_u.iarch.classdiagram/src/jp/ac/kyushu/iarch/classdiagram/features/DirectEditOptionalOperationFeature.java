package jp.ac.kyushu.iarch.classdiagram.features;

import jp.ac.kyushu.iarch.classdiagram.utils.ClassUtils;
import jp.ac.kyushu.iarch.classdiagram.utils.OperationUtils;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.impl.AbstractDirectEditingFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

import umlClass.OptionalOperation;

public class DirectEditOptionalOperationFeature extends
		AbstractDirectEditingFeature {

	public DirectEditOptionalOperationFeature(IFeatureProvider fp) {
		super(fp);
	}

	public int getEditingType() {
		// there are several possible editor-types supported:
		// text-field, checkbox, color-chooser, combobox, ...
		return TYPE_TEXT;
	}

	@Override
	public boolean canDirectEdit(IDirectEditingContext context) {
		PictogramElement pe = context.getPictogramElement();
		Object bo = getBusinessObjectForPictogramElement(pe);
		GraphicsAlgorithm ga = context.getGraphicsAlgorithm();
		// support direct editing, if it is an OptionalOperation, and the user clicked
		// directly on the text and not somewhere else in the rectangle
		if (bo instanceof OptionalOperation && ga instanceof Text) {
			return true;
		}
		// direct editing not supported in all other cases
		return false;
	}

	public String getInitialValue(IDirectEditingContext context) {
		// return the current name of the OptionalOperation
		PictogramElement pe = context.getPictogramElement();
		OptionalOperation eOperation = (OptionalOperation) getBusinessObjectForPictogramElement(pe);
		return eOperation.getName();
	}

	@Override
	public String checkValueValid(String value, IDirectEditingContext context) {
		if (value.length() < 1)
			return "Please enter any text as operation name.";
		if (value.contains(" "))
			return "Spaces are not allowed in operation names.";
		if (value.contains("\n"))
			return "Line breakes are not allowed in operation names.";

		if (!OperationUtils.validName(value)) {
			return "Invalid operation name.";
		}

		PictogramElement pe = context.getPictogramElement();
		OptionalOperation eOperation = (OptionalOperation) getBusinessObjectForPictogramElement(pe);
		umlClass.Class eClass = eOperation.getClass_();
		if (eClass != null) {
			if (!eOperation.getName().equals(value) && ClassUtils.hasName(eClass, value)) {
				return "Cannot use operation names which already exist.";
			}
		} else {
			// Something goes wrong... (maybe ECore setting is inappropriate)
		}

		// null means, that the value is valid
		return null;
	}

	public void setValue(String value, IDirectEditingContext context) {
		// set the new name for the OptionalOperation
		PictogramElement pe = context.getPictogramElement();
		OptionalOperation eOperation = (OptionalOperation) getBusinessObjectForPictogramElement(pe);
		eOperation.setName(value);

		// Explicitly update the shape to display the new value in the diagram
		// Note, that this might not be necessary in future versions of Graphiti
		// (currently in discussion)

		// we know, that pe is the Shape of the Text, so its container is the
		// main shape of the EClass
		updatePictogramElement(((Shape) pe).getContainer());
	}
}