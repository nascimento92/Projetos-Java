package Helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.Normalizer;


public class WSPentaho {
	private String	url;
	private String	key;
	private int codRet;
	private String urlfull; 
	private InputStream Resultado;
	private HttpURLConnection connection;
    @SuppressWarnings("unused")
	private int status; 
    
    
	public WSPentaho(String url, String key) {
		this.url = url;
		this.key = key;
	}

	public void setSilentMode(boolean silentMode) {
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
		StringBuffer objeto = new StringBuffer();
		
        for(String parametro: parametros){
        	if (parametro==null){
        		continue;
        	}
        parameters.append(parametro.replaceAll(" ", "%20"));
            parameters.append("/");
            objeto.append(filtros.replaceAll("/", "%2F")/*.replaceAll(" ", "%20")*/);
         }
        int x = parameters.length();
        if(parameters.substring(x,x).equals("/")){
        	parameters.deleteCharAt(x);
        };
       buf.append(url).append(url.endsWith("/") ? "" : "/").append(parameters);
       buf.append("?").append(filtros).append("&xml=Y");
		return buf.toString();

	}

	private URLConnection openConn(String method, String body, String Filtros, String... parametros) throws Exception {
		StringBuffer buf = new StringBuffer();
		StringBuffer parameters = new StringBuffer();
		StringBuffer objeto = new StringBuffer();
		HttpURLConnection connection;
		
        for(String parametro: parametros){
        	if (parametro==null){
        		continue;
        	}
            parameters.append(parametro.replaceAll(" ", "%20"));
            parameters.append("/");
            objeto.append(Filtros.replaceAll("/", "%2F").replaceAll(" ", "%20"));
         }
        int x = parameters.length();
        if(parameters.substring(x,x).equals("/")){
        	parameters.deleteCharAt(x);
        };
       buf.append(url).append(url.endsWith("/") ? "" : "/").append(parameters);
       buf.append("?").append(objeto).append("&xml=Y");

	   try{
				URL u = new URL(buf.toString());
				URLConnection uc = u.openConnection();
				connection = (HttpURLConnection) uc;
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setRequestMethod(method);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setRequestProperty("User-Agent", "Pentaho");
				connection.setRequestProperty("Authorization", key);
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

	 //  System.out.println(" --> URL Pentaho:"+ buf.toString() );
	   
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
		

	}
	
	public String removeAcentos(String str) {

		  str = Normalizer.normalize(str, Normalizer.Form.NFD);
		  str = str.replaceAll("[^\\p{ASCII}]", "");
		  return str;

		};

	
	
	public Object runTrans(String path,String transformation )  throws Exception {
	    String metodo = "GET";
	    String fullPath = "trans="+path+transformation;
	    String body = "";
	    String modulo = "runTrans";
	    
	  Object response = callf(metodo,body, fullPath, modulo);
	  
	  return response;
	    
	}

	
	public Object runJob(String path,String job )  throws Exception {
	    String metodo = "POST";
	    String fullPath = "";
	    String body = "rep=GCWeb&job="+path+job;
	    String modulo = "executeJob";
	    
	  Object response = callf(metodo,body, fullPath, modulo);
	  
	  return response;
	}
	


}
