package xTestes;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sankhya.util.TimeUtils;

public class Main {
	public static void main(String[] args) throws ParseException {
		Timestamp data = TimeUtils.getNow();
		
		Timestamp buildPrintableTimestamp = TimeUtils.buildPrintableTimestamp(data.getTime(), "dd/MM/yyyy HH:mm:ss");
		
		
		System.out.println(buildPrintableTimestamp);
	}
	
	/*
	private static void listarCidade() {
		SWServiceInvoker si = new SWServiceInvoker("http://localhost:8080", "SUP", "");
		
		try{
			//SAVE
			//String xmlAsString = "<entity name='Cidade'><campo nome='CODCID'>12312</campo><campo nome='NOMECID'>Sao Paulo</campo><campo nome='UF'>1</campo></entity>";
			//Document docRet = serviceInvoker.call("crud.save", "mge", xmlAsString);
			//FIND
			String xmlAsString  = "<entity name='Cidade'><criterio nome='CODCID' valor='11' /></entity>";
			Document docRet = si.call("crud.find", "mge", xmlAsString);
			//REMOVE
			//String xmlAsString  = "<entity name='Cidade'><campo nome='CODCID'>12312</campo></entity>";
			//Document docRet = si.call("crud.remove", "mge", xmlAsString);
			printDocument(docRet);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	*/
	
	/*
	private static void printDocument(Document doc) throws Exception {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		String xmlString = result.getWriter().toString();
		System.out.println("----inicio---");
		System.out.println(xmlString);
		System.out.println("----fim-----");
	}
	*/

}
