package jp.ac.kyushu_u.iarch.classdiagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IDirectEditingContext;
import org.eclipse.graphiti.features.impl.AbstractDirectEditingFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;

import jp.ac.kyushu_u.iarch.classdiagram.utils.AlternativeOperationUtils;
import jp.ac.kyushu_u.iarch.classdiagram.utils.ClassUtils;
import jp.ac.kyushu_u.iarch.classdiagram.utils.OperationUtils;
import umlClass.AlternativeOperation;
import umlClass.Operation;

public class DirectEditAlternativeOperationFeature extends
		AbstractDirectEditingFeature {

	public DirectEditAlternativeOperationFeature(IFeatureProvider fp) {
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
		if (bo instanceof AlternativeOperation && ga instanceof Text) {
			return true;
		}
		// direct editing not supported in all other cases
		return false;
	}

	public String getInitialValue(IDirectEditingContext context) {
		// return the current name of the OptionalOperation
		PictogramElement pe = context.getPictogramElement();
		AlternativeOperation eOperation = (AlternativeOperation) getBusinessObjectForPictogramElement(pe);
		return AlternativeOperationUtils.getJoinedNames(eOperation);
	}

	@Override
	public String checkValueValid(String value, IDirectEditingContext context) {
		if (value.length() < 1)
			return "Please enter any text as operation name.";
//		if (value.contains(" "))
//			return "Spaces are not allowed in operation names.";
		if (value.contains("\n"))
			return "Line breakes are not allowed in operation names.";

		String[] names = AlternativeOperationUtils.splitNames(value);
		if (names == null) {
			return "Invalid operation names.";
		} else if (names.length == 1) {
			return "Input more than one operation names.";
		}

		PictogramElement pe = context.getPictogramElement();
		AlternativeOperation eOperation =
				(AlternativeOperation) getBusinessObjectForPictogramElement(pe);
		umlClass.Class eClass = eOperation.getClass_();
		if (eClass != null) {
			for (String name: names) {
				if (!AlternativeOperationUtils.hasName(eOperation, name)
						&& ClassUtils.hasName(eClass, name)) {
					return "Cannot use operation names which already exist.";
				}
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
		AlternativeOperation eOperation =
				(AlternativeOperation) getBusinessObjectForPictogramElement(pe);

		String[] names = AlternativeOperationUtils.splitNames(value);
		if (names == null) {
			// Something wrong.
			return;
		}

		eOperation.getOperations().clear();
		for (String name: names) {
			Operation op = OperationUtils.createElement(name, false);

			// Due to ECore property, DataType must be added to Resource directly.
			// TODO: It should be removed from Resouce on deleting.
			getDiagram().eResource().getContents().add(op.getDatatype());

			// Due to ECore property, Operation is added to Class
			// after DataType is set to Operation.
			eOperation.getOperations().add(op);
		}

		// Explicitly update the shape to display the new value in the diagram
		// Note, that this might not be necessary in future versions of Graphiti
		// (currently in discussion)

		// we know, that pe is the Shape of the Text, so its container is the
		// main shape of the EClass
		updatePictogramElement(((Shape) pe).getContainer());
	}
}