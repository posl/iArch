package jp.ac.kyushu_u.iarch.checkplugin.testsupport;

import java.util.List;
import java.util.Map;

/**
 * Aspect生成用の中間オブジェクトです。
 * Conponent、Connector、Alternative、Optional関係なく全てこれで表します。
 * @author watanabeke
 *
 */
public class UncertaintyBean {
	
	public UncertaintyBean() {}
	
	private String label;
	private List<MethodBean> whereCalled;
	private List<IMethodBean> methods;
	private Map<String, String> importClasses;
	
	public List<MethodBean> getWhereCalled() {
		return whereCalled;
	}
	public void setWhereCalled(List<MethodBean> whereCalled) {
		this.whereCalled = whereCalled;
	}
	public List<IMethodBean> getMethods() {
		return methods;
	}
	public void setMethods(List<IMethodBean> methods) {
		this.methods = methods;
	}
	@Override
	public String toString() {
		return "UncertaintyBean [whereCalled=" + whereCalled + ", methods=" + methods + "]";
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public Map<String, String> getImportClasses() {
		return importClasses;
	}
	public void setImportClasses(Map<String, String> importClasses) {
		this.importClasses = importClasses;
	}

}
