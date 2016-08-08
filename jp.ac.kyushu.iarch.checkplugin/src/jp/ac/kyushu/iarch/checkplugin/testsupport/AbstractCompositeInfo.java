package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.List;

/**
 * テストインスタンスの情報を保持し，その情報を種々の形式で出力します．
 * @author watanabeke
 */
public abstract class AbstractCompositeInfo<P extends IHasChildrenInfo, C extends IHasParentInfo>
		extends AbstractInnerInfo<C> implements IHasParentInfo<P> {

	private P parent;

	public AbstractCompositeInfo(List<C> children) {
		super(children);
	}

	@Override
	public P getParent() {
		return parent;
	}

	@Override
	public void setParent(P parent) {
		this.parent = parent;
	}

}
