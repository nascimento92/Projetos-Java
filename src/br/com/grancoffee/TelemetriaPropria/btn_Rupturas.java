package br.com.grancoffee.TelemetriaPropria;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btn_Rupturas implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		Object produto = linhas[0].getCampo("CODPROD");
		Object empresa = linhas[0].getCampo("CODEMP");
		Object patrimonio = linhas[0].getCampo("CODBEM");
		Object origem = linhas[0].getCampo("CODPRODORIG");
		
		arg0.setMensagemRetorno(" "+linhas.length);
		
		arg0.setMensagemRetorno(
				"produto: "+produto+"\n"+
				"empresa: "+empresa+"\n"+
				"patrimonio: "+patrimonio+"\n"+
				"origem: "+origem+"\n");
				
	}

}
