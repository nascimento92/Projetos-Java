package br.com.grancoffee.Ecommerce;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;

public class btn_aprovar_parceiro implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		boolean confirmarSimNao = arg0.confirmarSimNao("Atenção!", "Os registros serão aprovados, continuar?", 1);
		
		if(confirmarSimNao) {
			for (Registro registro : linhas) {
				start(registro);
			}
		}
	}
	
	private void start(Registro registro) throws Exception {
		registro.setCampo("APROVADO", "S");
		registro.setCampo("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
		registro.setCampo("DTAPROV", TimeUtils.getNow());
	}

}
