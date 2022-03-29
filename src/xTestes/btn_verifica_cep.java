package xTestes;

import java.io.IOException;
import java.text.Normalizer;

import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class btn_verifica_cep {
	public static void main(String[] args) throws IOException, ParseException {
		getCepAPI("13181400");
	}
	
	private static void getCepAPI(String cep) throws IOException, ParseException {
		String url = "https://viacep.com.br/ws/"+cep.toString()+"/json/";
		String request = request(url);
	
		JsonObject convertedObject = new Gson().fromJson(request, JsonObject.class);
		
		String logradouro = tratarEndereco(convertedObject.get("logradouro").toString());
		
		System.out.println(logradouro);
	}
	
	private static String request(String url) throws IOException, ParseException {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder()
				.url(url)
				.get()
				.build();

		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}
	
	private static String tratarEndereco(String valor) {
		
		String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	    String semCaracteresEspeciais=Normalizer.normalize(semAcento, Normalizer.Form.NFD).replaceAll("[(|!?¨*°;:{}$#%^~&'\"\\<>)]", "");
	    String stringFInal = semCaracteresEspeciais.trim().replaceAll("\\s+", " ").toUpperCase();
	   
		return stringFInal;
	}
}
