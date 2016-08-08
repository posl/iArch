/**
 *
 */
package jp.ac.kyushu.iarch.checkplugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fukamachi
 * AltMethodのComponentMethodPairModelを収納するためのコンテナ
 * Nameは一つ目のAltMethodの名前とし，内部のAltMethodのJavaNodeの有無に従い，hasJavaNodeも定める．
 * AlternativeはSPLに従い，JavaNodeが一つでも成立すればtrueを返すようにする
 * ComponentMethodPairModelのリストにまとめて入れるため，これを継承する．
 */
public class AltMethodPairsContainer extends ComponentMethodPairModel {
	private List<ComponentMethodPairModel> altMethodPairs = new ArrayList<ComponentMethodPairModel>();


	/**
	 * @param altMethodModels
	 *            AltMethodPairのリスト
	 * @param parentModel
	 *            クラスペア
	 */
	public AltMethodPairsContainer(
			List<ComponentMethodPairModel> altMethodModels,
			ComponentClassPairModel parentModel) {
		super(altMethodModels, parentModel);
		this.name = altMethodModels.get(0).getName();
		this.altMethodPairs = altMethodModels;
		for (ComponentMethodPairModel model : altMethodModels) {
			model.setParentAltMethodPairsContainer(this);
		}

		int counterHasJavaNode = 0;
		for (ComponentMethodPairModel model : altMethodModels) {
			if(model.hasJavaNode){
				counterHasJavaNode++;
			}
		}
		if(counterHasJavaNode >= 1){
			this.hasJavaNode = true;
		}else{
			this.hasJavaNode = false;
		}
	}

	/**
	 * @return altMethodPairs
	 */
	public List<ComponentMethodPairModel> getAltMethodPairs() {
		return altMethodPairs;
	}

	/**
	 * @param altMethodPairs セットする altMethodPairs
	 */
	public void setAltMethodPairs(List<ComponentMethodPairModel> altMethodPairs) {
		this.altMethodPairs = altMethodPairs;
	}

}
