package jp.ac.kyushu_u.iarch.basefunction.controller;

import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.graphiti.mm.pictograms.PictogramsPackage;

import behavior.BehaviorPackage;
import umlClass.UmlClassPackage;

/**
 * Graphiti file parser
 * @author hosoai
 */
public class GraphitiModelManager {

	private static ResourceSet createResourceSet() {
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put("diagram", new XMIResourceFactoryImpl());
		resourceSet.getPackageRegistry().put(
				"http://eclipse.org/graphiti/mm/pictograms",
				PictogramsPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put("http://umlClass/1.0",
				UmlClassPackage.eINSTANCE);
		resourceSet.getPackageRegistry().put("http://behavior/1.0",
				BehaviorPackage.eINSTANCE);
		return resourceSet;
	}

	public static Resource getGraphitiModel(IResource file) {
		URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
		Resource resource = createResourceSet().getResource(uri, true);
		return resource;
	}

	public static Resource createGraphitiModel(IResource file) {
		URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(), true);
		Resource resource = createResourceSet().createResource(uri);
		return resource;
	}

}
