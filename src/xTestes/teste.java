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
		
		int x = 0;
		
		for(int i =0; i < 10; i++) {
			x += i;
			System.out.println(i);
		}
		
		System.out.println("\n\n"+x);
		
		/*
		String retorno = "{\"id\":16257,\"created_at\":\"2018-01-26T13:48:18.000Z\",\"updated_at\":\"2021-10-25T21:18:17.000Z\",\"type\":\"Product\",\"manufacturer_id\":74960,\"category_id\":116,\"name\":\"IOGURTE CAFE DA MANHA LINHACA MORANGO E BANANA ACTIVIA 170G\",\"upc_code\":\"100136\",\"barcode\":null,\"external_id\":\"100136\",\"weight\":null,\"cost_price\":null,\"vendible_balance\":-32817.0,\"tags\":[],\"additional_barcodes\":[{\"id\":210,\"value\":\"7891025107170\"},{\"id\":609,\"value\":\"7891025112174\"},{\"id\":718,\"value\":\"7891025117742\"}],\"ncm_code\":null,\"cest_code\":null,\"url\":\"http://vmpay.vertitecnologia.com.br/api/v1/products/16257\",\"inventories\":[{\"distribution_center_id\":23,\"total_quantity\":120.0,\"committed_quantity\":0.0},{\"distribution_center_id\":128,\"total_quantity\":63.0,\"committed_quantity\":0.0},{\"distribution_center_id\":132,\"total_quantity\":0.0,\"committed_quantity\":0.0}]}";
		JSONObject j = new JSONObject(retorno);
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
