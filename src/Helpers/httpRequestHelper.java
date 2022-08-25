package Helpers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.parser.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class httpRequestHelper {

	public static void main(String[] args)
			throws URISyntaxException, IOException, InterruptedException, ParseException, JSONException {
		//String url = "http://api.grancoffee.com.br:8000/mid/inventario?codbem=eq.8689&tecla=eq.32768";
		String url = "http://api.grancoffee.com.br:8000/mid/inventario?codbem=in.(\"GCETESTE\")";	
		String metodo = "GET";
		String body = "";
		String pt = "";

		// String retorno = sendRequest(url,body,metodo);
		// String retorno2 = newHttpRequest(url);
		// String request3 = request3(url, metodo);
		//System.out.println(request4);
		
		String request4 = request4(url);
		
		JSONArray array = new JSONArray(request4);
		
		if(array.length()==0) {
			System.out.println("erro");
		}else {
			for(int i=0; i < array.length(); i++)   
			{  
				JSONObject object = array.getJSONObject(i);
				pt += "\n"+object.getString("codbem")+" tecla: "+object.getString("tecla")+" estoque: "+object.getInt("estoque");	
			}
			
			System.out.println(pt);
		}
		
		// System.out.println(retorno);
		// System.out.println(retorno2);
		// System.out.println(request3);
		// System.out.println(request4);
	}
	
	

	public static String newHttpRequest(String url) throws URISyntaxException, IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).header("x-api-key",
				"token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGlhY2Nlc3MifQ.BlvnsLa4kDAAlyxYuLRc1qo-hd72YqHPdr3SKnCxxqI")
				.GET().build();

		java.net.http.HttpResponse<String> response = HttpClient.newHttpClient().send(request,
				java.net.http.HttpResponse.BodyHandlers.ofString());

		return response.toString();
	}

	public static String sendRequest(String urlRequest, String body, String method) {

		String retorno = null;

		try {

			URL url = new URL(urlRequest);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("x-api-key",
					"token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGlhY2Nlc3MifQ.BlvnsLa4kDAAlyxYuLRc1qo-hd72YqHPdr3SKnCxxqI");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod(method);

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

			retorno = response.toString();
		} catch (Exception e) {
			System.out.println("Erro ao realizar a requisicao!" + e.getMessage());
		}

		return retorno;
	}

	public String getJssesionId(String response) {
		String jsessionid = null;

		Pattern p = Pattern.compile("<jsessionid>(\\S+)</jsessionid>");
		Matcher m = p.matcher(response);
		if (m.find()) {
			jsessionid = m.group(1);
		}

		return jsessionid;
	}

	private static String request3(String urlx, String method) {

		try {
			URL url = new URL(urlx);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(method);
			conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setReadTimeout(120000);
			conn.setConnectTimeout(120000);
			JSONObject postDataParams = new JSONObject();
			postDataParams.put("api_key",
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGlhY2Nlc3MifQ.BlvnsLa4kDAAlyxYuLRc1qo-hd72YqHPdr3SKnCxxqI");
			postDataParams.put("token",
					"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGlhY2Nlc3MifQ.BlvnsLa4kDAAlyxYuLRc1qo-hd72YqHPdr3SKnCxxqI");
			DataOutputStream os = new DataOutputStream(conn.getOutputStream());
			// os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
			os.writeBytes(postDataParams.toString());
			os.flush();
			os.close();

			int responseCode = conn.getResponseCode();

			if (responseCode == HttpsURLConnection.HTTP_OK) {

				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

				StringBuffer sb = new StringBuffer("");
				String line = "";

				while ((line = in.readLine()) != null) {

					sb.append(line);
					break;
				}

				in.close();
				conn.disconnect();
				return sb.toString();

			} else {
				conn.disconnect();
				return new String("false : " + responseCode);
			}

		} catch (Exception e) {
			return new String("Exception: " + e.getMessage());
		}
	}

	private static String request4(String url) throws IOException, ParseException {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder()
				.url(url)
				.method("GET", null)
				.addHeader("token",
						"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGlhY2Nlc3MifQ.BlvnsLa4kDAAlyxYuLRc1qo-hd72YqHPdr3SKnCxxqI")
				.build();

		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}
}
