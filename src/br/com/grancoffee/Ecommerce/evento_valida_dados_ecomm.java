package br.com.grancoffee.Ecommerce;

import java.math.BigDecimal;

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
	
	public void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal nunota = VO.asBigDecimal("NUNOTA");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		String volume = VO.asString("CODVOL");
		BigDecimal quantidade = VO.asBigDecimal("QTDNEG");
		
		boolean validaSeEhUmPedidoDoEcomm = validaSeEhUmPedidoDoEcomm(nunota);
		
		if(validaSeEhUmPedidoDoEcomm) {
			String unidadeEcomm = getUnidadeEcomm(produto);
			if(unidadeEcomm!=null) {
				
				if(unidadeEcomm!=volume) {
					
					BigDecimal qtdUnidadeAlternativa = getQuantidade(produto,unidadeEcomm);
					
					VO.setProperty("QTDNEG", quantidade.multiply(qtdUnidadeAlternativa));
				}
				
			}
		}
	}
	
	
	public boolean validaSeEhUmPedidoDoEcomm(BigDecimal nunota) {
		boolean valida = false;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
			DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
			
			if(VO!=null) {
				BigDecimal usuarioInclusao = VO.asBigDecimal("CODUSUINC");
				
				if(usuarioInclusao.intValue()==3538) { //usuário 3538 = wevo
					valida=true;
				}
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valida;
	}
	
	public String getUnidadeEcomm(BigDecimal produto) {
		String retorno = null;
		try {
			JapeWrapper DAO = JapeFactory.dao("Produto");
			DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
			
			if(VO!=null) {
				String unidade = VO.asString("AD_UNIDADELV");
				
				if(unidade!=null) {
					retorno = unidade;
				}
				
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return retorno;
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






















