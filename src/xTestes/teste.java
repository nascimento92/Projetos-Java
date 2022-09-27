package xTestes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.jsoup.internal.StringUtil;

import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

public class teste {

	public static void main(String[] args) throws Exception {

		/*
		 * BigDecimal qtdMinima = new BigDecimal(1); BigDecimal falta = new
		 * BigDecimal(3);
		 * 
		 * 
		 * if(falta.divide(qtdMinima, 2, RoundingMode.HALF_EVEN).doubleValue()==1) {
		 * System.out.println("pode"); }else { System.out.println("nao pode"); }
		 * 
		 * 
		 * 
		 * if(falta.doubleValue()%qtdMinima.doubleValue()==0) {
		 * System.out.println("numero inteiro"); }else {
		 * System.out.println("numero quebrado"); }
		 * 
		 * 
		 * 
		 * System.out.println("Qtd minima: "+qtdMinima+ "\nfalta: "+falta);
		 * 
		 * 
		 * String Hora_Ag = "122"; String Data_Ag = getData(Hora_Ag, "dd/MM/yyyy");
		 * 
		 * System.out.println(Data_Ag);
		 */
		
		//03-05-22 teste sobre extração de uma string.
		String valor = "014";
		String Hora = valor.substring(1)+":00:00";
		int QtdDias = Integer.valueOf(valor.substring(0, 1));
		Timestamp dataAgendamento = buildData(Hora);
		Timestamp novaData = TimeUtils.dataAddDay(dataAgendamento, QtdDias);
		
		System.out.println(
				"Valor: "+valor+
				"\nHora: "+Hora+
				"\nQtd Dias: "+QtdDias+
				"\nDt. Agendamento: "+dataAgendamento+
				"\nDt. Atendimento: "+novaData);
		
		String a = "TÉRREO ESPAÇO MARKET HONEST";
		String removerCaracteresEspeciais = StringUtils.removerCaracteresEspeciais(a);
		String removerAcentos = removerAcentos(a);
		System.out.println(removerAcentos);
		
		

	}
	
	public static String removerAcentos(String str) {
	    return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}
	
	private static Timestamp buildData(String hora) {
		String formato1 = "yyyy-MM-dd";
		DateFormat df = new SimpleDateFormat(formato1);
		String dtAtual = df.format(TimeUtils.getNow());
		
		String dataEHora = dtAtual+" "+hora;
		
		Timestamp time = Timestamp.valueOf(dataEHora);
		
		return time;
	}
	
	
	 private static String getData(String hora,String formato) {
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
