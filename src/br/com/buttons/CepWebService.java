package br.com.buttons;

import java.net.URL;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class CepWebService {
	private String estado = "";
	private String cidade = "";
	private String bairro = "";
	private String tipo_logradouro = "";
	private String logradouro = "";
	private String IBGE = "";
	private int resultado = 0;
	private String resultado_txt = "";

	public CepWebService(String cep) {
		try {
			URL url = new URL("http://viacep.com.br/ws/" + cep + "/xml/");
			Document document = getDocumento(url);

			Element root = document.getRootElement();

			for (Iterator<?> i = root.elementIterator(); i.hasNext();) {
				Element element = (Element) i.next();

				if (element.getQualifiedName().equals("uf")) {
					setEstado(element.getText());
				}
				if (element.getQualifiedName().equals("localidade")) {
					setCidade(element.getText());
				}
				if (element.getQualifiedName().equals("bairro")) {
					setBairro(element.getText());
				}
				if (element.getQualifiedName().equals("ibge")) {
					setIBGE(element.getText());
				}

				if (element.getQualifiedName().equals("logradouro")) {
					setLogradouro(element.getText());
				}

				setResultado_txt("1");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public Document getDocumento(URL url) throws DocumentException {
		SAXReader reader = new SAXReader();
		return reader.read(url);
	}

	public String getEstado() {
		return this.estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public String getCidade() {
		return this.cidade;
	}

	public void setCidade(String cidade) {
		this.cidade = cidade;
	}

	public String getBairro() {
		return this.bairro;
	}

	public void setBairro(String bairro) {
		this.bairro = bairro;
	}

	public String getIBGE() {
		return this.IBGE;
	}

	public void setIBGE(String iBGE) {
		this.IBGE = iBGE;
	}

	public String getTipo_logradouro() {
		return this.tipo_logradouro;
	}

	public void setTipo_logradouro(String logradouro) {
		this.tipo_logradouro = logradouro.substring(0,
				logradouro.indexOf(" ") - 1);
	}

	public String getLogradouro() {
		return this.logradouro;
	}

	public void setLogradouro(String logradouro) {
		this.logradouro = logradouro;
	}

	public int getResultado() {
		return this.resultado;
	}

	public void setResultado(int resultado) {
		this.resultado = resultado;
	}

	public String getResultado_txt() {
		return this.resultado_txt;
	}

	public void setResultado_txt(String resultado_txt) {
		this.resultado_txt = resultado_txt;
	}
}