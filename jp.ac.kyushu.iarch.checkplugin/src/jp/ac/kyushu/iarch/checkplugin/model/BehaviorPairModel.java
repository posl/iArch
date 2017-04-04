package jp.ac.kyushu.iarch.checkplugin.model;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.SuperMethod;
import jp.ac.kyushu.iarch.checkplugin.utils.ArchModelUtils;

/**
 * @author fukamachi
 *
 */
public class BehaviorPairModel {
	ArrayList<CallPairModel> callModels = new ArrayList<CallPairModel>();
	UncertainBehaviorContainer parentUncertainBehaviorContainer = null;
	String name = null;

	public BehaviorPairModel(String name,
			ArrayList<CallPairModel> callModels) {
		this.name = name;
		this.callModels = callModels;
	}

	public BehaviorPairModel(String name,
			ArrayList<CallPairModel> callModels,
			UncertainBehaviorContainer parentContainer) {
		this.name = name;
		this.callModels = callModels;
		this.parentUncertainBehaviorContainer = parentContainer;
	}

	public String getName() {
		return name;
	}

	/**
	 * @return methodModels
	 */
	public List<CallPairModel> getCallModels() {
		return callModels;
	}

	/**
	 * @return parentUncertainBehaviorContainer
	 */
	public UncertainBehaviorContainer getParentUncertainBehaviorContainer() {
		return parentUncertainBehaviorContainer;
	}

	public int getDesignPointCount() {
		if (callModels == null) {
			return 0;
		} else {
			// Implemented after AbstractionRatioChecker#checkArchface
			int c = 0;
			String previousInterfaceName = "";
			for (CallPairModel cpm : callModels) {
				SuperMethod superMethod = cpm.getArchMethod();
				if (superMethod instanceof Method) {
					Interface nowInterface = ArchModelUtils.getInterface((Method) superMethod, true);
					if (nowInterface == null) {
						// It means that the Method does not belong to Interface.
						continue;
					}
					String nowInterfaceName = nowInterface.getName();
					if (nowInterfaceName.equals(previousInterfaceName)) {
						c += 2;
					} else {
						c++;
						previousInterfaceName = nowInterfaceName;
					}
				}
			}
			return c;
		}
	}
}
