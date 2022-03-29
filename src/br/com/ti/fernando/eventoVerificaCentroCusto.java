package br.com.ti.fernando;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class eventoVerificaCentroCusto implements EventoProgramavelJava{

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
		if(verificaCentroCusto(arg0)) {
			throw new Error("<br/><b>ATENÇÃO</b><br/>Centro de Resultado exige que seja informado na aba Endereçamento o campo <b>\"Cód. Empresa Abast\"</b><br/><br/>!");
		}		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		if(verificaCentroCusto(arg0)) {
			throw new Error("<br/><b>ATENÇÃO</b><br/>Centro de Resultado exige que seja informado na aba Endereçamento o campo <b>\"Cód. Empresa Abast\"</b><br/><br/>!");
		}		
	}
	
	public boolean verificaCentroCusto(PersistenceEvent arg0) throws Exception{
		boolean retorno = false;
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal numContrato = (BigDecimal) VO.asBigDecimal("NUMCONTRATO");
		BigDecimal empAbast = (BigDecimal) VO.asBigDecimal("CODEMPABAST");
		BigDecimal centroCusto = getCentroCusto(numContrato);
		
		switch (centroCusto.intValue()){
	        case 1010201:
	        	if(empAbast == null) {
	        		retorno = true;
	        	}
	            break;
	        case 1010203:
	        	if(empAbast == null) {
	        		retorno = true;
	        	}
	            break;
	        case 1010202:
	        	if(empAbast == null) {
	        		retorno = true;
	        	}
	            break;
	        case 1010212:
	        	if(empAbast == null) {
	        		retorno = true;
	        	}
	            break;
	        case 1010610:
	        	if(empAbast == null) {
	        		retorno = true;
	        	}
	            break;
	        case 1010612:
	        	if(empAbast == null) {
	        		retorno = true;
	        	}
	            break;
			default: retorno = false;
		}

		return retorno;
	}

	private BigDecimal getCentroCusto(BigDecimal pNumContrato) throws Exception {
		BigDecimal codcencus = BigDecimal.ZERO;

		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("this.NUMCONTRATO=?",new Object[] { pNumContrato });

		if(VO!=null){
			codcencus = VO.asBigDecimal("CODCENCUS");
		}

		return codcencus;
	}

}
