package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * テストインスタンスにおける，インターフェース(実装においてはクラス)の情報を保持します．
 * @author watanabeke
 */
public class InterfaceInfo extends AbstractCompositeInfo<SelectionInfo, AbstractUncertaintyInfo>
		implements ITreeContentInfo, IAspectGeneratable, IXMLGeneratable {

	// XMLでのタグ名および属性名．
	public static final String TAG = "interface";
	public static final String ATTR_NAME = "name";

	private final String name;

	public InterfaceInfo(List<AbstractUncertaintyInfo> children, String name) {
		super(children);
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * 自分の子オブジェクトのselectedを取りまとめてSetで返します．
	 * ただしMethodInfo以外(EmptyMethodInfoやnull)は除外します．
	 */
	private Set<MethodInfo> getSelecteds() {
		Set<MethodInfo> result = new HashSet<>();
		for (AbstractUncertaintyInfo child : getChildren()) {
			IMethodInfo selected = child.getSelected();
			if (selected instanceof MethodInfo) {
				result.add((MethodInfo) selected);
			}
		}
		return result;
	}

	/**
	 * Selection
	 * @param other
	 */
	protected void update(InterfaceInfo other) {
		// otherで選択されているメソッドたちを
		Set<MethodInfo> selecteds = other.getSelecteds();
		// 自分の子オブジェクトに渡す
		for (AbstractUncertaintyInfo child : getChildren()) {
			child.electSelected(selecteds);
		}
	}

	@Override
	protected String getXMLTag() {
		return TAG;
	}

	@Override
	protected List<List<String>> getXMLAttrIter() {
		return Arrays.asList(Arrays.asList(ATTR_NAME, name));
	}

	@Override
	public Object[] getTreeContentChildren() {
		return getChildren().toArray();
	}

	@Override
	public boolean hasTreeContentChildren() {
		return !getChildren().isEmpty();
	}

	@Override
	public String getTreeContentLabel() {
		return name;
	}

}
