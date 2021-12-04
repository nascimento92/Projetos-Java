package xTestes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class teste{

	public static void main(String[] args) {
		
		BigDecimal qtdMinima = new BigDecimal(1);
		BigDecimal falta = new BigDecimal(3);
		
		/*
		 * if(falta.divide(qtdMinima, 2, RoundingMode.HALF_EVEN).doubleValue()==1) {
		 * System.out.println("pode"); }else { System.out.println("nao pode"); }
		 */
		
		/*
		 * if(falta.doubleValue()%qtdMinima.doubleValue()==0) {
		 * System.out.println("numero inteiro"); }else {
		 * System.out.println("numero quebrado"); }
		 */
		
		/*
		 * System.out.println("Qtd minima: "+qtdMinima+ "\nfalta: "+falta);
		 */
		
		String Hora_Ag = "122";
		String Data_Ag = getData(Hora_Ag,"dd/MM/yyyy");
		
		System.out.println(Data_Ag);
		
	}	
	
	static private String getData(String hora,String formato) {
		 int soma = 0;
		 if(hora.length()>2) {
			 soma = new Integer(hora.substring(0,1));
		 }
		 
		 if (formato==null) {
			 formato = "yyyy-MM-dd'T'HH:mm:ss";
		 }
		 
//		 DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		 DateFormat df = new SimpleDateFormat(formato);

		 Date dia = Calendar.getInstance().getTime();
		 Calendar  today = Calendar.getInstance();   
		 today.setTime(dia);
		 today.add(Calendar.DATE, soma);
		 String reportDate = df.format(today.getTime());
		 return reportDate;
	 }
}
