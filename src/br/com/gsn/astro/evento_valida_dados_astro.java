package br.com.gsn.astro;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class evento_valida_dados_astro implements EventoProgramavelJava {

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
		start(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		BigDecimal numeroUnico = VO.asBigDecimal("NUNOTA");

		//TODO::Descobrir a TOP
		BigDecimal top = pegaTipoDeOperacao(numeroUnico);
		
		if(top.intValue()==10001) {
			
			//TODO:: Descobrir o local padrão do item
			BigDecimal local = pegarLocalPadrao(produto);
			
			if(local == null && produto.intValue()!=515259) {
				local = new BigDecimal(1110);
			}
			
			if(local!=null) {
				VO.setProperty("CODLOCALORIG", local);
			}
		}	
		
	}
	
	private BigDecimal pegaTipoDeOperacao(BigDecimal numeroUnico) throws Exception {
		BigDecimal top = null;
		
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { numeroUnico });
		
		if(VO!=null) {
			top = VO.asBigDecimal("CODTIPOPER");
		}

		return top;
	}
	
	private BigDecimal pegarLocalPadrao(BigDecimal produto) throws Exception {
		BigDecimal localpadrao = null;
		
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		
		if(VO!=null) {
			localpadrao = VO.asBigDecimal("CODLOCALPADRAO");
		}

		return localpadrao;
	}

}
