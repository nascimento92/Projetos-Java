package br.com.buttons;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;

import com.sankhya.util.BigDecimalUtil;

//import br.com.sankhya.modelcore.util.MGECoreParameter;



public class WServiceInvoker {
	private String	url;
	private String	key;
	private int codRet;
	private String urlfull; 
	private InputStream Resultado;
	private HttpURLConnection connection;
    @SuppressWarnings("unused")
	private int status;
    
    
	public WServiceInvoker(String url, String key) {
		this.url = url;
		this.key = key;
	}

	public void setSilentMode(boolean silentMode) {
	}

	
	public Object call(String method,  String body, String... parametros) throws Exception {
		Object docResp = null;
		HttpURLConnection conn = (HttpURLConnection) openConn(method, body, parametros);
		if (body==null){
			body="{}";
		}
		
		if (!method.equals("GET")) {
			docResp = callService(conn, body);
		} else {
			this.Resultado=conn.getInputStream();
			docResp =getResult();
		}
		
		conn.disconnect();
		
		this.codRet=((HttpURLConnection) conn).getResponseCode();
		this.urlfull=conn.getURL().toString();
		this.Resultado=conn.getInputStream();
		
	//	System.out.println("---->WS Resp:----->"+docResp);
		
		return docResp;
	}

	public String returnURL ( String Filtros, String... parametros) throws Exception {
		String Resultado;
		
		Resultado = getUrlChain( Filtros, parametros);
		
		return Resultado;
	}
	
	public Object callf(String method,  String body, String Filtros, String... parametros) throws Exception {
		Object docResp = null;
		HttpURLConnection conn = (HttpURLConnection) openConn(method, body, Filtros, parametros);
		if (body==null){
			body="{}";
		}
		
		if (!method.equals("GET")) {
			docResp = callService(conn, body);
		} else {
			this.Resultado=conn.getInputStream();
			docResp =getResult();
		}
		
		conn.disconnect();
		
		this.codRet=((HttpURLConnection) conn).getResponseCode();
		this.urlfull=conn.getURL().toString();
		this.Resultado=conn.getInputStream();
		
		return docResp;
	}

	
	
	public int getCodRet(){
		return this.codRet;
	}
	
	public String getURL(){
		return this.urlfull;
	}


	public String getResult() throws IOException{
		BufferedReader br = new BufferedReader(new InputStreamReader((Resultado)));

		return br.readLine();
	}

	
	
	
	private String getUrlChain(String filtros, String... parametros) throws Exception {
		StringBuffer buf = new StringBuffer();
		StringBuffer parameters = new StringBuffer();
		
        for(String parametro: parametros){
        	if (parametro==null){
        		continue;
        	}
        	parameters.append(parametro.replaceAll(" ", "%20"));
            parameters.append("/");
         }
        int x = parameters.length();
        if(parameters.substring(x,x).equals("/")){
        	parameters.deleteCharAt(x);
        };
       buf.append(url).append(url.endsWith("/") ? "" : "/").append(parameters);
       buf.append("?").append(filtros).append("&access_token=").append(key);

		return buf.toString();

	}

	
	
	private URLConnection openConn(String method, String body, String... parametros) throws Exception {
		StringBuffer buf = new StringBuffer();
		StringBuffer parameters = new StringBuffer();
		HttpURLConnection connection;
		
        for(String parametro: parametros){
        	if (parametro==null){
        		continue;
        	}
        	parameters.append(parametro.replaceAll(" ", "%20"));
        	parameters.append("/");
         }
        int x = parameters.length();
        if(parameters.substring(x,x).equals("/")){
        	parameters.deleteCharAt(x);
        };
       buf.append(url).append(url.endsWith("/") ? "" : "/").append(parameters);
	   buf.append("?access_token=").append(key);

	   try{
				URL u = new URL(buf.toString());
				URLConnection uc = u.openConnection();
				connection = (HttpURLConnection) uc;
				connection.setDoOutput(true);
				connection.setDoInput(true);
				if (method.equals("PATCH")){
					connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
					method = "POST";
				}
				connection.setRequestMethod(method);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("User-Agent", "WServiceInvoker");
				if (!method.equals("GET")){
			         connection.setDoOutput(true);
			         connection.setUseCaches(false);
				     connection.setConnectTimeout(15000);
			         connection.setRequestProperty("Content-Length", Integer.toString(body.length()));
				}
				this.connection = connection;
				
	    } catch (Exception e) {
	         e.printStackTrace();
	      }	

		return this.connection;

	}
	

	private URLConnection openConn(String method, String body, String Filtros, String... parametros) throws Exception {
		StringBuffer buf = new StringBuffer();
		StringBuffer parameters = new StringBuffer();
		HttpURLConnection connection;
		
        for(String parametro: parametros){
        	if (parametro==null){
        		continue;
        	}
        	parameters.append(parametro.replaceAll(" ", "%20"));
            parameters.append("/");
         }
        int x = parameters.length();
        if(parameters.substring(x,x).equals("/")){
        	parameters.deleteCharAt(x);
        };
       buf.append(url).append(url.endsWith("/") ? "" : "/").append(parameters);
       buf.append("?").append(Filtros).append("&access_token=").append(key);

	   try{
				URL u = new URL(buf.toString());
				URLConnection uc = u.openConnection();
				connection = (HttpURLConnection) uc;
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod(method);
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("User-Agent", "WServiceInvoker");
				if (!method.equals("GET")){
			         connection.setDoOutput(true);
			         connection.setUseCaches(false);
				     connection.setConnectTimeout(15000);
			         connection.setRequestProperty("Content-Length", Integer.toString(body.length()));
				}
				this.connection = connection;
				
	    } catch (Exception e) {
	         e.printStackTrace();
	      }	

		return this.connection;

	}

	
	
	
	private Object callService(HttpURLConnection conn, String body ) throws Exception {

		String input = body;

		OutputStream os = conn.getOutputStream();
		os.write(input.getBytes());
		os.flush();
		
		if (conn.getResponseCode() >= 400) {
			String Ret = "-----> Erro no retorno. Cód. Retorno:"+Integer.toString(conn.getResponseCode());
			conn.disconnect();
			return  Ret;
		}
    
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		return br.readLine();
		
		/*
		String output;
		while ((output = br.readLine()) != null) {
		//	System.out.println(output);
		}

		conn.disconnect();
		return output;
*/
	}
	

	
	public Object envia(String module,  BigDecimal IdVerti, String Acao, String xml) throws Exception {
		Object docResp = null;
		this.status = 0;

		String metodo = "POST";
		String chave = IdVerti.toString();
		
		String valores = removeAcentos(xml);
		
		if("D-I-U".indexOf(Acao)==-1) {
			return docResp;
			//this.status=new java.math.BigDecimal(0);
		}
		
		if(Acao.equals("D") && !BigDecimalUtil.getValueOrZero(IdVerti).equals(new java.math.BigDecimal(0))) {
			metodo = "GET";
			docResp = call(metodo,valores,module,chave);
			if (codRet>=200 && codRet<=299) {
				metodo = "DELETE";
				docResp = call(metodo,valores,module,chave);				
			}
			this.status = codRet;
		}

		if(Acao.equals("I") || Acao.equals("U") ) {
			if (!BigDecimalUtil.getValueOrZero(IdVerti).equals(new java.math.BigDecimal(0))) {
				metodo = "GET";
				docResp = call(metodo,valores,module,chave);
				if (codRet>=200 && codRet<=299) {
					metodo = "PATCH";
				} else {
					metodo = "POST";
				}
				docResp = call(metodo,valores,module,chave);
			}
			this.status = codRet;
		}
		
		
		return docResp;

	}
	
	
	public String removeAcentos(String str) {

		  str = Normalizer.normalize(str, Normalizer.Form.NFD);
		  str = str.replaceAll("[^\\p{ASCII}]", "");
		  return str;

		}

}
