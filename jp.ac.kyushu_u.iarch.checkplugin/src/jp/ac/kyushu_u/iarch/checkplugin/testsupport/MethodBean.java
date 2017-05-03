package jp.ac.kyushu_u.iarch.checkplugin.testsupport;

import java.util.List;

/**
 * Aspect生成用の中間オブジェクトです。
 * クラス名や重みを含むメソッドの情報を保持します。
 * @author watanabeke
 *
 */
public class MethodBean implements IMethodBean {
	
	public MethodBean() {}
	
	private String packageName;
	private String className;
	private String type;
	private String name;
	private List<ParamBean> params;
	private String label;
	
	private boolean isStatic;
	private double weight;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<ParamBean> getParams() {
		return params;
	}
	public void setParams(List<ParamBean> params) {
		this.params = params;
	}
	public boolean getIsStatic() {
		return isStatic;
	}
	public void setIsStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	@Override
	public String toString() {
		return "MethodBean [className=" + className + ", type=" + type + ", name=" + name + ", params=" + params
				+ ", isStatic=" + isStatic + ", weight=" + weight + "]";
	}
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public boolean getIsEmpty() {
		return false;
	}

}
