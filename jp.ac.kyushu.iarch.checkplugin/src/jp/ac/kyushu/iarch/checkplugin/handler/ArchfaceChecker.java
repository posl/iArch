package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.UncertainBehaviorContainer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class ArchfaceChecker extends XMLreader {

	Logger logger = Logger.getGlobal();
	private List<ComponentClassPairModel> classPairs = new ArrayList<ComponentClassPairModel>();
	private List<UncertainBehaviorContainer> behaviorPairs = new ArrayList<UncertainBehaviorContainer>();

	public ArchfaceChecker(IProject project) {
		super(project);
	}

	// public static ArchfaceChecker getInstance(IProject project){
	// ProblemViewManager.removeAllProblems(project);
	// readXMLContent(project);
	// setJavaProject(JavaCore.create(project));
	// return archfacechecker;
	// }
	public void checkProject() {
		checkProjectValidation(getArchfileResource(),
				getClassDiagramResource(),
				getSequenceDiagramResource(), getSourceCodeResource(),
				getARXMLResource());
	}

	public synchronized void checkProjectValidation(IResource archfile,
			IResource classDiagramResource,
			List<IResource> sequenceDiagramResources,
			List<IResource> sourceCodeResources, IResource aRXMLResource) {
		if (archfile == null) {
			logger.info("No archfile found. Stop the auto check.");
			return;
		}
		ArchModel archmodel = new ArchModel(archfile);

		Model archModel = archmodel.getModel();
		// check diagram
		if (classDiagramResource != null) {
			ClassDiagramChecker classDiagramChecker = new ClassDiagramChecker();
			classDiagramChecker.checkClassDiagram(archModel,
					classDiagramResource);
		}

		if (sequenceDiagramResources.size() > 0) {
			SequenceDiagramChecker sequenceDiagramChecker = new SequenceDiagramChecker();
			for (IResource sequenceDiagramResource : sequenceDiagramResources) {
				sequenceDiagramChecker.checkSequenceDiagram(archModel,
						sequenceDiagramResource);
			}
		}

		// Check source code
		ASTSourceCodeChecker astchecker = new ASTSourceCodeChecker();
		astchecker.SourceCodeArchifileChecker(archModel, getJavaProject());
		classPairs = astchecker.getComponentClassPairModels();
		behaviorPairs = astchecker.getBehaviorContainers();

		// Check AR
		// ARChecker archecker = new ARChecker();
		// archecker.checkAR(archfile, aRXMLResource);

		return;
	}

	public List<ComponentClassPairModel> getClassPairs() {
		return classPairs;
	}

	/**
	 * @return behaviorPairs
	 */
	public List<UncertainBehaviorContainer> getBehaviorPairs() {
		return behaviorPairs;
	}

}
