package br.com.flow.grancoffee.CancelamentoContrato;

import br.com.sankhya.extensions.flow.ContextoEvento;
import br.com.sankhya.extensions.flow.EventoProcessoJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class flow_cc_tarefaJava_validaSePatrimoniosForamPreenchidos implements EventoProcessoJava {

	@Override
	public void executar(ContextoEvento arg0) throws Exception {
		start(arg0);
		
	}
	
	private void start(ContextoEvento arg0) throws Exception {
		Object idInstanceTarefa = arg0.getIdInstanceProcesso();
		
		if(!validaSePatrimoniosForamPreenchidos(idInstanceTarefa)) {
			throw new PersistenceException("<br/><br/><br/><b>Preencher pelo menos um patrimônio para o cancelamento!</b><br/><br/><br/>");
		}
	}
	
	private boolean validaSePatrimoniosForamPreenchidos(Object idInstanceTarefa) throws Exception {
		boolean valida=false;
		
		JapeWrapper DAO = JapeFactory.dao("AD_PATCANCELAMENTO");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?", new Object[] { idInstanceTarefa });
		if (VO != null) {
			valida = true;
		}
		
		return valida;
	}
}
