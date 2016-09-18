import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import java.util.ArrayList;

public class BackUPChat {

	BackUPChat(String Name) {
		name = Name;
		try {
			new File("../BackupChat").mkdir();
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			XMLdoc = builder.newDocument();
			chat = XMLdoc.createElement("Chat");
			XMLdoc.appendChild(chat);
		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}
	}

	public void addTab(String Name, ArrayList<String[]> msg) {
		Element Tabs = XMLdoc.createElement("Tabs");
		chat.appendChild(Tabs);
		Tabs.setAttribute("TabName", Name);
		for (String[] m : msg) {
			Element MSG = XMLdoc.createElement("Msg");
			MSG.setAttribute("from", m[0]);
			MSG.setAttribute("text", m[1]);
			Tabs.appendChild(MSG);
		}

	}

	public void Save() {
		try {

			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			DOMSource XMLSource = new DOMSource(XMLdoc);
			StreamResult XMLfile = new StreamResult(new File("../BackupChat/" + name + ".xml"));
			transformer.transform(XMLSource, XMLfile);
		}

		catch (Exception err) {
			System.err.println(err.getMessage());
		}

	}


	private String name;
	private Document XMLdoc;
	private Element chat;
}