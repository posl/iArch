package jp.ac.kyushu.iarch.checkplugin.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentMethodPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.GitDiff;
import jp.ac.kyushu.iarch.checkplugin.model.GitDiff.UncertainType;

import org.eclipse.core.resources.IProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitUtils {

	private GitUtils() {
		// util class
	}

	// blobIdが持つファイルの情報を持つRawTextインスタンスを返すメソッド
	private static RawText readText(AbbreviatedObjectId blobId, ObjectReader reader) throws IOException {
		ObjectLoader oldLoader = reader.open(blobId.toObjectId(), Constants.OBJ_BLOB);
		return new RawText(oldLoader.getCachedBytes());
	}

	public static boolean isEnableGitRepository(IProject proj){
		String gitPath = proj.getLocationURI().getPath() + "/.git";
		File gitFolder = new File(gitPath);
		// no git folder simply
		if(!gitFolder.exists()){
			return false;
		}
		try {
			Repository repo = new FileRepository(gitFolder);
			if(repo.getRepositoryState().canResetHead()){
				return true;
			}
		} catch (IOException e) {
			// some error about git
			e.printStackTrace();
		}
		return false;
	}

	public static UncertainType getDiffUncertainType(String code, ComponentMethodPairModel methodModel, GitDiff diff) {
		UncertainType uncertainType = UncertainType.UNDEFINED;
		Pattern fetchOptMethodPattern = Pattern.compile("\\[.*" + methodModel.getName() + ".*\\]");
		Matcher optMatcher = fetchOptMethodPattern.matcher(code);
		Pattern fetchAltMethodPattern = Pattern.compile("\\{.*" + methodModel.getName() + ".*\\}");
		Matcher altMatcher = fetchAltMethodPattern.matcher(code);
		if (optMatcher.find()) {
			uncertainType = UncertainType.OPTIONAL;
		} else if (altMatcher.find()) {
			uncertainType = UncertainType.ALTERNATIVE;
		} else {
			uncertainType = UncertainType.CERTAIN;
		}
		return uncertainType;
	}

	public static ArrayList<GitDiff> getSingleMethodDiffList(ArrayList<GitDiff> diffList, ComponentMethodPairModel methodModel) {
		ArrayList<GitDiff> methodDiffs = new ArrayList<GitDiff>();
		for (GitDiff diff : diffList) {
			String deletedCode = diff.getTextA().getString(diff.getEdit().getBeginA(), diff.getEdit().getEndA(), false);
			String insertedCode = diff.getTextB().getString(diff.getEdit().getBeginB(), diff.getEdit().getEndB(), false);
			Pattern fetchMethodPattern = Pattern.compile(".*" + methodModel.getName() + ".*");
			Matcher insertMatcher = fetchMethodPattern.matcher(insertedCode);
			Matcher deleteMatcher = fetchMethodPattern.matcher(deletedCode);
			UncertainType insertedUncertainType = getDiffUncertainType(insertedCode, methodModel, diff);
			UncertainType deletedUncertainType = getDiffUncertainType(deletedCode, methodModel, diff);
			boolean isUncertain = false;
			if (insertMatcher.find()) {
				if (insertedUncertainType == UncertainType.OPTIONAL || insertedUncertainType == UncertainType.ALTERNATIVE) {
					diff.setUncertainTypeB(insertedUncertainType);
					isUncertain = true;
				} else {
					diff.setUncertainTypeB(UncertainType.CERTAIN);
				}
			}
			if (deleteMatcher.find()) {
				if (deletedUncertainType == UncertainType.OPTIONAL || deletedUncertainType == UncertainType.ALTERNATIVE) {
					diff.setUncertainTypeA(deletedUncertainType);
					isUncertain = true;
				} else {
					diff.setUncertainTypeA(UncertainType.CERTAIN);
				}
			}
			if (isUncertain) {
				methodDiffs.add(diff);
			}
		}
		return methodDiffs;
	}

	public static ArrayList<GitDiff> getAllDiffList(IProject proj) {
		String projectPath = proj.getLocationURI().getPath();
		ArrayList<GitDiff> diffList = new ArrayList<GitDiff>();

		try {
			Repository repo = new RepositoryBuilder().findGitDir(new File(projectPath)).readEnvironment().findGitDir().build();
			Git git = new Git(repo);
			DiffAlgorithm diffAlgorithm = DiffAlgorithm.getAlgorithm(repo.getConfig()
					.getEnum(ConfigConstants.CONFIG_DIFF_SECTION, null, ConfigConstants.CONFIG_KEY_ALGORITHM,
							SupportedAlgorithm.HISTOGRAM));
			ObjectReader reader = repo.newObjectReader();

			String archifilePath = new XMLreader(proj).getArchfileResource().getProjectRelativePath().toString();
			Iterator<RevCommit> gitLog = git.log().add(repo.resolve(Constants.HEAD)).addPath(archifilePath).all().call()
					.iterator();
			DiffFormatter diffFormatter = new DiffFormatter(System.out);
			// TODO setPathFilterによってArchifileのみのDiffを取得する
			// diffFormatter.setPathFilter(filter);
			diffFormatter.setRepository(repo);
			RevCommit currentCommit = null;
			AnyObjectId prevObjectId = null;
			AnyObjectId currentObjectId = null;

			if (gitLog.hasNext()) {
				currentCommit = gitLog.next();
				currentObjectId = currentCommit.toObjectId();
			}
			while (gitLog.hasNext()) {
				for (RevCommit prevCommit : currentCommit.getParents()) {
					prevObjectId = prevCommit.toObjectId();
					List<DiffEntry> list = diffFormatter.scan(prevObjectId, currentObjectId);
					EditList editList = new EditList();
					for (DiffEntry diffEntry : list) {
						if (diffEntry.getChangeType() != DiffEntry.ChangeType.DELETE
								&& diffEntry.getChangeType() != DiffEntry.ChangeType.ADD) {
							if (diffEntry.getNewPath().equals(archifilePath)) {
								RawText oldText = readText(diffEntry.getOldId(), reader);
								RawText newText = readText(diffEntry.getNewId(), reader);
								editList = diffAlgorithm.diff(RawTextComparator.DEFAULT, oldText, newText);
								for (Edit edit : editList) {
									GitDiff diff = new GitDiff(diffEntry, oldText, newText, edit, prevCommit, currentCommit);
									diffList.add(diff);
								}
							}
						} else if (diffEntry.getChangeType() == DiffEntry.ChangeType.ADD
								&& diffEntry.getNewPath().equals(archifilePath)) {
							RawText newText = readText(diffEntry.getNewId(), reader);
							GitDiff diff = new GitDiff(diffEntry, null, newText, null, prevCommit, currentCommit);
							diffList.add(diff);
							// System.out.println(diffEntry.getNewPath() + " "
							// + diffEntry.getChangeType());
						} else if (diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE
								&& diffEntry.getOldPath().equals(archifilePath)) {
							RawText oldText = readText(diffEntry.getOldId(), reader);
							GitDiff diff = new GitDiff(diffEntry, oldText, null, null, prevCommit, currentCommit);
							diffList.add(diff);
							// System.out.println(diffEntry.getOldPath() + " "
							// + diffEntry.getChangeType());
						}
					}
				}
				// System.out.println();
				currentCommit = gitLog.next();
				currentObjectId = currentCommit.toObjectId();
			}
			return diffList;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoHeadException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return null;
	}
}
