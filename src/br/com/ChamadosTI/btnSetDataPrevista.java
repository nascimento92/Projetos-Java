package br.com.ChamadosTI;

import java.sql.Timestamp;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btnSetDataPrevista implements AcaoRotinaJava {

	public void doAction(ContextoAcao arg0) throws Exception {
		
		start(arg0);
		
	}
	
	private void start(ContextoAcao arg0) throws Exception {
		
		Timestamp parDtPrevista = (Timestamp) arg0.getParam("DTPREVISTA");
			
		  if(parDtPrevista!=null) { 
			  Registro[] linhas = arg0.getLinhas();
			  linhas[0].setCampo("DTENTREGA", parDtPrevista); 
		  }
		 
	}
}
