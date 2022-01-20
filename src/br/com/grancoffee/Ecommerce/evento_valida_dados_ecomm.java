package br.com.grancoffee.Ecommerce;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class evento_valida_dados_ecomm implements EventoProgramavelJava{

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
	
	public void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO)arg0.getVo();
	    BigDecimal nunota = VO.asBigDecimal("NUNOTA");
	    BigDecimal produto = VO.asBigDecimal("CODPROD");
	    String codvol = VO.asString("CODVOL");
	    BigDecimal quantidadeOriginal = VO.asBigDecimal("QTDNEG");
	    BigDecimal valorOriginal = VO.asBigDecimal("VLRUNIT");
	    BigDecimal codlocalorig = VO.asBigDecimal("CODLOCALORIG");
	    BigDecimal novocodlocalorig = new BigDecimal(1117);
	    
	    if (nunota != null) {
	      DynamicVO tgfcab = getTGFCAB(nunota);
	      if (tgfcab != null) {
	        BigDecimal usuarioInclusao = tgfcab.asBigDecimal("CODUSUINC");
	        if (usuarioInclusao.intValue() == 3538) {
	        //if (usuarioInclusao.intValue() == 648) {
	          DynamicVO tgfpro = getTGFPRO(produto);
	          if (tgfpro != null) {
	        	
	        	//TODO::Verifica se é uma máquina 
	        	BigDecimal grupoProduto = tgfpro.asBigDecimal("CODGRUPOPROD");
	        	if(grupoProduto.intValue()>=500000 && grupoProduto.intValue()<600000) {//é uma máquina
	        		if(codlocalorig.intValue()!=novocodlocalorig.intValue()) {
	        			VO.setProperty("CODLOCALORIG", novocodlocalorig);
	        		}
	        	}
	        	  
	        	//TODO::Verifica a unidade e-commerce.  
	            String unidadeVtex = tgfpro.asString("AD_UNIDADELV");
	            if (!codvol.equals(unidadeVtex)) {
	              BigDecimal quantidade = getQuantidade(produto, unidadeVtex);
	              if (quantidade != null) {
	                VO.setProperty("QTDNEG", quantidadeOriginal.multiply(quantidade));
	                VO.setProperty("CODVOL", unidadeVtex);
	                VO.setProperty("VLRUNIT", valorOriginal.divide(quantidade, 2, RoundingMode.HALF_EVEN));
	              } 
	            }
	            
	          } 
	        } 
	      } 
	    }
	    
	}
	
	private DynamicVO getTGFCAB(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
		return VO;
	}
	
	private DynamicVO getTGFPRO(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		return VO;
	}
		
	public BigDecimal getQuantidade(BigDecimal produto, String unidade) {
		BigDecimal qtd = null;
		try {
			JapeWrapper DAO = JapeFactory.dao("VolumeAlternativo");
			DynamicVO VO = DAO.findOne("CODPROD=? AND CODVOL=?",new Object[] { produto,unidade });
			
			if(VO!=null) {
				BigDecimal quantidade = VO.asBigDecimal("QUANTIDADE");
				
				if(unidade!=null) {
					qtd = quantidade;
				}
				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return qtd;
	}

}






















