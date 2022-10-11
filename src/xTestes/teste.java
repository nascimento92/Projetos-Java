package xTestes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

public class teste {

	public static void main(String[] args) throws JSONException {

		String retorno = "{\"serviceName\":\"MobileLoginSP.login\",\"status\":\"1\",\"pendingPrinting\":\"false\",\"transactionId\":\"39868C2AC9106E6B0C60423F1C8E3B8A\",\"responseBody\":{\"callID\":{\"$\":\"31E59C0195EE35E24836CF33D4940D2C\"},\"jsessionid\":{\"$\":\"UFGI5u_rHocQaKXD1ctP_XUWztWzlm4DPXbCxE5T\"},\"kID\":{\"$\":\"NEMzRjc5QTU0QTM0Njc4MzIwNkY1MzIxQzU1NEQwMjc=\\n\"},\"idusu\":{\"$\":\"NjQ4\\n\"}}}";
		JSONObject j = new JSONObject(retorno);
		String string = j.getJSONObject("responseBody").getJSONObject("jsessionid").getString("$");
		System.out.println(string);
		
		/*
		JSONArray jsonArray = j.getJSONArray("additional_barcodes");
		for(int i = 0; i < jsonArray.length(); i++) {
			JSONObject x = jsonArray.getJSONObject(i);
			int int1 = x.getInt("id");
			String string = x.getString("value");
			System.out.println("ID: "+int1+"\nVALOR: "+string);
		}
		*/
		
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
