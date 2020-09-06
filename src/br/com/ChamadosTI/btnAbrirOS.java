package br.com.ChamadosTI;

import java.math.BigDecimal;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btnAbrirOS implements AcaoRotinaJava {
	
	private String id;
	private HashMap<String, Object> parans;
	private String mensagemRetorno;
	private BigDecimal numos;
	
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
		
	}
	
	private void start(ContextoAcao arg0) {
		Registro[] linhas = arg0.getLinhas();
		
		if(linhas.length==1) {
			numos = (BigDecimal) linhas[0].getCampo("NUMOS");
			HashMap<String, Object> parans2 = new HashMap<String, Object>();
			parans2.put("\"NUMOS\"", numos);
			this.id="br.com.sankhya.os.mov.OrdemServico";
			this.parans = parans2;
			String msg2 = mensagemRetorno();
			arg0.setMensagemRetorno(msg2);
		}
	}
	
	public String mensagemRetorno() {
		
		String caminho = "/mge/system.jsp#app/";
		String idBase64 = DatatypeConverter.printBase64Binary(id.getBytes());
		String paransBase64 = DatatypeConverter.printBase64Binary(parans.toString().replaceAll("=", ":").getBytes());
		String icone = "<p align=\"rigth\"><a href=\"" + caminho + idBase64 + "/" + paransBase64
				+ "\" target=\"_top\" >"
				+ "<img src=\"http://imageshack.com/a/img923/7316/ux573F.png\" ><font size=\"20\" color=\"#008B45\"><b>"
				+ "Click aqui para abrir a OS - "+numos + "</b></font></a></p>";
		this.mensagemRetorno = "" + icone + "<p align=\"left\">";

		return this.mensagemRetorno;
	}

}
