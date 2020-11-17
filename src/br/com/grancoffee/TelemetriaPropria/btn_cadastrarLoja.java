package br.com.grancoffee.TelemetriaPropria;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class btn_cadastrarLoja implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		arg0.setMensagemRetorno("OI");
	}
	
	private void start(ContextoAcao arg0) {
		String nome = (String) arg0.getParam("NOME");
		Object param = arg0.getParam("ENDERECO");
	}
	
	//montagem do nome do corner tem que ser feita aqui no JAVA, pega o ultimo corner, substrai os ultimos 4 valores
	//soma mais um, formata com 4 caracteres, concatena a palavra corner, faz o insert 

}
