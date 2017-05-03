package jp.ac.kyushu_u.iarch.classdiagram.features;

import org.eclipse.graphiti.examples.common.ExampleUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.impl.AbstractCreateFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;

import jp.ac.kyushu_u.iarch.classdiagram.utils.ClassUtils;
import jp.ac.kyushu_u.iarch.classdiagram.utils.OperationUtils;
import jp.ac.kyushu_u.iarch.classdiagram.utils.OptionalOperationUtils;
import umlClass.OptionalOperation;

public class CreateOptionalOperationFeature extends AbstractCreateFeature {
	private static final String TITLE = "Create OptionalOperation";
	private static final String USER_QUESTION = "Enter new OptionalOperation name";

	public CreateOptionalOperationFeature(IFeatureProvider fp) {
		super(fp, "OptionalOperation", "Create OptionalOperation");
	}

	public boolean canCreate(ICreateContext context) {
		ContainerShape shape = context.getTargetContainer();
		Object bo = getBusinessObjectForPictogramElement(shape);
		if (bo instanceof umlClass.Class) {
			return true;
		}
		return false;
	}

	public Object[] create(ICreateContext context) {
		String newOperationName = (String) context.getProperty("name");
		if (newOperationName == null) {
			// ask user for Operation name
			newOperationName = ExampleUtil.askString(TITLE, USER_QUESTION, "");
		}
		if (newOperationName == null || newOperationName.trim().length() == 0) {
			return EMPTY;
		}
		if (!OperationUtils.validName(newOperationName)) {
			return EMPTY;
		}

		ContainerShape targetClass = context.getTargetContainer();
		umlClass.Class eClass = (umlClass.Class) getBusinessObjectForPictogramElement(targetClass);
		// Check name duplication
		if (ClassUtils.hasName(eClass, newOperationName)) {
			return EMPTY;
		}

		// create OptionalOperation and DataType
		OptionalOperation newOperation =
				OptionalOperationUtils.createElement(newOperationName, true);

		// Due to ECore property, DataType must be added to Resource directly.
		// TODO: It should be removed from Resouce on deleting.
		getDiagram().eResource().getContents().add(newOperation.getDatatype());

		// Due to ECore property, Operation is added to Class
		// after DataType is set to Operation.
		eClass.getOwnedOperation().add(newOperation);

		// do the add
		addGraphicalRepresentation(context, newOperation);
		updatePictogramElement(targetClass);

		// return newly created business object(s)
		return new Object[] { newOperation };
	}
}