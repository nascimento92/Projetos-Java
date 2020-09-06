package Links;

import java.math.BigDecimal;
import java.util.HashMap;

import javax.xml.bind.DatatypeConverter;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class linkTeste implements AcaoRotinaJava {

	private String id;
	private BigDecimal os;
	private String mensagemRetorno;
	private HashMap<String, Object> parans;

	public void doAction(ContextoAcao contexto) throws Exception {
		
		os = new BigDecimal(127);
		
		HashMap<String, Object> parans2 = new HashMap<String, Object>();
		parans2.put("\"NUNOTA\"", os);
		this.id="br.com.sankhya.os.mov.OrdemServico";
		this.parans = parans2;
		String msg2 = mensagemRetorno();
		contexto.setMensagemRetorno(msg2);

	}

	public String mensagemRetorno() {

		/* Link de Acesso portal */
		String caminho = "/mge/system.jsp#app/";
		String idBase64 = DatatypeConverter.printBase64Binary(id.getBytes());
		String paransBase64 = DatatypeConverter.printBase64Binary(parans.toString().replaceAll("=", ":").getBytes());
		String icone = "<p align=\"rigth\"><a href=\"" + caminho + idBase64 + "/" + paransBase64
				+ "\" target=\"_top\" >"
				+ "<img src=\"http://imageshack.com/a/img923/7316/ux573F.png\" ><font size=\"20\" color=\"#008B45\"><b>"
				+ "Click aqui para abrir a OS - "+os + "</b></font></a></p>";
		this.mensagemRetorno = "" + icone + "<p align=\"left\">";

		return this.mensagemRetorno;
	}

}
