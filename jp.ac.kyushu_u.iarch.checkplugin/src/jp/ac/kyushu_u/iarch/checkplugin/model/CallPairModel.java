package jp.ac.kyushu_u.iarch.checkplugin.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.AltCallChoice;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.CertainCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.OptCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperCall;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.UncertainInterface;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.impl.AltMethodImpl;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.impl.InterfaceImpl;
import jp.ac.kyushu_u.iarch.archdsl.archDSL.impl.OptMethodImpl;

/**
 *
 * CallとMethodPairModelのペア
 *
 * @author fukamachi
 *
 */
/**
 * @author fukamachi
 *
 */
public class CallPairModel{
	ArrayList<ComponentClassPairModel> componentClassPairModels = new ArrayList<ComponentClassPairModel>();
	ComponentMethodPairModel methodModel;
	SuperCall methodSuperCall;
	SuperMethod archMethod;
	String name;
	List<ComponentMethodPairModel> altMethodPairSets = new ArrayList<ComponentMethodPairModel>();
	boolean isOpt = false;
	boolean isAlt = false;

	/**
	 * @param componentClassPairModels
	 *            取得した全てのComponentClassPairModel
	 * @param methodSuperCall
	 *            Archfaceのcall
	 */
	public CallPairModel(
			ArrayList<ComponentClassPairModel> componentClassPairModels,
			SuperCall methodSuperCall) {
		this.componentClassPairModels = componentClassPairModels;
		this.methodSuperCall = (SuperCall) methodSuperCall;
		if (methodSuperCall instanceof CertainCall) {
			this.archMethod = ((CertainCall) methodSuperCall).getName();
		} else if (methodSuperCall instanceof OptCall) {
			this.archMethod = ((OptCall) methodSuperCall).getName();
		} else if (methodSuperCall instanceof AltCall) {
			this.archMethod = ((AltCall) methodSuperCall).getName().getName();
		}
		// tmp impl(finally, archMethod type will change Method from
		// SuperMethod)
		//this.name = ((Method) this.archMethod).getName();
		// It is found that archMethod get a SuperMethod instance, which is not used normally,
		// so ad-hoc treatment is applied temporally. we should correct afterwards.
		this.name = (archMethod instanceof Method) ? ((Method) archMethod).getName() : "-";

		this.methodModel = getMethodPairModelByArchMethod(archMethod);
		if (methodSuperCall instanceof OptCall) {
			this.isOpt = true;
		}
		if (methodSuperCall instanceof AltCall) {
			this.isAlt = true;
			for (AltCallChoice choice : ((AltCall) methodSuperCall)
					.getA_name()) {
				SuperMethod altMethod = choice.getName();
				this.altMethodPairSets
						.add(getMethodPairModelByArchMethod(altMethod));
			}
		}
	}

	public CallPairModel(
			ArrayList<ComponentClassPairModel> componentClassPairModels,
			Method methodCall) {
		this.componentClassPairModels = componentClassPairModels;
		this.methodSuperCall = null;
		this.archMethod = methodCall;
		this.name = ((Method) archMethod).getName();
		this.methodModel = getMethodPairModelByArchMethod((SuperMethod) archMethod);
	}

	/**
	 * コピーコンストラクタ
	 * @param orgModel
	 */
	public CallPairModel(CallPairModel orgModel){
		this.componentClassPairModels = (ArrayList<ComponentClassPairModel>) orgModel.getComponentClassPairModels();
		this.methodSuperCall = orgModel.getMethodSuperCall();
		this.archMethod = orgModel.getArchMethod();
		// tmp impl(finally, archMethod type will change Method from
		// SuperMethod)
		this.name = ((Method) this.archMethod).getName();

		this.methodModel = getMethodPairModelByArchMethod(archMethod);
		if (methodSuperCall instanceof OptCall) {
			this.isOpt = true;
		}
		if (methodSuperCall instanceof AltCall) {
			this.isAlt = true;
			for (AltCallChoice choice : ((AltCall) methodSuperCall)
					.getA_name()) {
				SuperMethod altMethod = choice.getName();
				this.altMethodPairSets
						.add(getMethodPairModelByArchMethod(altMethod));
			}
		}
	}

	/**
	 * すべてのComponentClassPairModelからCallで呼ばれているメソッドのクラスを探し、
	 * それをもとに引数と同等のComponentMethodPairModelを返す。
	 *
	 * @param archMethod
	 * @return 引数と同等のComponentMethodPairModel
	 */
	ComponentMethodPairModel getMethodPairModelByArchMethod(
			SuperMethod archMethod) {
		ComponentClassPairModel callClass = null;
		for (ComponentClassPairModel classPair : componentClassPairModels) {
			EObject container = archMethod.eContainer();
			if (container != null) {
				// in case of Call from Certain Component
				if (container instanceof InterfaceImpl) {
					if (((InterfaceImpl) container).getName().equals(classPair.getName())) {
						callClass = classPair;
						break;
					}
				}
				// in case of Call from Uncertain Component
				else if (container.eContainer() instanceof UncertainInterface) {
					if (((UncertainInterface) (container.eContainer()))
							.getSuperInterface().getName()
							.equals(classPair.getName())) {
						callClass = classPair;
						break;
					}
				}
			}
		}
		if (callClass == null) {
			return null;
		}

		ComponentMethodPairModel callMethod = null;
		for (ComponentMethodPairModel methodPair : callClass.methodPairsList) {
			if (archMethod.eContainer() instanceof InterfaceImpl
					|| archMethod.eContainer() instanceof OptMethodImpl) {
				if (((Method) archMethod).getName()
						.equals(methodPair.getName())) {
					callMethod = methodPair;
				}
			} else if (archMethod.eContainer() instanceof AltMethodImpl) {
				if (methodPair instanceof AltMethodPairsContainer) {
					for (ComponentMethodPairModel method : ((AltMethodPairsContainer) methodPair)
							.getAltMethodPairs()) {
						if (method.getName().equals(
								((Method) archMethod).getName())) {
							callMethod = method;
						}
					}
				}
			}
			if (callMethod != null) {
				break;
			}
		}
		return callMethod;
	}



	/**
	 * @return componentClassPairModels
	 */
	public List<ComponentClassPairModel> getComponentClassPairModels() {
		return componentClassPairModels;
	}

	/**
	 * @return methodModel
	 */
	public ComponentMethodPairModel getMethodModel() {
		return methodModel;
	}

	/**
	 * @return methodCall
	 */
	public SuperCall getMethodSuperCall() {
		return methodSuperCall;
	}

	/**
	 * @return archMethod
	 */
	public SuperMethod getArchMethod() {
		return archMethod;
	}

	/**
	 * @return altCallSets
	 */
	public List<ComponentMethodPairModel> getAltMethodPairSets() {
		return altMethodPairSets;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return isOpt
	 */
	public boolean isOpt() {
		return isOpt;
	}

	/**
	 * @param setOpt
	 *            セットする isOpt
	 */
	synchronized public void setOpt(boolean isOpt) {
		this.isOpt = isOpt;
	}

	/**
	 * @return isAlt
	 */
	public boolean isAlt() {
		return isAlt;
	}

	/**
	 * @param setAlt
	 *            セットする isAlt
	 */
	synchronized public void setAlt(boolean isAlt) {
		this.isAlt = isAlt;
	}
}
