package br.com.flow.grancoffee.CancelamentoContrato;

import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_cc_tarefaJava_ExcluirNotas implements TarefaJava {

	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);
	}
	
	private void start(ContextoTarefa arg0) throws Exception {
		Object idflow = arg0.getIdInstanceProcesso();
		excluirNotas(idflow);
		
		QueryExecutor query = arg0.getQuery();
		query.nativeSelect("SELECT * FROM EU");
	}
	
	private void excluirNotas(Object idflow) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_DEVNFCANCELAMENTO", "this.SELECIONAR=? and this.IDINSTPRN=?",new Object[] {"N",idflow}));
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_ExcluirNotas] ## - Nao foi possivel excluir notas de devolucao!");
		}
	}

}
