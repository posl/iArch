package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.Arrays;

/**
 * @author watanabeke
 */
public class OptionalInfo extends AbstractUncertaintyInfo {

	public static final String TAG = "optional";

	public OptionalInfo(final MethodInfo child) {
		super(Arrays.asList(child));
		setSelectedAsUndefined();
	}

	public OptionalInfo(MethodInfo child, boolean isSelected) {
		this(child);
		setSelected(isSelected);
	}

	// 子オブジェクトは1個のはずである
	private MethodInfo getChild() {
		return getChildren().get(0);
	}

	public void setSelected(boolean isSelected) {
		if (isSelected) {
			setSelected(getChild());
		} else {
			setSelected(EmptyMethodInfo.getInstance());
		}
	}

	@Override
	protected String selectedToString() {
		if (getSelected() == null) {
			return "";
		} else {
			return Boolean.toString(getSelected().equals(getChild()));
		}
	}

	@Override
	protected String getXMLTag() {
		return TAG;
	}

	@Override
	public String getTreeContentLabel() {
		return "Optional Uncertainty";
	}

}
