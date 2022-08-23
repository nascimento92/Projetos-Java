package br.com.gsn.Projetos;

import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class evento_valida_req_proj implements EventoProgramavelJava{
	
	//TODO::Quando selecionar o status de cancelado, preencher o cronograma cancelado e preencher a hora da finalização

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		VO.setProperty("CRONOGRAMA", "1");
		VO.setProperty("STATUS", "1");
		start(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		validacoes(VO);
	}
	
	private void validacoes(DynamicVO VO) {
		String status = VO.asString("STATUS");
		Timestamp dtPrevFim = VO.asTimestamp("DTPREVFIM");
		Timestamp dtfim = VO.asTimestamp("DTFINALIZACAO");
		
		//TODO :: Se o status for planejamento e a data prevista for maior que hoje o status deve ser "no prazo".
		if(dtPrevFim!=null && status!=null) {
			if("2".equals(status) && dtPrevFim.after(TimeUtils.getNow())) {
				VO.setProperty("CRONOGRAMA", "2");
			}
		}
		
		//TODO :: se finalizado mudar status e cronograma.
		if(dtfim!=null) {
			VO.setProperty("CRONOGRAMA", "4");
			VO.setProperty("STATUS", "8");
		}
		
		//TODO :: se status cancelado o cronograma fica como cancelado
		if(status!=null) {
			if("9".equals(status)) {
				VO.setProperty("CRONOGRAMA", "5");
			}
		}
		
	}

}
