package top.vergessen.pixiv.util;

import org.jsoup.nodes.Document;
import org.seimicrawler.xpath.JXDocument;
import org.seimicrawler.xpath.JXNode;

import java.util.List;

// Xpath解析器
public class XpathUtil {

    private JXDocument jxDocument;

    public XpathUtil(Document doc){
        jxDocument = JXDocument.create(doc);
    }

    public List<JXNode> xpath(String xpath){
        List<JXNode> jxNodes = jxDocument.selN(xpath);
        return jxNodes;
    }

    public JXNode xpathOne(String xpath){
        return jxDocument.selNOne(xpath);
    }
}
