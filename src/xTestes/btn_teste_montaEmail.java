package xTestes;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class btn_teste_montaEmail implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		arg0.mostraErro(
		"<img src=\"https://vitrioenhanced.vteximg.com.br/arquivos/88724-Campanha-Dia-do-Amigo-EMKT_01a.jpg?v=1\" width=\"600\" height=\"129\" alt=\"\" style=\"display:block; outline:none !important; mso-line-height-rule:exactly;\"/></a>"+
		"");		
	}

}
