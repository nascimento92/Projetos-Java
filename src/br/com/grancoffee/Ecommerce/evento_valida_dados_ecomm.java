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
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal nunota = VO.asBigDecimal("NUNOTA");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		String codvol = VO.asString("CODVOL");
		BigDecimal quantidadeOriginal = VO.asBigDecimal("QTDNEG");
		BigDecimal valorOriginal = VO.asBigDecimal("VLRUNIT");
		BigDecimal produtoParaNota = null;
		BigDecimal qtdKit = null;
		
		if(nunota!=null) {
			DynamicVO tgfcab = getTGFCAB(nunota);
			if(tgfcab!=null) {
				BigDecimal usuarioInclusao = tgfcab.asBigDecimal("CODUSUINC");
				//if(usuarioInclusao.intValue()==3538) {
				if(usuarioInclusao.intValue()==648) {	
					if(descobreSeEhUmKit(produto)) { //para produto que é um kit
						DynamicVO itemDoKit = getItemDoKit(produto);
						if(itemDoKit!=null) {
							produtoParaNota = itemDoKit.asBigDecimal("CODMATPRIMA");
							qtdKit = itemDoKit.asBigDecimal("QTDMISTURA");
							
							if(produtoParaNota!=null && qtdKit!=null) {
								DynamicVO tgfpro = getTGFPRO(produtoParaNota);
								if(tgfpro!=null) {
									VO.setProperty("CODPROD", produtoParaNota);
									VO.setProperty("QTDNEG", quantidadeOriginal.multiply(qtdKit));
									VO.setProperty("CODVOL", tgfpro.asString("CODVOL"));
								}
							}
							
						}
						
					}else { //para produto que não é kit
						DynamicVO tgfpro = getTGFPRO(produto);
						if(tgfpro!=null) {
							String unidadeVtex = tgfpro.asString("AD_UNIDADELV");
							
							if(!codvol.equals(unidadeVtex)) {
								BigDecimal quantidade = getQuantidade(produto,unidadeVtex);
								
								if(quantidade!=null) {
									VO.setProperty("QTDNEG", quantidadeOriginal.multiply(quantidade));
									VO.setProperty("CODVOL", unidadeVtex);
									VO.setProperty("VLRUNIT", valorOriginal.divide(quantidade,2,RoundingMode.HALF_EVEN));
								}	
							}		
						}
					}	
				}
			}
		}
		
		
		/*
		//String volume = VO.asString("CODVOL");
		BigDecimal quantidade = VO.asBigDecimal("QTDNEG");
		
		boolean validaSeEhUmPedidoDoEcomm = validaSeEhUmPedidoDoEcomm(nunota);
		
		if(validaSeEhUmPedidoDoEcomm) {
			String unidadeEcomm = getUnidadeEcomm(produto);
			String volume = getUnidadeProduto(produto);
			
			if(unidadeEcomm!=null) {
				
				if(volume!=null) {
					
					if(unidadeEcomm!=volume) {
						
						BigDecimal qtdUnidadeAlternativa = getQuantidade(produto,unidadeEcomm);
						
						VO.setProperty("QTDNEG", quantidade.multiply(qtdUnidadeAlternativa));
					}
					
				}
			}
		}
		*/
	}
	
	private boolean descobreSeEhUmKit(BigDecimal produto) throws Exception {
		boolean valida = false;
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		if(VO!=null) {
			String tipokit = VO.asString("TIPOKIT");
			if(tipokit!=null) {
				valida=true;
			}
		}
		return valida;
	}
	
	private DynamicVO getItemDoKit(BigDecimal produto) throws Exception {
		DynamicVO prod = null;
		JapeWrapper DAO = JapeFactory.dao("ItemComposicaoProduto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		if(VO!=null) {
			prod = VO;
		}
		return prod;
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






















