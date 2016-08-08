package jp.ac.kyushu.iarch.checkplugin.model;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.revwalk.RevCommit;

public class GitDiff {
	public static enum UncertainType {
		UNDEFINED, OPTIONAL, ALTERNATIVE, CERTAIN
	};

	private DiffEntry diffEntry = null;
	private RawText textA = null;
	private RawText textB = null;
	private Edit edit = null;
	private RevCommit commitA = null;
	private RevCommit commitB = null;
	private UncertainType uncertainTypeA = UncertainType.UNDEFINED;
	private UncertainType uncertainTypeB = UncertainType.UNDEFINED;

	/**
	 * @param diffEntry
	 *            new diffEntry(commitA, commitB)
	 * @param textA
	 *            「時系列的に前の」RevText
	 * @param textB
	 *            「時系列的に後の」RevText
	 * @param edit
	 *            new Edit()
	 * @param commitA
	 *            「時系列的に前の」RevCommit
	 * @param commitB
	 *            「時系列的に後の」RevCommit
	 */
	public GitDiff(DiffEntry diffEntry, RawText textA, RawText textB, Edit edit, RevCommit commitA, RevCommit commitB) {
		this.diffEntry = diffEntry;
		this.textA = textA;
		this.textB = textB;
		this.edit = edit;
		this.setCommitA(commitA);
		this.setCommitB(commitB);
		this.uncertainTypeA = UncertainType.UNDEFINED;
		this.uncertainTypeB = UncertainType.UNDEFINED;
	}

	public String getDeletedCode() {
		return this.textA.getString(this.getEdit().getBeginA(), this.getEdit().getEndA(), false);
	}

	public String getInsertedCode() {
		return this.textB.getString(this.getEdit().getBeginB(), this.getEdit().getEndB(), false);
	}

	/**
	 * @return diffEntry
	 */
	public DiffEntry getDiffEntry() {
		return diffEntry;
	}

	/**
	 * @param diffEntry
	 *            セットする diffEntry
	 */
	public void setDiffEntry(DiffEntry diffEntry) {
		this.diffEntry = diffEntry;
	}

	/**
	 * @return textA
	 */
	public RawText getTextA() {
		try {
			return textA;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param textA
	 *            セットする textA
	 */
	public void setTextA(RawText textA) {
		this.textA = textA;
	}

	/**
	 * @return textB
	 */
	public RawText getTextB() {
		try {
			return textB;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param textB
	 *            セットする textB
	 */
	public void setTextB(RawText textB) {
		this.textB = textB;
	}

	/**
	 * @return edit
	 */
	public Edit getEdit() {
		try {
			return edit;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param edit
	 *            セットする edit
	 */
	public void setEdit(Edit edit) {
		this.edit = edit;
	}

	public RevCommit getCommitA() {
		return commitA;
	}

	public void setCommitA(RevCommit commitA) {
		this.commitA = commitA;
	}

	public RevCommit getCommitB() {
		return commitB;
	}

	public void setCommitB(RevCommit commitB) {
		this.commitB = commitB;
	}

	public UncertainType getUncertainTypeA(){
		return uncertainTypeA;
	}

	public UncertainType getUncertainTypeB() {
		return uncertainTypeB;
	}

	public String getUncertainStrTypeA() {
		switch (this.uncertainTypeA) {
		case UNDEFINED:
			return "UNDEFINED";
		case OPTIONAL:
			return "OPTIONAL";
		case ALTERNATIVE:
			return "ALTERNATIVE";
		case CERTAIN:
			return "CERTAIN";
		default:
			return null;
		}
	}

	public String getUncertainStrTypeB() {
		switch (this.uncertainTypeB) {
		case UNDEFINED:
			return "UNDEFINED";
		case OPTIONAL:
			return "OPTIONAL";
		case ALTERNATIVE:
			return "ALTERNATIVE";
		case CERTAIN:
			return "CERTAIN";
		default:
			return null;
		}
	}

	public void setUncertainTypeA(UncertainType uncertainType) {
		this.uncertainTypeA = uncertainType;
	}

	public void setUncertainTypeB(UncertainType uncertainType) {
		this.uncertainTypeB = uncertainType;
	}

}
