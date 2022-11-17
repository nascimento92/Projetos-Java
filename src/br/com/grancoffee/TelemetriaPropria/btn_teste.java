package br.com.grancoffee.TelemetriaPropria;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class btn_teste implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		testaMensagens(arg0);
	}
	
	private void testaMensagens(ContextoAcao arg0) {
		int cont = 1;
		String tipo = (String) arg0.getParam("TIPO");
		if("1".equals(tipo)) {
			arg0.setMensagemRetorno("<center><img src=\"https://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/sign-error-icon.png\" width=\"30\" height=\"30\"><br/> \n<b>Não foram agendadas visitas!</b><br/></center>");
		}else if("2".equals(tipo)) {
			arg0.setMensagemRetorno("<center><img src=\"https://cdn-icons-png.flaticon.com/512/148/148767.png\" width=\"30\" height=\"30\"><br/> \nForam solicitado(s) <b>" + cont + "</b> abastecimento(s)!<br/></center>");
		}else {
			arg0.setMensagemRetorno(
					"<center>"+
					"<img src=\"https://media.tenor.com/PYQdx807FXAAAAAC/sucess-transparent.gif\" width=\"150\" height=\"100\"><br/><br/>"+
					"<img src=\"https://media3.giphy.com/media/8L0Pky6C83SzkzU55a/200w.gif?cid=6c09b952s6x6b1lrua14y7v2e02mthwjsgn3pzuxel6encmo&rid=200w.gif&ct=g\" width=\"120\" height=\"150\"><br/><br/>"+
					"<img src=\"https://media2.giphy.com/media/Qxkf4LQ1xIbXpH8z0I/giphy.gif\" width=\"100\" height=\"100\"><br/><br/>"+
					"</center>");
		}
	}
}
