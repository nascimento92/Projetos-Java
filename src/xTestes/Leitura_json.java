package xTestes;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Leitura_json {
	
	public static void main(String[] args){
		
	
		
		
	}
	
	private String RealizarLogin() {
		String url = "http://sankhya.grancoffee.com.br:8180/mge/service.sbr?serviceName=MobileLoginSP.login&outputType=json";
		String body = "{\"serviceName\": \"MobileLoginSP.login\",\"requestBody\": {"+
			        "\"NOMUSU\": {"+
			            "\"$\": \"GCDIGITAL\"},"+
			        "\"INTERNO\": {"+
			            "\"$\": \"123456789\"},"+
			        "\"KEEPCONNECTED\": {"+
			            "\"$\": \"S\"}}}";
		StringBuilder post_JSON = Post_JSON(url,body);
		String jSession = getJSession(post_JSON);
		return jSession;
	}
	
	private static String getJSession(StringBuilder post_JSON) {

		String jSession = null;
		
		Object obj2= JSONValue.parse(post_JSON.toString());
		JSONObject jsonObject2 = (JSONObject) obj2;
		JSONObject b = (JSONObject) jsonObject2.get("responseBody");
		
		if(b!=null) {
			JSONObject c = (JSONObject) b.get("jsessionid");
			String string = c.values().toString();
			jSession = string.replace("[", "").replace("]", "");
		}
		
		return jSession;
	}

	public static StringBuilder Post_JSON(String query_url,String request){
		StringBuilder response = new StringBuilder();
		
		try {
			
			URL url = new URL(query_url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			
			OutputStream os = conn.getOutputStream();
			byte[] b = request.getBytes("UTF-8");
			os.write(b);
			os.flush();
			os.close();
			
			InputStream in = new BufferedInputStream(conn.getInputStream());
			byte[] res = new byte[2048];
			int i = 0;
			
			while ((i = in.read(res)) != -1) {
				response.append(new String(res, 0, i));
			}
			in.close();

		} catch (Exception e) {
			System.out.println("ERRRO !"+e.getMessage());
		}
		
		return response;
	}
		
	

}
