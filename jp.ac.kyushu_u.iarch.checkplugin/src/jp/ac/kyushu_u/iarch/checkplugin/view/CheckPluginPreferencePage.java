package jp.ac.kyushu_u.iarch.checkplugin.view;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import jp.ac.kyushu_u.iarch.checkplugin.CheckPluginPreference;

/**
 * @author nakazato
 *
 */
public class CheckPluginPreferencePage
extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(CheckPluginPreference.getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(new IntegerFieldEditor(CheckPluginPreference.PROP_ANALYSIS_DEPTH,
				"Analysis depth", parent));
	}

	@Override
	public boolean performOk() {
		IPreferenceStore ps = getPreferenceStore();
		System.out.println(ps);
		if (ps != null) {
			int ad = ps.getInt(CheckPluginPreference.PROP_ANALYSIS_DEPTH);
			System.out.println(ad);
		}
		return super.performOk();
	}
}
