package jp.ac.kyushu.iarch.checkplugin.testsupport;

import org.eclipse.core.resources.IFile;

/**
 * IFileを受け取り，SelectionInfoを生成する抽象的なFactoryです．
 * @author watanabeke
 */
public interface IInfoGenerateReader {

	/**
	 * fileの内容に対応するSelectionInfoを生成します．
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public SelectionInfo read(IFile file) throws Exception;

}
