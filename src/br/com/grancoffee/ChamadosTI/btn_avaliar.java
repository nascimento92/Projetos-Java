package br.com.grancoffee.ChamadosTI;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class btn_avaliar implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		arg0.setMensagemRetorno("TESTE");
	}

}
