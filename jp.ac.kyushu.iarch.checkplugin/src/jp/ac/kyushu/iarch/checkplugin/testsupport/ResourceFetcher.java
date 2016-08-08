package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.util.ArrayList;
import java.util.List;

import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class ResourceFetcher {

	public static final String CONFIG_SUFFIX = ".xml";
	public static final String CONFIG_FOLDER = "test-support";
	public static final String ASPECT_NAME = "SelectionAspect";
	public static final String ASPECT_PATH = String.format("src/%s.java", ASPECT_NAME);

	private static final IWorkspaceRoot root;
	private IProject project;

	static {
		root = ResourcesPlugin.getWorkspace().getRoot();
	}

	public ResourceFetcher(IProject project) {
		this.project = project;
	}

	/**
	 * 現在のプロジェクトを取得します。
	 */
	static public IProject fetchProject() {
		try {
			return ProjectReader.getProject();
		} catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * プロジェクト名を取得します。
	 */
	public String getProjectName() {
		return project.getName();
	}

	/**
	 * archfaceを取得します。
	 */
	public IFile fetchArchFile() {
		return (IFile)(new XMLreader(project).getArchfileResource());
	}

	/**
	 * 構成フォルダを作成します。
	 * 既に存在する場合はそれを返します。
	 * @throws CoreException
	 */
	private IFolder createConfigFolder() {
		IFolder configFolder = project.getFolder(CONFIG_FOLDER);
		if (!configFolder.exists()) {
			try {
				configFolder.create(false, true, null);
			} catch (CoreException e) {
				// おそらく発生しないので
				throw new RuntimeException(e);
			}
		}
		return configFolder;
	}

	/**
	 * 与えられた名前を持つ、新たな構成ファイルを返します。
	 * @param name 拡張子を含まないファイル名
	 */
	public IFile fetchConfigFile(String name) {
		IFolder configFolder = createConfigFolder();
		IFile configFile = configFolder.getFile(String.format(
				"%s%s", name, CONFIG_SUFFIX));
		return configFile;
	}

	/**
	 * 構成フォルダ内の構成ファイルをすべて返します。
	 * 現在の実装は、フィルタせずフォルダ内の全てのファイルを返します。
	 * @throws CoreException
	 */
	public List<IFile> fetchConfigFiles() {
		List<IFile> configFiles = new ArrayList<>();
		IFolder configFolder = createConfigFolder();
		IResource[] members;
		try {
			members = configFolder.members();
		} catch (CoreException e) {
			// おそらく発生しないので
			throw new RuntimeException(e);
		}
		for (IResource member : members) {
			if (member instanceof IFile) {
				configFiles.add((IFile)member);
			}
		}
		return configFiles;
	}

	/**
	 * アスペクトを保存するファイルを返します。
	 */
	public IFile fetchAspectFile() {
		return root.getFile(project.getFullPath().append(ASPECT_PATH));
	}

}
