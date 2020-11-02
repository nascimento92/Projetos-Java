package br.com.grancoffee.TelemetriaPropria;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btn_marcarAbastecer implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String abastecer = (String) arg0.getParam("ABASTECER");
		
		for(int i=0; i<linhas.length; i++) {
			linhas[i].setCampo("AD_ABASTECER", abastecer);
		}
		
		arg0.setMensagemRetorno("Finalizado!");
	}

}
