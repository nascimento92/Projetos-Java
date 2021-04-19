package br.com.flow.trocaDeGrade;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class flow_t_grade_evento_gradeFurura implements EventoProgramavelJava {

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public void update(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		BigDecimal idflow = newVO.asBigDecimal("IDINSTPRN");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		
		String tecla = newVO.asString("TECLA");
		if(tecla!="0") {
			validarAlteracao(idflow,tecla,produto,oldVO);
		}
	}
	
	public void validarAlteracao(BigDecimal idflow, String tecla,BigDecimal produto, DynamicVO oldVO) {
		
	}

}
