package xTestes;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

public class Main {
	public static void main(String[] args) throws Exception {
		//Timestamp data = TimeUtils.getNow();
		
		//Timestamp buildPrintableTimestamp = TimeUtils.buildPrintableTimestamp(data.getTime(), "dd/MM/yyyy HH:mm:ss");
		
		//Timestamp buildPrintableTimestamp2 = TimeUtils.buildPrintableTimestamp(TimeUtils.addWorkingDays(TimeUtils.getNow().getTime(), 3), "dd/MM/yyyy HH:mm:ss");
		
		//Timestamp dataA = new Timestamp(TimeUtils.addWorkingDays(TimeUtils.getNow().getTime(), 3));
		
		//System.out.println(dataA);
		
		int dia = 01;
		int mes = 01;
		int ano = 2023;
		
		Timestamp buildData = TimeUtils.buildData(dia, mes-1, ano);
		System.out.println(buildData);
		
		int day = TimeUtils.getDay(TimeUtils.getNow());
		int month = TimeUtils.getMonth(TimeUtils.getNow());
		int year = TimeUtils.getYear(TimeUtils.getNow());

		System.out.println(TimeUtils.buildData(day, month-1, year)+" - "+month);
		
		ArrayList<String> emails = new ArrayList<String>();  
        emails.add("javaTpoint@@domain.co.in@.com");  
        emails.add("javaTpoint@domain.com");  
        emails.add("javaTpoint.name@domain.com");  
        emails.add("javaTpoint#@domain.co.in");  
        emails.add("javaTpoint@domain.com");  
        emails.add("javaTpoint@domaincom");  
        //Add invalid emails in list  
        emails.add("@yahoo.com");  
        emails.add("javaTpoint#domain.com"); 
        //Regular Expression   
        //String regex = "^(.+)@(.+)$";
        String regex = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";  
        Pattern pattern = Pattern.compile(regex);
        for(String email : emails){  
            //Create instance of matcher   
            Matcher matcher = pattern.matcher(email);  
            System.out.println(email +" : "+ matcher.matches()+"\n");  
        } 
        
        String cpf="419.356.648-08";
        String formataCgcCpf = StringUtils.removePontuacao(cpf).replace("-", "").replace("/", "");
        
        System.out.println(formataCgcCpf);
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
