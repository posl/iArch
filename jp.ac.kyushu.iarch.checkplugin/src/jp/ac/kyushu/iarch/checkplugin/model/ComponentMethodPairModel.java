/**
 *
 */
package jp.ac.kyushu.iarch.checkplugin.model;

import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.AltMethod;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.OptMethod;

import org.dom4j.Element;
import org.dom4j.Node;

/**
 * @author fukamachi
 *
 */
// TODO Uncertainな要素を分離し，このクラスを継承したクラスを作成する
public class ComponentMethodPairModel {
	private Method archMethod = null;
	private ComponentClassPairModel parentModel = null;
	private Node javaMethodNode = null;
	private Node javaClassNode = null;
	private int lineNumber = 0;
	protected String name = null;
	protected boolean hasJavaNode = false;
	private boolean isOpt = false;
	private boolean isAlt = false;
	private boolean hasInvocation = false;
	private ComponentMethodPairModel parentAltMethodPairsContainer = null;
	private GitDiff recentDiff = null;

	/**
	 *
	 * @param archMethod
	 *            メソッド本体
	 * @param javaMethodNode
	 *            合致しているJavaのメソッドNode(ない場合はnull)
	 * @param parentModel
	 *            メソッドが属しているクラスのComponentClassModel
	 */
	public ComponentMethodPairModel(Method archMethod, Node javaMethodNode,
			ComponentClassPairModel parentModel) {
		this.archMethod = archMethod;
		this.parentModel = parentModel;
		this.javaMethodNode = javaMethodNode;
		if (javaMethodNode != null) {
			hasJavaNode = true;
			this.javaClassNode = javaMethodNode.getParent();
			this.lineNumber = Integer.parseInt(((Element) this.javaMethodNode)
					.attributeValue("lineNumber").toString());
		}
		this.name = archMethod.getName();
		if (archMethod.eContainer() instanceof OptMethod) {
			isOpt = true;
		} else if (archMethod.eContainer() instanceof AltMethod) {
			isAlt = true;
		}
	}

	/**
	 * AltMethodPairsContainer用のコンストラクタ
	 *
	 * @param altMethodModels
	 * @param parentModel
	 */
	public ComponentMethodPairModel(
			List<ComponentMethodPairModel> altMethodModels,
			ComponentClassPairModel parentModel) {
	}

	/**
	 * @return archMethod
	 */
	public Method getArchMethod() {
		return archMethod;
	}

	/**
	 * @return javaMethodNode
	 */
	public Node getJavaMethodNode() {
		return javaMethodNode;
	}

	/**
	 * @return javaClassNode
	 */
	public Node getJavaClassNode() {
		return javaClassNode;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return parentModel
	 */
	public ComponentClassPairModel getParentModel() {
		return parentModel;
	}

	/**
	 * @return isExistJavaNode
	 */
	public boolean hasJavaNode() {
		return hasJavaNode;
	}

	/**
	 * @return isOpt
	 */
	public boolean isOpt() {
		return isOpt;
	}

	/**
	 * @return isAlt
	 */
	public boolean isAlt() {
		return isAlt;
	}

	/**
	 * @return isInvocationExist
	 */
	public boolean hasInvocation(String name) {
		// そもそもメソッド自体が存在しない場合はXMLノードを辿れないのでチェックをする
		if (this.hasJavaNode) {
			hasInvocation = (javaMethodNode
					.selectSingleNode("MethodInvocation[@name='" + name + "']") != null);
			return hasInvocation;
		} else {
			return false;
		}
	}

	public ComponentMethodPairModel getParentAltMethodPairsContainer() {
		return parentAltMethodPairsContainer;
	}

	public void setParentAltMethodPairsContainer(
			ComponentMethodPairModel parentAltMethodPairsContainer) {
		this.parentAltMethodPairsContainer = parentAltMethodPairsContainer;
	}

	/**
	 * @return lineNumber
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	public GitDiff getRecentDiff() {
		return recentDiff;
	}

	public void setRecentDiff(GitDiff recentDiff) {
		this.recentDiff = recentDiff;
	}



}
