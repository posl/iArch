package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.Arrays;
import java.util.List;

/**
 * テストインスタンスにおける，メソッド引数の情報を保持します．
 * @author watanabeke
 */
public class ParameterInfo extends AbstractLeafInfo<MethodInfo>
		implements IXMLGeneratable {

	// XMLでのタグ名および属性名．
	public static final String TAG = "parameter";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_TYPE = "type";

	private final String type;
	private final String name;

	public ParameterInfo(String type, String name) {
		super();
		this.type = type;
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	@Override
	protected String getXMLTag() {
		return TAG;
	}

	@Override
	protected List<List<String>> getXMLAttrIter() {
		return Arrays.asList(
				Arrays.asList(ATTR_TYPE, type),
				Arrays.asList(ATTR_NAME, name));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ParameterInfo))
			return false;
		ParameterInfo other = (ParameterInfo) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}
