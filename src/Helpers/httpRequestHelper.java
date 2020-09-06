package Helpers;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class httpRequestHelper {
	
	private String method = null;
	
	public httpRequestHelper(String method) {
		this.method = method;
	}
	
	public String sendRequest(String urlRequest, String body) {
		
		String retorno = null;
		
		try {
			
			URL url = new URL(urlRequest);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
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
			System.out.println("Erro ao realizar a requisicao!"+e.getMessage());
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
}
