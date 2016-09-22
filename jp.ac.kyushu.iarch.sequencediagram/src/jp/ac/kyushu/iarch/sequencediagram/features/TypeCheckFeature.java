package jp.ac.kyushu.iarch.sequencediagram.features;

import java.util.List;

import jp.ac.kyushu.iarch.archdsl.archDSL.Model;
import jp.ac.kyushu.iarch.basefunction.model.ConnectorTypeCheckModel;
import jp.ac.kyushu.iarch.basefunction.model.ConnectorTypeCheckModel.BehaviorModel;
import jp.ac.kyushu.iarch.basefunction.model.ConnectorTypeCheckModel.CallModel;
import jp.ac.kyushu.iarch.basefunction.reader.ArchModel;
import jp.ac.kyushu.iarch.basefunction.reader.XMLreader;
import jp.ac.kyushu.iarch.basefunction.utils.PlatformUtils;
import jp.ac.kyushu.iarch.basefunction.utils.ProblemViewManager;
import jp.ac.kyushu.iarch.sequencediagram.utils.MessageUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.pictograms.Diagram;

import behavior.Actor;
import behavior.AlternativeMessage;
import behavior.Message;
import behavior.OptionalMessage;

public class TypeCheckFeature extends AbstractCustomFeature {
	public TypeCheckFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public String getName() {
		return "Type Check";
	}

	@Override
	public String getDescription() {
		return "Performs type check.";
	}

	@Override
	public boolean canExecute(ICustomContext context) {
		return true;
	}

	@Override
	public void execute(ICustomContext context) {
		// Get file of class diagram.
		IFile diagramFile = PlatformUtils.getActiveFile();

		// Get Archface model within the project.
		IProject project = diagramFile != null ? diagramFile.getProject() : null;
		if (project == null) {
			System.out.println("TypeCheckFeature: failed to get active project.");
			return;
		}
		IResource archfile = new XMLreader(project).getArchfileResource();
		ArchModel archModel = new ArchModel(archfile);
		Model model = archModel.getModel();

		doTypeCheck(diagramFile, model, getDiagram());
	}

	private static class MessageCallModel extends CallModel {
		private boolean certain;
		private boolean optional;
		private boolean alternative;

		private MessageCallModel(Message message) {
			this.certain = false;
			this.optional = false;
			this.alternative = false;

			if (message instanceof AlternativeMessage) {
				this.alternative = true;
				boolean first = true;
				for (Message m : ((AlternativeMessage) message).getMessages()) {
					setMessage(m, first);
					first = false;
				}
			} else {
				if (message instanceof OptionalMessage) {
					this.optional = true;
				} else {
					this.certain = true;
				}
				setMessage(message, true);
			}
		}
		private boolean setMessage(Message message, boolean setClassName) {
			behavior.Object bObj = MessageUtils.getReceivingObject(message);
			if (bObj != null) {
				methodNames.add(message.getName());
				if (setClassName) {
					className = bObj.getName();
				}
				return true;
			} else {
				return false;
			}
		}

		@Override
		public boolean isCertain() {
			return certain;
		}
		@Override
		public boolean isOptional() {
			return optional;
		}
		@Override
		public boolean isAlternative() {
			return alternative;
		}
	}

	private void doTypeCheck(IFile diagramFile, Model model, Diagram diagram) {
		// TODO: Change marker type to unique one so as not to remove by other checkers.
		ProblemViewManager problemViewManager = ProblemViewManager.getInstance();
		// Remove previously added markers.
		problemViewManager.removeProblems(diagramFile, false);

		// Collect ordered Messages from diagram.
		List<Message> messages = MessageUtils.collectMessages(diagram);

		// Create behavior model from Messages
		BehaviorModel diagramBm = new BehaviorModel();
		for (Message message : messages) {
			// Check if message is not received by Actor.
			if (checkMessage(message)) {
				MessageCallModel mcm = new MessageCallModel(message);
				diagramBm.add(mcm);
//				System.out.println(mcm);
			} else {
				String msg = "Ignored a message received by Actor.";
				problemViewManager.createWarningMarker(diagramFile, msg, message.getName());

				System.out.println("WARNING: Message is ignored since it is received by Actor: " + message.getName());
			}
		}

		// Compare with behavior model on Archmodel.
		List<ConnectorTypeCheckModel> ctcModels =
				ConnectorTypeCheckModel.getTypeCheckModel(model, false);
		boolean foundBehavior = false;
		for (ConnectorTypeCheckModel ctcModel : ctcModels) {
			for (BehaviorModel bm : ctcModel.getBehaviorModels()) {
				if (bm.sameBehavior(diagramBm)) {
					System.out.println("INFO: Sequence is defined: " + ctcModel.getConnectorName());
					foundBehavior = true;
				}
			}
		}
		if (!foundBehavior) {
			String msg = "Sequence is not defined in Archcode.";
			problemViewManager.createErrorMarker(diagramFile, msg, "-");

			System.out.println("ERROR: Sequence is not defined in Archcode.");
		}
	}
	private boolean checkMessage(Message message) {
		if (message instanceof AlternativeMessage) {
			return checkMessage(((AlternativeMessage) message).getMessages().get(0));
		}
		behavior.Object bObj = MessageUtils.getReceivingObject(message);
		return (bObj != null) && !(bObj instanceof Actor);
	}

	@Override
	public boolean hasDoneChanges() {
		// If results of type check modify objects, it should return true.
		return false;
	}
}
