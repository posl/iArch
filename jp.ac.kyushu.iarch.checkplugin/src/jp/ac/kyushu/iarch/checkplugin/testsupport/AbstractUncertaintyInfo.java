package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractUncertaintyInfo
		extends AbstractCompositeInfo<InterfaceInfo, MethodInfo>
		implements ITreeContentInfo {

	public static final String ATTR_SEL = "selection";

	/**
	 * methodInfosの内、選択されているメソッドを指定します。
	 * 未定義のときはnullを入れます。
	 */
	private IMethodInfo selected;

	public IMethodInfo getSelected() {
		return selected;
	}

	public void setSelected(IMethodInfo selected) {
		this.selected = selected;
	}

	public AbstractUncertaintyInfo(List<MethodInfo> children) {
		super(children);
	}

	public boolean electSelected(Set<MethodInfo> candidates) {
		// Setとしてシャローコピー
		HashSet<MethodInfo> intersection = new HashSet<>(getChildren());
		// 共通部分をとる
		intersection.retainAll(candidates);  // Setにした理由はこれが速いと思われるから
		// 1個なら
		if (intersection.size() == 1) {
			// 選択を更新
			selected = intersection.iterator().next();
			return true;
		}
		return false;
	}

	public void setSelectedAsUndefined() {
		selected = null;
	}

	@Override
	protected List<List<String>> getXMLAttrIter() {
		return Arrays.asList(Arrays.asList(ATTR_SEL, selectedToString()));
	}

	@Override
	public Object[] getTreeContentChildren() {
		return getChildren().toArray();
	}

	@Override
	public boolean hasTreeContentChildren() {
		return !getChildren().isEmpty();
	}

	abstract protected String selectedToString();

}
