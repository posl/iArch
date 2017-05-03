package jp.ac.kyushu_u.iarch.classdiagram.features;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReason;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.impl.AbstractUpdateFeature;
import org.eclipse.graphiti.features.impl.Reason;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;

import umlClass.Operation;
import umlClass.Property;

public class UpdateClassFeature extends AbstractUpdateFeature {
    
    public UpdateClassFeature(IFeatureProvider fp) {
        super(fp);
    }
 
    public boolean canUpdate(IUpdateContext context) {
        // return true, if linked business object is a EClass
        Object bo =
            getBusinessObjectForPictogramElement(context.getPictogramElement());
        return (bo instanceof umlClass.Class);
    }
 
    public IReason updateNeeded(IUpdateContext context) {
        // retrieve name from pictogram model
        String pictogramName = null;
        PictogramElement pictogramElement = context.getPictogramElement();
        if (pictogramElement instanceof ContainerShape) {
            ContainerShape cs = (ContainerShape) pictogramElement;
            for (Shape shape : cs.getChildren()) {
                if (shape.getGraphicsAlgorithm() instanceof Text) {
                    Text text = (Text) shape.getGraphicsAlgorithm();
                    pictogramName = text.getValue();
                    break;
                }
            }
        }
 
        // retrieve name from business model
        String businessName = null;
        Object bo = getBusinessObjectForPictogramElement(pictogramElement);
        if (bo instanceof umlClass.Class) {
        	umlClass.Class eClass = (umlClass.Class) bo;
            businessName = eClass.getName();
        }
 
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
		// /////////////////////////////////////

		String businessName = null;
		PictogramElement pictogramElement = context.getPictogramElement();
		Object bo = getBusinessObjectForPictogramElement(pictogramElement);
		if (bo instanceof umlClass.Class) {
			umlClass.Class eClass = (umlClass.Class) bo;
			businessName = eClass.getName();
		}

		// Set name in pictogram model
		if (pictogramElement instanceof ContainerShape) {
			ContainerShape cs = (ContainerShape) pictogramElement;
			umlClass.Class eClass = (umlClass.Class) getBusinessObjectForPictogramElement(cs);
			IGaService gaService = Graphiti.getGaService();

			GraphicsAlgorithm containerGa = cs.getGraphicsAlgorithm();
			int width = containerGa.getWidth();
			int minHeight =
					(eClass.getAttribute().size() + eClass.getOwnedOperation().size()) * 20 + 32;
			int height = containerGa.getHeight() <= minHeight ? minHeight : containerGa.getHeight();
			gaService.setSize(containerGa, width, height);

			int start = eClass.getAttribute().size() * 20 + 32;
			int numOperations = 0;
			int numProperties = 0;
			int numLines = 0;
			for (Shape shape : cs.getChildren()) {
				GraphicsAlgorithm shapeGa = shape.getGraphicsAlgorithm();
				Object bos = getBusinessObjectForPictogramElement(shape);
				if (shapeGa instanceof Text && bos instanceof umlClass.Class) {
					Text text = (Text) shapeGa;
					text.setValue(businessName);
				}
				if (shapeGa instanceof Text && bos instanceof Operation) {
					// Names are updated by themselves.
					//String name = ((Operation) bos).getName() + "()";
					Text text = (Text) shapeGa;
					//text.setValue(name);
					gaService.setLocationAndSize(text,
							2, start + numOperations * 20, width - 4, 20);
					numOperations++;
				}
				if (shapeGa instanceof Text && bos instanceof Property) {
					// Names are updated by themselves.
					//String name = ((Property) bos).getName();
					Text text = (Text) shapeGa;
					//text.setValue(name);
					gaService.setLocationAndSize(text,
							2, (numProperties + 1) * 20, width - 4, 20);
					numProperties++;
				}
				if (shapeGa instanceof Polyline) {
					if (++numLines == 2) {
						Polyline polyline = (Polyline) shapeGa;
						polyline.getPoints().get(0).setY(start);
						polyline.getPoints().get(1).setY(start);
					}
				}
			}
			int neededHeight = (numOperations + numProperties) * 20 + 36;
			if (containerGa.getHeight() < neededHeight) {
				containerGa.setHeight(neededHeight);
			}
			return true;
		}

		return false;
////////////////////////////////////////////////
//    	String businessName = null;
//        PictogramElement pictogramElement = context.getPictogramElement();
//        Object bo = getBusinessObjectForPictogramElement(pictogramElement);
//        if (bo instanceof umlClass.Class) {
//        	umlClass.Class eClass = (umlClass.Class) bo;
//            businessName = eClass.getName();
//        }
// 
//        // Set name in pictogram model
//        if (pictogramElement instanceof ContainerShape) {
//            ContainerShape cs = (ContainerShape) pictogramElement;
//            for (Shape shape : cs.getChildren()) {
//                if (shape.getGraphicsAlgorithm() instanceof Text) {
//                    Text text = (Text) shape.getGraphicsAlgorithm();
//                    text.setValue(businessName);
//                    return true;
//                }
//            }
//        }
// 
//        return false;
////////////////////////////////////////////    
	}
}