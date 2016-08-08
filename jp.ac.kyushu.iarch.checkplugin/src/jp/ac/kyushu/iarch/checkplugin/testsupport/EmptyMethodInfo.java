package jp.ac.kyushu.iarch.checkplugin.testsupport;

/**
 * 空のメソッドです．
 * Optionalで選択していないとき，selectedはこれになります．
 * シングルトンです．
 * @author watanabeke
 */
public class EmptyMethodInfo implements IInfo, IMethodInfo {

	private static EmptyMethodInfo instance = new EmptyMethodInfo();

	private EmptyMethodInfo() {}

	public static EmptyMethodInfo getInstance() {
		return instance;
	}

	@Override
	public String getType() {
		return "void";
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException();
	}

}
