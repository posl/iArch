package jp.ac.kyushu.iarch.checkplugin.testsupport;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * テストインスタンスの情報を保持します．
 * このクラスが階層のルートになります．
 * @author watanabeke
 */
public class SelectionInfo extends AbstractInnerInfo<InterfaceInfo>
		implements ITreeContentInfo, IAspectGeneratable, IXMLGeneratable {

	// XMLでのタグ名
	public static final String TAG = "selection";

	// パフォーマンスのためにchildrenとは別に参照を持ちます．
	// 名前をキーとします．追加順を維持するマップを使用してください．
	private final HashMap<String, InterfaceInfo> nameMap = new HashMap<>();

	public SelectionInfo(List<InterfaceInfo> children) {
		super(children);
		for (InterfaceInfo child : children) {
			nameMap.put(child.getName(), child);
		}
	}

	/**
	 * otherで選択されているメソッドについて，自分に対応するメソッドがあれば，自分もそのメソッドを選択します．
	 * ただし，同じ不確かさの中に対応するメソッドが複数存在する場合は，自分の選択は変更しないものとします．
	 * ここで対応するとは，インターフェイス名・メソッド型・メソッド名・引数型・引数名が順序を含めすべて等しいことです．
	 * @param other
	 */
	public void update(SelectionInfo other) {
		for (String name : other.nameMap.keySet()) {
			InterfaceInfo myChild = nameMap.get(name);
			// otherのキーが自分にもあれば
			if (myChild != null) {
				InterfaceInfo othersChild = other.nameMap.get(name);
				assert othersChild != null;
				myChild.update(othersChild);
			}
		}
	}

	/**
	 * テストインスタンスを反映してメソッドの処理を適切に置き換えるようなアスペクトを生成します．
	 * @return AspectJによるアスペクトのコード
	 * @throws IOException
	 */
	public String generateAspect(final String aspectName) throws IOException {
		StringWriter writer = new StringWriter();
		writer.write(String.format(
				"import org.aspectj.lang.annotation.*;%n" +
						"@Aspect%n" +
						"public class %s {%n", aspectName));
		this.generateAspect(writer);
		writer.write(String.format("}%n"));
		writer.flush();
		String result = writer.toString();
		writer.close();
		result = Utility.formatJavaCode(result);
		return result;
	}

	/**
	 * ConfigReaderの書式に準ずるXMLを生成します．
	 * @return XMLのコード
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	public String generateXML() throws XMLStreamException, IOException {
		String result = null;
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		StringWriter stringWriter = new StringWriter();
		XMLStreamWriter writer;
		writer = factory.createXMLStreamWriter(stringWriter);
		this.generateXML(writer);
		writer.flush();
		result = stringWriter.toString();
		writer.close();
		stringWriter.close();
		result = Utility.formatXMLCode(result);
		return result;
	}

	@Override
	protected String getXMLTag() {
		return TAG;
	}

	@Override
	protected List<List<String>> getXMLAttrIter() {
		return Arrays.asList();
	}

	@Override
	public Object[] getTreeContentChildren() {
		return getChildren().toArray();
	}

	@Override
	public boolean hasTreeContentChildren() {
		return !getChildren().isEmpty();
	}

	@Override
	public String getTreeContentLabel() {
		throw new UnsupportedOperationException();
	}

}
