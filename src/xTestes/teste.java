package xTestes;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class teste implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		boolean confirmarSimNao = arg0.confirmarSimNao("Atenção", "Teste", 1);
		arg0.mostraErro(""+confirmarSimNao);
		
	}

	
}
