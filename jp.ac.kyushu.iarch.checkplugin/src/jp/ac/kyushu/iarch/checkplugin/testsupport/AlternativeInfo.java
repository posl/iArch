package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.List;

/**
 * @author watanabeke
 */
public class AlternativeInfo extends AbstractUncertaintyInfo {

	public static final String TAG = "alternative";

	public AlternativeInfo(List<MethodInfo> children) {
		super(children);
		setSelectedAsUndefined();
	}

	public AlternativeInfo(List<MethodInfo> children, int index) {
		this(children);
		setSelected(index);
	}

	/**
	 * 与えられたインデックスのMethodInfoが存在すれば、それをselectedとします。
	 * @param index
	 * @return selectedが変更されたかどうか
	 */
	public void setSelected(int index) {
		setSelected(getChildren().get(index));
	}

	@Override
	protected String selectedToString() {
		if (getSelected() == null) {
			return "";
		} else {
			// 0-base -> 1-base
			return Integer.toString(getChildren().indexOf(getSelected()) + 1);
		}
	}

	@Override
	protected String getXMLTag() {
		return TAG;
	}

	@Override
	public String getTreeContentLabel() {
		return "Alternative Uncertainty";
	}

}
