package jp.ac.kyushu.iarch.checkplugin.testsupport;

/**
 * テストインスタンスにおける，メソッドの情報を保持します．
 * @author watanabeke
 */
public interface IMethodInfo extends IInfo {

	public abstract String getType();

	public abstract String getName();

}
