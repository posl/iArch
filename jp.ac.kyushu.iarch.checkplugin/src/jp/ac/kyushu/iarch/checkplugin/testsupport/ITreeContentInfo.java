package jp.ac.kyushu.iarch.checkplugin.testsupport;

public interface ITreeContentInfo extends IInfo {

	public Object[] getTreeContentChildren();

	public boolean hasTreeContentChildren();

	public String getTreeContentLabel();

}
