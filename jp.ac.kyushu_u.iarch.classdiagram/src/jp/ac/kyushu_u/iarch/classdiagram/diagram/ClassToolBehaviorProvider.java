package jp.ac.kyushu_u.iarch.classdiagram.diagram;

import java.util.ArrayList;

import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.platform.IPlatformImageConstants;
import org.eclipse.graphiti.tb.ContextMenuEntry;
import org.eclipse.graphiti.tb.DefaultToolBehaviorProvider;
import org.eclipse.graphiti.tb.IContextMenuEntry;
import org.eclipse.graphiti.tb.IDecorator;
import org.eclipse.graphiti.tb.ImageDecorator;

import umlClass.NamedElement;

public class ClassToolBehaviorProvider extends DefaultToolBehaviorProvider{

	public ClassToolBehaviorProvider(IDiagramTypeProvider diagramTypeProvider) {
		super(diagramTypeProvider);
		// TODO Auto-generated constructor stub
	}

	@Override
	public IDecorator[] getDecorators(PictogramElement pe) {
	    	
    	IFeatureProvider featureProvider = getFeatureProvider();
        Object bo = featureProvider.getBusinessObjectForPictogramElement(pe);

        if(bo instanceof NamedElement){
        	boolean isArchpoint = ((NamedElement)bo).isArchpoint();
        	if(!isArchpoint){
        		IDecorator imageRenderingDecorator =
                        new ImageDecorator(
                            IPlatformImageConstants.IMG_ECLIPSE_WARNING_TSK);
                    imageRenderingDecorator
                        .setMessage("Archpoint Unselected");
                    return new IDecorator[] { imageRenderingDecorator };
        	}
        }
        return super.getDecorators(pe);
    }

	@Override
	public IContextMenuEntry[] getContextMenu(ICustomContext context) {
		ArrayList<IContextMenuEntry> entries = new ArrayList<IContextMenuEntry>();

		// create a sub-menu for rename/refactoring features
		ContextMenuEntry subMenu = new ContextMenuEntry(null, context);
		subMenu.setText("Refactor");
		subMenu.setDescription("Refactor features submenu");

		for (ICustomFeature customFeature :
			getFeatureProvider().getCustomFeatures(context)) {
			if (customFeature.isAvailable(context)) {
				ContextMenuEntry menuEntry =
						new ContextMenuEntry(customFeature, context);

				String customFeatureName = customFeature.getName();
				if (customFeatureName.startsWith("Rename ")
						|| customFeatureName.startsWith("Refactoring ")) {
					subMenu.add(menuEntry);
				} else {
					entries.add(menuEntry);
				}
			}
		}

		entries.add(subMenu);
		return entries.toArray(new IContextMenuEntry[entries.size()]);
	}
}
