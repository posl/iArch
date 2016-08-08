package jp.ac.kyushu.iarch.checkplugin.model;

import java.util.ArrayList;
import java.util.List;

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

}
