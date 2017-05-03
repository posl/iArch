package jp.ac.kyushu_u.iarch.checkplugin.testsupport;

/**
 * Aspect生成用の中間オブジェクトです。
 * OptionalをAlternativeとみなすときに暗黙的に包含される空の処理を表しています。
 * @author watanabeke
 *
 */
public class EmptyMethodBean implements IMethodBean {
	
	public EmptyMethodBean() {}
	
	private String label;
	
	private double weight;

	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public boolean getIsEmpty() {
		return true;
	}

}
