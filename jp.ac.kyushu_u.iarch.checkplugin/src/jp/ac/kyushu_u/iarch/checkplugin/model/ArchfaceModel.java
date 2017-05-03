package jp.ac.kyushu_u.iarch.checkplugin.model;

import jp.ac.kyushu_u.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu_u.iarch.basefunction.reader.ArchModel;

import org.eclipse.core.resources.IResource;

public class ArchfaceModel {
	public static Model getArchfaceModel(IResource archfile) {
		return new ArchModel(archfile).getModel();
	}
}
