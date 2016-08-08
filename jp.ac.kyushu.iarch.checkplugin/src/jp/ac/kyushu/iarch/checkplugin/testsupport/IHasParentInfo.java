package jp.ac.kyushu.iarch.checkplugin.testsupport;

public interface IHasParentInfo<P> extends IConcreteInfo {

	public P getParent();

	public void setParent(P parent);

}
