package xTestes;

import java.io.IOException;
import java.math.BigDecimal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class testeOkHttp {

	public static void main(String[] args) throws JSONException, IOException, ParseException {
		String url = "http://vmpay.vertitecnologia.com.br/api/v1/machines/5114/installations?access_token=DQfDYxCRstr7Ti2KSu5VouW3JzLqtuNZpMXNNvm8";
		String request = request(url);
		
		JSONArray array = new JSONArray(request);
		BigDecimal id = BigDecimal.ZERO;
		 for (int i = 0; i < array.length(); i++) {
			 JSONObject explrObject = array.getJSONObject(i);
			 if(explrObject.get("removed_at").equals(null)) {
				 id = new BigDecimal(explrObject.get("id").toString());
			 }			 
		 }
		 System.out.println("ID: "+id);
		
	}
	
	private static String request(String url) throws IOException, ParseException {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder().url(url).get().build();
		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}
}
