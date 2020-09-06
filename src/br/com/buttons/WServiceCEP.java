package br.com.buttons;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class WServiceCEP {
	
	private String cep="";
	private String logradouro="";
	private String complemento="";
	private String bairro="";
	private String localidade="";
	private String uf="";
	private String unidade="";
	private String ibge="";
	private String gia="";
	private int retorno = 0;
	
	
	public void buscarCep(String cep) {
		String json = "";

		try {
			URL url = new URL("http://viacep.com.br/ws/" + cep + "/json");
			URLConnection urlConnection = url.openConnection();
			InputStream is = urlConnection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			StringBuilder jsonSb = new StringBuilder();

			//br.lines().forEach(l -> jsonSb.append(l.trim()));

			json = jsonSb.toString();

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		retornoValor(json);
	}
	
	private void retornoValor(String json) {

		JSONObject jsonObject;
		JSONParser parser = new JSONParser();

		try {
			
			jsonObject = (JSONObject) parser.parse(json);

			setCep((String) jsonObject.get("cep"));
			setLogradouro((String) jsonObject.get("logradouro"));
			setBairro((String) jsonObject.get("bairro"));
			setLocalidade((String) jsonObject.get("localidade"));
			setUf((String) jsonObject.get("uf"));
			setUnidade((String) jsonObject.get("unidade"));
			setIbge((String) jsonObject.get("ibge"));
			setGia((String) jsonObject.get("gia"));
			
			setRetorno(1);

		}
		
		catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	// Getter Methods

	public String getCep() {
		return cep;
	}

	public String getLogradouro() {
		return logradouro;
	}

	public String getComplemento() {
		return complemento;
	}

	public String getBairro() {
		return bairro;
	}

	public String getLocalidade() {
		return localidade;
	}

	public String getUf() {
		return uf;
	}

	public String getUnidade() {
		return unidade;
	}

	public String getIbge() {
		return ibge;
	}

	public String getGia() {
		return gia;
	}

	public int getRetorno() {
		return retorno;
	}
	
	// Setter Methods

	public void setCep(String cep) {
		this.cep = cep;
	}

	public void setLogradouro(String logradouro) {
		this.logradouro = logradouro;
	}

	public void setComplemento(String complemento) {
		this.complemento = complemento;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public void setLocalidade(String localidade) {
		this.localidade = localidade;
	}

	public void setUf(String uf) {
		this.uf = uf;
	}

	public void setUnidade(String unidade) {
		this.unidade = unidade;
	}

	public void setIbge(String ibge) {
		this.ibge = ibge;
	}

	public void setGia(String gia) {
		this.gia = gia;
	}
	
	public void setRetorno(int retorno) {
		this.retorno = retorno;
	}
	
}

