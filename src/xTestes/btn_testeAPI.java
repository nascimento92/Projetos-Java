package xTestes;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class btn_testeAPI implements AcaoRotinaJava{
	
	/**
	 * @author Gabriel
	 * @serialData 01/03/2022
	 * Object created to test the MID API
	 * 
	 * {@docRoot}
	 * https://square.github.io/okhttp/
	 */
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		//String url = "http://api.grancoffee.com.br:8000/mid/inventario?codbem=eq.8689&tecla=eq.32768";
		String url = "http://api.grancoffee.com.br:8000/mid/inventario?codbem=in.(\"019521\",\"8689\")";
		String request = request(url);
		String pt = "";
		
		JSONArray array = new JSONArray(request);
		
		for(int i=0; i < array.length(); i++)   
		{  
			JSONObject object = array.getJSONObject(i);
			pt += "\n"+object.getString("codbem")+" tecla: "+object.getString("tecla");	
		}
		
		arg0.setMensagemRetorno(pt);
	}
	
	private String request(String url) throws IOException, ParseException {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder()
				.url(url)
				.get()
				//.method(method, null)
				.addHeader("token","eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhcGlhY2Nlc3MifQ.BlvnsLa4kDAAlyxYuLRc1qo-hd72YqHPdr3SKnCxxqI")
				.build();

		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}

}
