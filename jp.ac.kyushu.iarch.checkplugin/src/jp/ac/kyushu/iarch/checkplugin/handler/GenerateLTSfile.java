package jp.ac.kyushu.iarch.checkplugin.handler;

import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import behavior.Message;
import jp.ac.kyushu.iarch.archdsl.archDSL.Behavior;
import jp.ac.kyushu.iarch.archdsl.archDSL.Connector;
import jp.ac.kyushu.iarch.archdsl.archDSL.Interface;
import jp.ac.kyushu.iarch.archdsl.archDSL.Method;
import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.ProjectReader;
import jp.ac.kyushu.iarch.checkplugin.model.BehaviorPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.CallPairModel;
import jp.ac.kyushu.iarch.checkplugin.model.ComponentClassPairModel;
import jp.ac.kyushu.iarch.checkplugin.utils.ProjectSelectionUtils;
import jp.ac.kyushu.iarch.checkplugin.view.SelectArchfile;


public class GenerateLTSfile implements IHandler{

		@Override
		public void addHandlerListener(IHandlerListener handlerListener) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			IProject project = ProjectSelectionUtils.getProject(event,"Generate LTS file");
			SelectArchfile archfile = new SelectArchfile(HandlerUtil.getActiveShell(event),project);
			if(archfile.open() == MessageDialog.OK){
			GenerateLTSCode(archfile.getArchiface());
			}
			return null;
		}

		void GenerateLTSCode(IResource archcode){
			String code = "";
			code += getSequenceCode(archcode);	
			String projectPath = archcode.getProject().getLocation().toOSString();
			String ltsCodeFile = projectPath + "/Gen-LTS.lts";
			File myFilePath = new File(ltsCodeFile);
			try{
				if(!myFilePath.exists()){
					myFilePath.createNewFile();
				}
				FileWriter resultFile = new FileWriter(myFilePath);
				PrintWriter myFile = new PrintWriter(resultFile);
				myFile.print(code);
				resultFile.close();
			}catch(Exception e){
				System.out.println("Create file error");
				e.printStackTrace();
			}
		}
		
		private String getSequenceCode(IResource archcode){
			String SequenceCode = "";
			ArchModel archmodel = new ArchModel(archcode);
			Model archModel = archmodel.getModel();
			ConnectorToFSP cfsp = new ConnectorToFSP();
			SequenceCode = cfsp.convert(archModel);
			return SequenceCode;
		}
		
		@Override
		public boolean isEnabled() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean isHandled() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void removeHandlerListener(IHandlerListener handlerListener) {
			// TODO Auto-generated method stub
			
		}
	
}



