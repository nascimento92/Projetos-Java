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
		BigDecimal top = null;
		BigDecimal qtd = null;
		BigDecimal contrato = null;
		BigDecimal vlr = null;

		//TODO::Descobrir a TOP
		top = getTgfcab(numeroUnico).asBigDecimal("CODTIPOPER");
		
		if(top.intValue()==10001) {
			
			//TODO:: Descobrir o local padrão do item
			BigDecimal local = pegarLocalPadrao(produto);
			
			if(local == null && produto.intValue()!=515613) {
				local = new BigDecimal(1110);
			}
			
			if(local!=null) {
				VO.setProperty("CODLOCALORIG", local);
			}
			
			//TODO:: Desconsiderar item de assinatura no faturamento da 10002
			if(produto.intValue()==515613) {
				VO.setProperty("PENDENTE", "N");
				VO.setProperty("QTDENTREGUE", new BigDecimal(1));
			}
			
			//TODO:: Pega a quantidade negociada do contrato
			contrato = getTgfcab(numeroUnico).asBigDecimal("NUMCONTRATO");
			qtd = getTcspsc(contrato,produto).asBigDecimal("QTDEPREVISTA");
			
			if(qtd!=null) {
				vlr = VO.asBigDecimal("VLRUNIT");
				VO.setProperty("QTDNEG", qtd);
				VO.setProperty("VLRTOT", vlr.multiply(qtd));
			}
		}	
		
	}
	
	private DynamicVO getTgfcab(BigDecimal numeroUnico) throws Exception {
		DynamicVO VOs = null;
		
		
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { numeroUnico });
		
		if(VO!=null) {
			VOs = VO;
		}

		return VOs;
	}
	
	private DynamicVO getTcspsc(BigDecimal contrato, BigDecimal produto) throws Exception { //produtos e serviços
		DynamicVO VOs = null;
		
		
		JapeWrapper DAO = JapeFactory.dao("ProdutoServicoContrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND CODPROD=?",new Object[] { contrato, produto });
		
		if(VO!=null) {
			VOs = VO;
		}

		return VOs;
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
