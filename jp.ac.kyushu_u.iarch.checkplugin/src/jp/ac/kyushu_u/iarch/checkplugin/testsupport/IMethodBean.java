package jp.ac.kyushu_u.iarch.checkplugin.testsupport;

/**
 * Aspect生成用の中間オブジェクトのインターフェースです。
 * @author watanabeke
 *
 */
public interface IMethodBean {
	
	public boolean getIsEmpty();
	
	public String getLabel();
	public void setLabel(String label);
	
	public double getWeight();
	public void setWeight(double weight);

}
