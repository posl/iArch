package jp.ac.kyushu_u.iarch.classdiagram.features;

import org.eclipse.graphiti.examples.common.ExampleUtil;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.impl.AbstractCreateFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;

import jp.ac.kyushu_u.iarch.classdiagram.utils.AlternativeOperationUtils;
import jp.ac.kyushu_u.iarch.classdiagram.utils.ClassUtils;
import jp.ac.kyushu_u.iarch.classdiagram.utils.OperationUtils;
import umlClass.AlternativeOperation;
import umlClass.Operation;
import umlClass.UmlClassFactory;

public class CreateAlternativeOperationFeature extends AbstractCreateFeature {
	private static final String TITLE = "Create AlternativeOperation";
	private static final String USER_QUESTION = "Enter new AlternativeOperation name";

	public CreateAlternativeOperationFeature(IFeatureProvider fp) {
		super(fp, "AlternativeOperation", "Create AlternativeOperation");
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
		String[] newOperationNames = AlternativeOperationUtils.splitNames(newOperationName);
		if (newOperationNames == null || newOperationNames.length <= 1) {
			return EMPTY;
		}

		ContainerShape targetClass = context.getTargetContainer();
		umlClass.Class eClass = (umlClass.Class) getBusinessObjectForPictogramElement(targetClass);
		// Check name duplication
		for (String newName: newOperationNames) {
			if (ClassUtils.hasName(eClass, newName)) {
				return EMPTY;
			}
		}

		// create AlternativeOperation
		AlternativeOperation newOperation = UmlClassFactory.eINSTANCE.createAlternativeOperation();
		newOperation.setArchpoint(true);
		newOperation.setName("[Alternative]");

		// create Operation and DataType
		for (String newName: newOperationNames) {
			Operation op = OperationUtils.createElement(newName, false);

			// Due to ECore property, DataType must be added to Resource directly.
			// TODO: It should be removed from Resouce on deleting.
			getDiagram().eResource().getContents().add(op.getDatatype());

			// Due to ECore property, Operation is added to Class
			// after DataType is set to Operation.
			newOperation.getOperations().add(op);
		}
		eClass.getOwnedOperation().add(newOperation);

		// do the add
		addGraphicalRepresentation(context, newOperation);
		updatePictogramElement(targetClass);

		// return newly created business object(s)
		return new Object[] { newOperation };
	}
}