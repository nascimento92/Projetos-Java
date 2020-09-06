package br.com.grancoffee.TelemetriaPropria;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btn_teste implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		arg0.setMensagemRetorno(linhas[0].getCampo("AD_IDROTA").toString());
		
	}

}
