package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * テストインスタンスにおける，空でないメソッドの情報を保持します．
 * @author watanabeke
 */
public class MethodInfo extends AbstractCompositeInfo<AbstractUncertaintyInfo, ParameterInfo>
		implements IMethodInfo, ITreeContentInfo, IAspectGeneratable, IXMLGeneratable {

	// XMLでのタグ名および属性名．
	public static final String TAG = "method";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_TYPE = "type";

	private final String type;
	private final String name;

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	private Iterator<String> getParamNameIter(final List<ParameterInfo> params) {
		// "name1" "name2" "name3"...
		return new Iterator<String>() {

			Iterator<ParameterInfo> i = params.iterator();

			@Override
			public boolean hasNext(){
				return i.hasNext();
			}

			@Override
			public String next(){
				ParameterInfo child = i.next();
				return child.getType();
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException();
			}

		};
	}

	private Iterator<String> getParamTypeIter(final List<ParameterInfo> params) {
		// "type1" "type2" "type3"...
		return new Iterator<String>() {

			private Iterator<ParameterInfo> i = params.iterator();

			@Override
			public boolean hasNext(){
				return i.hasNext();
			}

			@Override
			public String next(){
				ParameterInfo child = i.next();
				return child.getName();
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException();
			}

		};
	}

	private Iterator<String> getParamTypeNameIter(final List<ParameterInfo> params) {
		// "type0 name0" "type1 name1" "type2 name2" "type3 name3"...
		return new Iterator<String>() {

			private Iterator<ParameterInfo> i = params.iterator();

			@Override
			public boolean hasNext(){
				return i.hasNext();
			}

			@Override
			public String next(){
				ParameterInfo child = i.next();
				return String.format("%s %s", child.getType(), child.getName());
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException();
			}

		};
	}

	private Iterator<String> getCombinedParamNameIter(
			final List<ParameterInfo> myParams, final List<ParameterInfo> otherParams) {
		/*
		 * otherの引数の数だけ，自分の引数の引数名を順に戻し続けます．
		 * 自分の引数がそれ以上無くなった場合は，"null"を戻します．
		 * これは，アスペクト生成時に置き換え先の関数との間で引数が一致しない場合の想定された挙動です．
		 */
		return new Iterator<String>() {

			private Iterator<ParameterInfo> myIter = myParams.iterator();
			private Iterator<ParameterInfo> othersIter = otherParams.iterator();

			@Override
			public boolean hasNext(){
				return othersIter.hasNext();
			}

			@Override
			public String next(){
				othersIter.next();
				if (myIter.hasNext()) {
					ParameterInfo child = myIter.next();
					return child.getName();
				} else {
					return "null";
				}
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException();
			}

		};
	}

	public MethodInfo(List<ParameterInfo> children, String type, String name) {
		super(children);
		this.type = type;
		this.name = name;
	}

	@Override
	public void generateAspect(StringWriter writer) {
		final IMethodInfo selected = getParent().getSelected();
		final String interfaceName = getParent().getParent().getName();

		// 選択が未定義なら何もしない
		if (selected == null) {
			return;
		}

		// 自分が選択されているなら何もしない
		if (selected.equals(this)) {
			return;
		}

		// 1行目
		writer.write(String.format(
				"@Around(\"execution(%2$s %1$s.%3$s(%4$s)) && this(__this) && args(%5$s)\")%n",
				interfaceName,
				type,
				name,
				Utility.join(getParamNameIter(getChildren()), ", "),
				Utility.join(getParamTypeIter(getChildren()), ", ")));

		// 2行目
		List<ParameterInfo> params = new ArrayList<>();
		params.add(new ParameterInfo(interfaceName, "__this"));
		params.addAll(getChildren());
		writer.write(String.format(
				"public %2$s %1$s(%3$s) {%n",
				name,
				selected.getType(),
				Utility.join(getParamTypeNameIter(params), ", ")));

		// 3行目
		if (selected instanceof MethodInfo) {
			final MethodInfo selectedMethod = (MethodInfo) selected;
			if (!type.equals("void")) {
				writer.write("return ");
			}
			writer.write(String.format(
					"__this.%1$s(%2$s);%n",
					selectedMethod.name,
					Utility.join(getCombinedParamNameIter(getChildren(), selectedMethod.getChildren()), ", ")));
		}

		// 4行目
		writer.write(String.format("}%n"));
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

	@Override
	public Object[] getTreeContentChildren() {
		return new Object[] {};
	}

	@Override
	public boolean hasTreeContentChildren() {
		return false;
	}

	/**
	 * Eclipseの補完機能のような書式で，メソッドの情報を表します．
	 */
	@Override
	public String getTreeContentLabel() {
		return String.format(
				"%s(%s) : %s",
				name,
				Utility.join(getParamTypeNameIter(getChildren()), ", "),
				type);
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
		result = prime * result
				+ ((getChildren() == null) ? 0 : getChildren().hashCode());
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
		if (!(obj instanceof MethodInfo))
			return false;
		MethodInfo other = (MethodInfo) obj;
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
		if (getChildren() == null) {
			if (other.getChildren() != null)
				return false;
		} else if (!getChildren().equals(other.getChildren()))
			return false;
		return true;
	}

}
