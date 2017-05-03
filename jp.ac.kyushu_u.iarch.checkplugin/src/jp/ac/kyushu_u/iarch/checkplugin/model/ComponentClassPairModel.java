package jp.ac.kyushu_u.iarch.checkplugin.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.Node;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.Interface;

/**
 * ArchfaceとJavaコードのXMLノードのペアを保存するためのモデル． 子にメソッドのペアを持つ．
 *
 * @author fukamachi
 *
 */
public class ComponentClassPairModel {

	private Interface archInterface = null;
	private Node javaClassNode = null;
	private boolean hasJavaNode = false;
	private String name = null;
	private Node packageNode = null;
	public List<ComponentMethodPairModel> methodPairsList = new ArrayList<ComponentMethodPairModel>();

	public ComponentClassPairModel(Interface archInterface, Node javaClassNode) {
		this.archInterface = archInterface;
		this.javaClassNode = javaClassNode;
		this.name = archInterface.getName();
		if (javaClassNode != null) {
			this.hasJavaNode = true;
			this.packageNode = this.javaClassNode.getParent();
		}
	}

	public boolean overrideMethodPairModel(ComponentMethodPairModel newModel) {
		Iterator<ComponentMethodPairModel> iter = methodPairsList.iterator();
		while (iter.hasNext()) {
			ComponentMethodPairModel methodPairModel = iter.next();
			if (newModel instanceof AltMethodPairsContainer) {
				// If model is altset, simply, remove same methods, and add
				// altset model.
				for (ComponentMethodPairModel altMethodModel : ((AltMethodPairsContainer) newModel)
						.getAltMethodPairs()) {
					if(altMethodModel.getName().equals(
							methodPairModel.getName())){
						iter.remove();
					}
				}
			} else {
				if (newModel.getName().equals(methodPairModel.getName())) {
					methodPairsList.set(
							methodPairsList.indexOf(methodPairModel), newModel);
					return true;
				}
			}
		}

		methodPairsList.add(newModel);
		return false;
	}

	public Interface getArchInterface() {
		return archInterface;
	}

	public Node getJavaClassNode() {
		return javaClassNode;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return packageNode
	 */
	public Element getPackageNode() {
		return (Element) packageNode;
	}

	public boolean hasJavaNode() {
		return hasJavaNode;
	}

	// TODO なんか返り値がおかしい…
	public IResource getClassPath(IProject resource) {
		IResource src;
		if(this.getPackageNode().attributeValue("name").equals("")){
			src = resource.getFile(new Path("src/" + this.getName() + ".java"));
		} else {
			src = resource.getFile(new Path("src/"
					+ this.getPackageNode().attributeValue("name") + "/"
					+ this.getName() + ".java"));
		}
		return src;

	}

	/**
	 * @return methodPairsList
	 */
	public List<ComponentMethodPairModel> getMethodPairsList() {
		return methodPairsList;
	}

	public int getDesignPointCount() {
		if (archInterface == null) {
			return 0;
		} else {
			return 1 + archInterface.getMethods().size();
		}
	}
	public int getProgramPointCount() {
		if (javaClassNode == null) {
			return 0;
		} else {
			return 1 + javaClassNode.selectNodes("MethodDeclaration").size();
		}
	}
}
