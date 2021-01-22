package xTestes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import Helpers.SWServiceInvoker;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class testeChamadaHTTP implements AcaoRotinaJava {
	SWServiceInvoker si = new SWServiceInvoker("http://localhost:8180", "GABRIEL", "gabriel123456");

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		String xmlAsString = "<entity name='Parceiro'>" + "<criterio nome='CODPARC' valor='1'/>" + "<fields>"
				+ "<field name='NOMEPARC'/>" + "<field name='ATIVO'/>" + "<field name='CLIENTE'/>" + "</fields>"
				+ "</entity>";

		String service = "CACSP.incluirNota";
		String modulo = "mgecom";
		String consultaProdutos = "<nota>\r\n" + "            <cabecalho>\r\n" + "                <NUNOTA/>\r\n"
				+ "                <TIPMOV>P</TIPMOV>\r\n" + "                <DTNEG>21/01/2021</DTNEG>\r\n"
				+ "                <CODTIPVENDA>50</CODTIPVENDA>\r\n" + "                <CODPARC>2</CODPARC>\r\n"
				+ "                <CODTIPOPER>2018</CODTIPOPER>\r\n" + "                <CODEMP>2</CODEMP>\r\n"
				+ "                <CODVEND>0</CODVEND>\r\n" + "                <CODCENCUS>1030000</CODCENCUS>\r\n"
				+ "                <CODNAT>130000</CODNAT>\r\n"
				+ "                <OBSERVACAO><![CDATA[PEDIDO TESTE]]></OBSERVACAO>\r\n"
				+ "            </cabecalho>\r\n" + "            <itens INFORMARPRECO=\"true\">\r\n"
				+ "                <item>\r\n" + "                    <NUNOTA/>\r\n"
				+ "                    <SEQUENCIA/>\r\n" + "                    <CODPROD>71</CODPROD>\r\n"
				+ "                    <CODVOL><![CDATA[UN]]></CODVOL>\r\n"
				+ "                    <CODLOCALORIG>1110</CODLOCALORIG>\r\n"
				+ "                    <VLRUNIT>1.2</VLRUNIT>\r\n" + "                    <QTDNEG>1</QTDNEG>\r\n"
				+ "                    <PERCDESC>0</PERCDESC>\r\n" + "                </item>\r\n"
				+ "            </itens>\r\n" + "        </nota>";

		// String xmlAsString = "<entity name='Cidade'><criterio nome='CODCID'
		// valor='11' /></entity>";
		Document call = si.call(service, modulo, consultaProdutos);

		NodeList list = call.getElementsByTagName("pk");

		for (int i = 0; i < list.getLength(); i++) {

			Node item = list.item(i);

			if (item.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) item;
				arg0.setMensagemRetorno("Nota: " + e.getElementsByTagName("NUNOTA").item(0).getTextContent());
			}
		}
	}
}
/*
 * for(int i=0; i<list.getLength(); i++) { Node item = list.item(i);
 * 
 * if (item.getNodeType()==Node.ELEMENT_NODE) { Element e = (Element) item;
 * NodeList childNodes = e.getChildNodes();
 * 
 * for(int j=0; j<childNodes.getLength();j++) { Node sub = childNodes.item(j);
 * 
 * if(sub.getNodeType()==Node.ELEMENT_NODE) { Element name = (Element) sub;
 * 
 * if(name.getTagName().contains("NOMEPARC")) {
 * arg0.setMensagemRetorno("Nome parceiro: "+name.getTextContent()); }
 * System.out.println("********* Elemento: "+
 * name.getTagName()+" - Valor: "+name.getTextContent()); }
 * 
 * }
 * 
 * }
 * 
 * } }
 */
