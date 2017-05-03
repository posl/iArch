package jp.ac.kyushu.iarch.sequencediagram.utils;

import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.ILinkService;

import behavior.Actor;
import behavior.BehavioredClassifier;
import behavior.Lifeline;

public class LifelineUtils {
	public static Lifeline getLifeline(PictogramElement pe) {
		ILinkService linkService = Graphiti.getLinkService();

		if (pe != null) {
			Object object = linkService.getBusinessObjectForLinkedPictogramElement(pe);
			if (object instanceof Lifeline) {
				return (Lifeline) object;
			}
		}
		return null;
	}

	public static int getLocationX(Lifeline lifeline, Diagram diagram, int defaultValue) {
		ILinkService linkService = Graphiti.getLinkService();

		BehavioredClassifier bc = lifeline.getActor();
		if (bc instanceof Actor || bc instanceof behavior.Object) {
			for (PictogramElement pe: linkService.getPictogramElements(diagram, bc)) {
				GraphicsAlgorithm ga = pe.getGraphicsAlgorithm();
				if (ga instanceof Rectangle) {
					return ga.getX() + ga.getWidth() / 2;
				}
			}
		}
		return defaultValue;
	}
}
