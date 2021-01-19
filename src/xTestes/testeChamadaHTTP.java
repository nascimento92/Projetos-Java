package xTestes;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

import Helpers.SWServiceInvoker;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;


public class testeChamadaHTTP implements AcaoRotinaJava {
	SWServiceInvoker si = new SWServiceInvoker("http://localhost:8180", "GABRIEL", "gabriel123456");
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		
		
		String xmlAsString = "<entity name='Parceiro'>"
				+ "<criterio nome='CODPARC' valor='1'/>"
				+ "<fields>"
				+ "<field name='NOMEPARC'/>"
				+ "<field name='ATIVO'/>"
				+ "<field name='CLIENTE'/>"
				+ "</fields>"
				+ "</entity>";
		
		//String xmlAsString  = "<entity name='Cidade'><criterio nome='CODCID' valor='11' /></entity>";
		Document call = si.call("crud.find", "mge", xmlAsString);
		
		NodeList list = call.getElementsByTagName("entidade");
		
		for(int i=0; i<list.getLength(); i++) {
			Node item = list.item(i);
			
			if (item.getNodeType()==Node.ELEMENT_NODE) {
				Element e = (Element) item;
				NodeList childNodes = e.getChildNodes();
				
				for(int j=0; j<childNodes.getLength();j++) {
					Node sub = childNodes.item(j);
					
					if(sub.getNodeType()==Node.ELEMENT_NODE) {
						Element name = (Element) sub;
						
						if(name.getTagName().contains("NOMEPARC")) {
							arg0.setMensagemRetorno("Nome parceiro: "+name.getTextContent());
						}
						System.out.println("********* Elemento: "+ name.getTagName()+" - Valor: "+name.getTextContent());
					}
					
				}
				
			}
			
		}
	}
	
	/*
	private String POST(String urlConnection, String body) throws IOException {
		
		URL url = new URL(urlConnection);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		conn.setConnectTimeout(5000);
		conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		conn.connect();
		
		OutputStream os = conn.getOutputStream();
		byte[] b = body.getBytes("UTF-8");
		os.write(b);
		os.flush();
		os.close();
		
		InputStream in = new BufferedInputStream(conn.getInputStream());
		byte[] res = new byte[2048];
		int i = 0;
		StringBuilder response = new StringBuilder();
		while ((i = in.read(res)) != -1) {
			response.append(new String(res, 0, i));
		}
		in.close();
		conn.disconnect();

		//System.out.println("Response= " + response.toString());
		String resp = response.toString();
		
		return resp;
	}
	*/
	
	/*
	private String GET(String urConnection) throws IOException {
		URL url = new URL(urConnection);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("Content-Type", "application/json");
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);
		
		int status = con.getResponseCode();
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
		    content.append(inputLine);
		}
		in.close();
		con.disconnect();
		
		return content.toString();
	}
	*/
}
