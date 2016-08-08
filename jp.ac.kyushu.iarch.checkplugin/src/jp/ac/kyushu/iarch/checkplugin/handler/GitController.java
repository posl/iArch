/**
 *
 */
package jp.ac.kyushu.iarch.checkplugin.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentMethodPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.GitDiff;
import jp.ac.kyushu.iarch.checkplugin.model.GitDiff.UncertainType;
import jp.ac.kyushu.iarch.checkplugin.utils.GitUtils;

import org.eclipse.core.resources.IProject;

/**
 * @author fukamachi 対象プロジェクトのGitの情報を取得し，保存，および閲覧用のモデルを準備するためのコントローラ
 */
public class GitController {

	private IProject proj = null;
	private ArrayList<GitDiff> diffList = null;
	private ArrayList<ComponentClassPairModel> classPairs = null;

	public GitController(IProject proj, List<ComponentClassPairModel> classPairs) {
		this.proj = proj;
		this.diffList = new ArrayList<GitDiff>();
		this.classPairs = (ArrayList<ComponentClassPairModel>) classPairs;
	}

	public void checkGitInfo() {
		this.diffList = GitUtils.getAllDiffList(proj);
		insertDifftoPairModel();
	}

	private void insertDifftoPairModel() {
		for (ComponentClassPairModel classPair : classPairs) {
			for (ComponentMethodPairModel methodModel : classPair.getMethodPairsList()) {
				for (GitDiff diff : diffList) {
					String deletedCode = diff.getTextA().getString(diff.getEdit().getBeginA(), diff.getEdit().getEndA(), false);
					String insertedCode = diff.getTextB().getString(diff.getEdit().getBeginB(), diff.getEdit().getEndB(), false);
					Pattern fetchMethodPattern = Pattern.compile(".*" + methodModel.getName() + ".*");
					Matcher insertMatcher = fetchMethodPattern.matcher(insertedCode);
					Matcher deletedMatcher = fetchMethodPattern.matcher(deletedCode);
					UncertainType insertedUncertainType = UncertainType.UNDEFINED;
					UncertainType deletedUncertainType = UncertainType.UNDEFINED;
					if (insertMatcher.find()) {
						insertedUncertainType = GitUtils.getDiffUncertainType(insertedCode, methodModel, diff);
						diff.setUncertainTypeB(insertedUncertainType);
					}
					if (deletedMatcher.find()) {
						deletedUncertainType = GitUtils.getDiffUncertainType(deletedCode, methodModel, diff);
						diff.setUncertainTypeA(deletedUncertainType);
					}
					if (insertedUncertainType == UncertainType.OPTIONAL || deletedUncertainType == UncertainType.OPTIONAL
							|| insertedUncertainType == UncertainType.ALTERNATIVE
							|| deletedUncertainType == UncertainType.ALTERNATIVE) {
						methodModel.setRecentDiff(diff);
						break;
					}
				}
			}
		}
	}

	/**
	 * @return diffList
	 */
	public ArrayList<GitDiff> getDiffList() {
		return diffList;
	}

	/**
	 * @param diffList
	 *            セットする diffList
	 */
	public void setDiffList(ArrayList<GitDiff> diffList) {
		this.diffList = diffList;
	}

}
