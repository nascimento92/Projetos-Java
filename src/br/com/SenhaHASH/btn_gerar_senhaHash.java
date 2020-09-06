package br.com.SenhaHASH;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btn_gerar_senhaHash implements AcaoRotinaJava {
	
	/**
	 * @author gabriel.nascimento
	 * Objeto responsavel por criar um botão que transforme um texto em uma senha criptografada.
	 * O botão está sendo utilizado para o portal do cliente.
	 */
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {

		Registro[] linhas = arg0.getLinhas();

		start(linhas, arg0);

	}

	public void start(Registro[] linhas, ContextoAcao arg0) throws Exception {
		
		int cont = 0;
		
		for (int i = 0; i < linhas.length; i++) {
			
			String senhaTemp = (String) linhas[i].getCampo("AD_SENHAPORTALTEMP");
			
			if(senhaTemp!=null) {
				//String senhaHash = BCrypt.hashpw(senhaTemp, BCrypt.gensalt());
				linhas[i].setCampo("SENHAPORTAL", senhaTemp);
				linhas[i].setCampo("AD_SENHAPORTALTEMP", "");
				cont++;
			}		
		}
		
		arg0.setMensagemRetorno("Quantidade registros selecionados: "+linhas.length+" quantidade registros alterados: "+cont);
	}
	
}
