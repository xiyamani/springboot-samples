package cn.training.erp;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;

public final class XmlSupport {
    public static final String SOAP="http://schemas.xmlsoap.org/soap/envelope/";
    public static final String MOM="urn:training:mom";
    private XmlSupport(){}
    public static Document parse(String xml){
        try{
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        }catch(Exception e){throw new IllegalArgumentException("XML 报文不符合接口约定",e);}
    }
    public static String field(Document document,String name){
        var nodes=document.getElementsByTagNameNS("*",name);
        if(nodes.getLength()==0)return"";
        return nodes.item(0).getTextContent();
    }
    public static String envelope(String operation,List<Map<String,Object>> rows){
        try{
            Document d=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element envelope=d.createElementNS(SOAP,"soap:Envelope");d.appendChild(envelope);
            Element body=d.createElementNS(SOAP,"soap:Body");envelope.appendChild(body);
            Element result=d.createElementNS(MOM,"m:"+operation);body.appendChild(result);
            for(var row:rows){
                Element item=d.createElementNS(MOM,"m:Record");result.appendChild(item);
                for(var entry:row.entrySet()){
                    Element value=d.createElementNS(MOM,"m:"+entry.getKey());
                    value.setTextContent(String.valueOf(entry.getValue()));item.appendChild(value);
                }
            }
            TransformerFactory factory=TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,"");
            Transformer transformer=factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
            StringWriter writer=new StringWriter();
            transformer.transform(new DOMSource(d),new StreamResult(writer));return writer.toString();
        }catch(Exception e){throw new IllegalStateException("XML 生成失败",e);}
    }
    public static List<Map<String,Object>> records(String xml){
        Document d=parse(xml);
        ErpService.require(d.getElementsByTagNameNS(SOAP,"Fault").getLength()==0,"制造接口返回异常");
        NodeList nodes=d.getElementsByTagNameNS(MOM,"Record");
        List<Map<String,Object>> result=new ArrayList<>();
        for(int i=0;i<nodes.getLength();i++){
            Map<String,Object> row=new LinkedHashMap<>();NodeList fields=nodes.item(i).getChildNodes();
            for(int j=0;j<fields.getLength();j++){
                Node field=fields.item(j);if(field instanceof Element)row.put(field.getLocalName(),field.getTextContent());
            }
            result.add(row);
        }
        return result;
    }
}
