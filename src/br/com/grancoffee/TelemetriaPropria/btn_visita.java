package br.com.grancoffee.TelemetriaPropria;

import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btn_visita implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		start(linhas,arg0);	
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		Timestamp dtVisita = (Timestamp) arg0.getParam("DTVISITA");
		
		arg0.setMensagemRetorno(dtVisita.toString());
	}

}
