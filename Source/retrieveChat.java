import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;


public class retrieveChat {
	retrieveChat(String name) {
		this.name = name;
		try {

			XmlFile = new File("../BackupChat/" + name + ".xml");
			if (XmlFile.exists()) {
				flag = 1;
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				XMLdoc = builder.parse(XmlFile);
				map = new LinkedHashMap<String, ArrayList<String>>();
			} else {
				flag = 0;
			}

		}

		catch (Exception err) {
			System.out.println(err.getMessage());
		}
	}
	public int getFlag() {
		return flag;
	}
	public LinkedHashMap<String, ArrayList<String>> restore() {
		Element etab = null;
		Element esub = null;
		NodeList Tabs = XMLdoc.getElementsByTagName("Tabs");
		for (int i = 0; i < Tabs.getLength(); i++) {
			Node tab = Tabs.item(i);
			if (tab.getNodeType() == Node.ELEMENT_NODE) {
				etab = (Element)tab;
				ArrayList<String> msg = new ArrayList<String>();
				NodeList nodes = etab.getChildNodes();
				for (int j = 0; j < nodes.getLength(); j++) {
					Node subnode = nodes.item(j);
					if (subnode.getNodeType() == Node.ELEMENT_NODE) {
						esub = (Element)subnode;
						msg.add(esub.getAttribute("from") + "~" + esub.getAttribute("text"));
					}
				}
				map.put(etab.getAttribute("TabName"), msg);
			}
		}
		XmlFile.delete();
		return map;
	}

	private File XmlFile;
	private Document XMLdoc;
	private String name;
	private LinkedHashMap<String, ArrayList<String>> map;
	private int flag;
}