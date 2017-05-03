package jp.ac.kyushu_u.iarch.classdiagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.impl.AbstractAddShapeFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.util.ColorConstant;
import org.eclipse.graphiti.util.IColorConstant;

import jp.ac.kyushu_u.iarch.classdiagram.utils.AlternativeOperationUtils;
import jp.ac.kyushu_u.iarch.classdiagram.utils.ClassUtils;
import umlClass.AlternativeOperation;

public class AddAlternativeOperationFeature extends AbstractAddShapeFeature {
	private static final IColorConstant ALTERNATIVE_OPERATION_TEXT_FOREGROUND =
			IColorConstant.BLACK;

	private static final IColorConstant ALTERNATIVE_OPERATION_FOREGROUND =
			new ColorConstant(98, 131, 167);

	public AddAlternativeOperationFeature(IFeatureProvider fp) {
		super(fp);
	}

	public boolean canAdd(IAddContext context) {
		// check if user wants to add a target Object.
		if (context.getNewObject() instanceof AlternativeOperation) {
			// check if user wants to add to a Class.
			ContainerShape shape = context.getTargetContainer();
			Object bo = getBusinessObjectForPictogramElement(shape);
			if (bo instanceof umlClass.Class) {
				return true;
			}
		}
		return false;
	}

	public PictogramElement add(IAddContext context) {
		AlternativeOperation addedOperation = (AlternativeOperation) context.getNewObject();
		ContainerShape targetClass = context.getTargetContainer();
		Object bo = getBusinessObjectForPictogramElement(targetClass);
		umlClass.Class eClass = (umlClass.Class) bo;

		IPeCreateService peCreateService = Graphiti.getPeCreateService();
		IGaService gaService = Graphiti.getGaService();

		// Get size of Class and extend height if needed.
		GraphicsAlgorithm targetGa = targetClass.getGraphicsAlgorithm();
		int targetWidth = targetGa.getWidth();
		int targetHeight = targetGa.getHeight();
		int leastTargetHeight = ClassUtils.calculateHeight(eClass);
		if (targetHeight <= leastTargetHeight) {
			targetHeight = leastTargetHeight;
			gaService.setSize(targetGa, targetWidth, targetHeight);
		}

		// Get baseline height to add pictograms.
		int baseline = ClassUtils.calculateOperationBaseline(eClass, addedOperation);

		// Count line elements.
		int linenum = 0;
		for (Shape shape: targetClass.getChildren()) {
			if (shape.getGraphicsAlgorithm() instanceof Polyline) {
				linenum++;
			}
		}

		if (linenum == 1) {
			// create shape for line
			Shape shape = peCreateService.createShape(targetClass, false);

			// create and set graphics algorithm
			Polyline polyline = gaService.createPolyline(shape,
					new int[] { 0, baseline, targetWidth, baseline });
			polyline.setForeground(manageColor(ALTERNATIVE_OPERATION_FOREGROUND));
			polyline.setLineWidth(2);
		}

		{
			// create shape for operation
			Shape shape = peCreateService.createShape(targetClass, true);

			// create and set text graphics algorithm
			Text text = gaService.createText(shape, AlternativeOperationUtils.getLabel(addedOperation));
			text.setForeground(manageColor(ALTERNATIVE_OPERATION_TEXT_FOREGROUND));
			text.setHorizontalAlignment(Orientation.ALIGNMENT_LEFT); 
			// vertical alignment has as default value "center"
			text.setFont(gaService.manageDefaultFont(getDiagram(), true, true));
			gaService.setLocationAndSize(text, 2, baseline, targetWidth - 4, 20);

			// create link and wire it
			link(shape, addedOperation);
		}

		// call the layout feature
		peCreateService.createChopboxAnchor(targetClass);
		layoutPictogramElement(targetClass);
		return targetClass;
	}
}