package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.List;

public interface IHasChildrenInfo<C> extends IConcreteInfo {

	public List<C> getChildren();

}
