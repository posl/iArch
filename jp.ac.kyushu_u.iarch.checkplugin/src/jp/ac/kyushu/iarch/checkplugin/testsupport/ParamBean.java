package jp.ac.kyushu.iarch.checkplugin.testsupport;

/**
 * Aspect生成用の中間オブジェクトです。
 * メソッドの引数を表しています。
 * @author watanabeke
 *
 */
public class ParamBean {
	
	public ParamBean() {}
	
	private String type;
	private String name;
	
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
	@Override
	public String toString() {
		return "ParamBean [type=" + type + ", name=" + name + "]";
	}

}
