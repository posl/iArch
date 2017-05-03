package jp.ac.kyushu_u.iarch.checkplugin.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author fukamachi 不確かなBehaviorを通常のBehaviorのリストに変換し，それを包括するコンテナです．
 *         具体的には，OptionalやAlternativeのコンポーネントを展開し，全ての通りにおいて型検査が可能になるようにします．
 */
public class UncertainBehaviorContainer {

	private String name;
	private BehaviorPairModel originalBehavior;
	private ArrayList<BehaviorPairModel> separatedBehaviors;
	private ArrayList<BehaviorPairModel> compileSuccessedBehaviors;
	private ArrayList<BehaviorPairModel> compileFailedBehaviors;
	private HashSet<ArrayList<CallPairModel>> errorCallSet = new HashSet<ArrayList<CallPairModel>>();

	/**
	 * @param uncertainBehavior
	 *            OptionalやAlternativeのCallを含むBehaviorModel
	 *
	 */
	public UncertainBehaviorContainer(BehaviorPairModel uncertainBehavior) {
		this.name = uncertainBehavior.getName();
		this.originalBehavior = uncertainBehavior;
		this.separatedBehaviors = new ArrayList<BehaviorPairModel>();
		this.compileSuccessedBehaviors = new ArrayList<BehaviorPairModel>();
		this.compileFailedBehaviors = new ArrayList<BehaviorPairModel>();
		separateBehavior(uncertainBehavior);
		typeCheckUncertainBehavior();
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return separatedBehaviors
	 */
	public ArrayList<BehaviorPairModel> getSeparatedBehaviors() {
		return separatedBehaviors;
	}

	/**
	 * @return compileSuccessedBehaviors
	 */
	public ArrayList<BehaviorPairModel> getCompileSuccessedBehaviors() {
		return compileSuccessedBehaviors;
	}

	/**
	 * @return compileFailedBehaviors
	 */
	public ArrayList<BehaviorPairModel> getCompileFailedBehaviors() {
		return compileFailedBehaviors;
	}

	/**
	 * @return errorCallSet
	 */
	public HashSet<ArrayList<CallPairModel>> getErrorCallSet() {
		return errorCallSet;
	}

	/**
	 * @return originalBehavior
	 */
	public BehaviorPairModel getOriginalBehavior() {
		return originalBehavior;
	}

	private void separateBehavior(BehaviorPairModel uncertainBehavior) {
		List<CallPairModel> callPairModels = uncertainBehavior.getCallModels();
		ArrayList<Integer> uncertainCallTypeNums = new ArrayList<Integer>();

		for (CallPairModel call : callPairModels) {
			if (call.isOpt) {
				uncertainCallTypeNums.add(2);
			} else if (call.isAlt) {
				uncertainCallTypeNums.add(1 + call.getAltMethodPairSets()
						.size());
			}
		}

		MyCounter counter = new MyCounter(uncertainCallTypeNums);

		while (true) {
			int uncertainCallIndex = 0;
			ArrayList<CallPairModel> certainCallPairModels = new ArrayList<CallPairModel>(
					callPairModels);
			for (int i = 0; i < certainCallPairModels.size(); i++) {
				CallPairModel call = certainCallPairModels.get(i);
				if (call.isOpt) {
					if (counter.getCounter().get(uncertainCallIndex) == 0) {
						certainCallPairModels.remove(i);
						break;
					} else {
						call = new CallPairModel(certainCallPairModels.get(i));
						call.setOpt(false);
					}
					uncertainCallIndex++;
				} else if (call.isAlt) {
					if (counter.getCounter().get(uncertainCallIndex) == 0) {
						call = new CallPairModel(certainCallPairModels.get(i));
						call.setAlt(false);
					} else {
						call = new CallPairModel(
								new ArrayList<ComponentClassPairModel>(
										call.getComponentClassPairModels()),
								call.getAltMethodPairSets()
										.get(counter.getCounter().get(
												uncertainCallIndex) - 1)
										.getArchMethod());
					}
					uncertainCallIndex++;
				}
				certainCallPairModels.set(i, call);
			}

			this.separatedBehaviors.add(new BehaviorPairModel(this.name,
					new ArrayList<CallPairModel>(certainCallPairModels), this));
			if (!counter.increment())
				break;
		}
	}

	/**
	 * タイプチェックを行い，separatedBehaviorsをタイプチェックがうまく行ったものとそうでないものに分割する．
	 */
	private void typeCheckUncertainBehavior() {

		// 全てCertainになったBehaviorを全て検査する
		for (BehaviorPairModel bp : this.separatedBehaviors) {
			ArrayList<CallPairModel> callPairModels = (ArrayList<CallPairModel>) bp
					.getCallModels();
			if (callPairModels.size() > 1) {// 長さ1のモノはチェックすることができないので除外
				boolean isCompileSuccess = true;
				// ループを行う回数はコールの数 - 1
				for (int i = 0; i < callPairModels.size() - 1; i++) {
					CallPairModel currentCallModel = callPairModels.get(i);
					CallPairModel nextCallModel = callPairModels.get(i + 1);
					if (!currentCallModel.getMethodModel().hasInvocation(
							nextCallModel.getName())) {// 呼び出しがない場合…
						if (isCompileSuccess) {
							isCompileSuccess = false;
						}
						ArrayList<CallPairModel> errorPair = new ArrayList<CallPairModel>();
						errorPair.add(currentCallModel);
						errorPair.add(nextCallModel);
						errorCallSet.add(errorPair);
					}
				}
				if (isCompileSuccess) {
					compileSuccessedBehaviors.add(bp); // コンパイルがうまく行ったBehaviorに追加
				} else {
					compileFailedBehaviors.add(bp); // コンパイルがうまく行かなかったBehaviorに追加
				}
			}
		}
	}

	private class MyCounter {
		private int[] counter;
		private ArrayList<Integer> maxDigitsList;

		/**
		 * @param uncertainCallTypeNums
		 *            不確かなBehaviorCallの数の組み合わせ
		 */
		public MyCounter(ArrayList<Integer> uncertainCallTypeNums) {
			this.counter = new int[uncertainCallTypeNums.size()];
			this.maxDigitsList = (ArrayList<Integer>) uncertainCallTypeNums;
			for (int i = 0; i < counter.length; i++) {
				counter[i] = 0;
			}
		}

		/**
		 * MyCounterのcounterをインクリメントする．
		 *
		 * @return MyCounterがまだインクリメントできるか(上限に来ていないか)．
		 */
		public boolean increment() {
			for (int i = 0; i < this.counter.length; i++) {
				if (this.counter[i] < maxDigitsList.get(i) - 1) {
					this.counter[i]++;
					return true;
				} else {
					this.counter[i] = 0;
				}
			}
			return false;
		}

		public ArrayList<Integer> getCounter() {
			ArrayList<Integer> list = new ArrayList<Integer>();
			for (int i = 0; i < counter.length; i++) {
				list.add(counter[i]);
			}
			return list;
		}
	}
}
