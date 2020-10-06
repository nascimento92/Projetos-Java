package br.com.grancoffee.TelemetriaPropria;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btn_cancelarAbastecimento implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		
		if(arg0.getLinhas().length>1) {
			arg0.mostraErro("<b>Selecione apenas uma linha!</b>");
		}else {
			Registro[] linhas = arg0.getLinhas();
			Object nunota = linhas[0].getCampo("NUNOTA");
			Object numos = linhas[0].getCampo("NUMOS");
			
			if(nunota!=null && numos!=null) {
				boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!", "O pedido <b>"+nunota+"</b> será excluido do portal de vendas, e a OS <b>"+numos+"</b> será cancelada, continuar?", 1);	
				
				if(confirmarSimNao) {
					start(arg0);
				}	
			}else {
				arg0.mostraErro("<b>Pedido de abastecimento ainda não foi gerado!</b>");
			}
		}		
	}
	
	private void start(ContextoAcao arg0) {
		
	}
}
