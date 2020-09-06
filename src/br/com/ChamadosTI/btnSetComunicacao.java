package br.com.ChamadosTI;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btnSetComunicacao implements AcaoRotinaJava {

	public void doAction(ContextoAcao arg0) throws Exception {
		
		start(arg0);
	}
	/**
	 * Descontinuado !
	 * @param arg0
	 * @throws Exception
	 */
	private void start(ContextoAcao arg0) throws Exception {
		String comunicacao = (String) arg0.getParam("COMUNICACAO");
		
		if(comunicacao!=null) {
			Registro[] linhas = arg0.getLinhas();
			
			linhas[0].setCampo("COMTI", comunicacao);
		}
	}

}
