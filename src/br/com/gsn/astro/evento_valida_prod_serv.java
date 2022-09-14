package br.com.gsn.astro;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class evento_valida_prod_serv implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		BigDecimal qtdprev = VO.asBigDecimal("QTDEPREVISTA");
		BigDecimal qtdEmKg = VO.asBigDecimal("AD_QTDEMKG");
		
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		DynamicVO TCSCON = getTCSCON(contrato);
		String tipoContrato = TCSCON.asString("AD_TIPCONT");
		
		if("A".equals(tipoContrato)) {
			
			if(produto.intValue()!=515613) {
				if(qtdprev!=null && qtdEmKg!=null) {
					if(qtdprev.intValue()==0 && qtdEmKg.intValue()==0) {
						throw new Error("<br/><b>OPS</b><br/><br/>tipo de contrato <b>Assinatura</b>! Valor 0 não é válido ! Preencha o campo <b>Qtd. Prevista</b> ou <b>Qtd. Em KG</b>!");
						
					}
				}
			}
			
		}	
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		BigDecimal qtdprev = VO.asBigDecimal("QTDEPREVISTA");
		BigDecimal qtdEmKg = VO.asBigDecimal("AD_QTDEMKG");
		
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		DynamicVO TCSCON = getTCSCON(contrato);
		String tipoContrato = TCSCON.asString("AD_TIPCONT");
		
		if("A".equals(tipoContrato)) {
			if(produto.intValue()!=515613) {
				if(qtdprev!=null && qtdEmKg!=null) {
					if(qtdprev.intValue()==0 && qtdEmKg.intValue()==0) {
						throw new Error("<br/><b>OPS</b><br/><br/>tipo de contrato <b>Assinatura</b>! Valor 0 não é válido ! Preencha o campo <b>Qtd. Prevista</b> ou <b>Qtd. Em KG</b>!");
						
					}
				}
			}
		}
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
		insert(arg0);		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}
	
	private void update(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		DynamicVO TCSCON = getTCSCON(contrato);
		String tipoContrato = TCSCON.asString("AD_TIPCONT");
		
		DynamicVO TGFPRO = getTGFPRO(produto);
		String unidadePadrao = "";
		String unidade = TGFPRO.asString("CODVOL");
		String unidadeDeVenda = TGFPRO.asString("AD_CODVOL");
		BigDecimal grupo = TGFPRO.asBigDecimal("CODGRUPOPROD");
		
		if(unidadeDeVenda!=null) {
			unidadePadrao = unidadeDeVenda;
		}else {
			unidadePadrao = unidade;
		}
		
		if("A".equals(tipoContrato)) {
			
			if(grupo.intValue()<500000 && grupo.intValue()>=600000) {
				
				VO.setProperty("AD_FRANQUIA", "S");
				String tipoFranquia = VO.asString("AD_FRANQUIA");
				
				if("S".equals(tipoFranquia)) {
					
					if(produto.intValue()!=515613) {
						
						DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
						
						//campos agora
						BigDecimal qtdprev = VO.asBigDecimal("QTDEPREVISTA");
						BigDecimal qtdEmKg = VO.asBigDecimal("AD_QTDEMKG");
						
						//campos anteriormente
						BigDecimal oldqtdprev = oldVO.asBigDecimal("QTDEPREVISTA");
						BigDecimal oldqtdEmKg = oldVO.asBigDecimal("AD_QTDEMKG");
						
						ValidaNullEZero(qtdprev,qtdEmKg);
						
						if(!"KG".equals(unidadePadrao)) { //unidade padrão não é quilo
							DynamicVO TGFVOA = getTGFVOA(produto);
							
							if(qtdprev!=oldqtdprev && qtdEmKg==oldqtdEmKg) {
								calculaPelaQuantidade(qtdprev,qtdEmKg,TGFVOA,VO);
							}
							
							if(qtdEmKg!=oldqtdEmKg && qtdprev==oldqtdprev) {
								calcularPeloKg(qtdprev,qtdEmKg,TGFVOA,VO);
							}
							
							
						}else { //unidade padrão já é quilo
							
							if(qtdEmKg!=oldqtdEmKg && qtdprev==oldqtdprev) {
								VO.setProperty("QTDEPREVISTA", qtdEmKg);
							}
							
							if(qtdprev!=oldqtdprev && qtdEmKg==oldqtdEmKg) {
								VO.setProperty("AD_QTDEMKG", qtdprev);
							}
							
							if(qtdprev!=oldqtdprev && qtdEmKg!=oldqtdEmKg) {
								VO.setProperty("QTDEPREVISTA", qtdEmKg);
							}
							
						}
					}	
				}
			}
		}
	}
	
	public void insert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		
		DynamicVO TCSCON = getTCSCON(contrato);
		String tipoContrato = TCSCON.asString("AD_TIPCONT"); //contrato tipo A - Assinatura
		
		DynamicVO TGFPRO = getTGFPRO(produto);
		String unidadePadrao = TGFPRO.asString("CODVOL");
		BigDecimal grupo = TGFPRO.asBigDecimal("CODGRUPOPROD");
		
		if("A".equals(tipoContrato)) {
			
			if(grupo.intValue()<500000 && grupo.intValue()>=600000) { //valida apenas produtos que não são máquinas
				
				VO.setProperty("AD_FRANQUIA", "S");
				String tipoFranquia = VO.asString("AD_FRANQUIA"); 
				
				if("S".equals(tipoFranquia)) {
					
					if(produto.intValue()!=515613) {
						
						BigDecimal qtdprev = VO.asBigDecimal("QTDEPREVISTA");
						BigDecimal qtdEmKg = VO.asBigDecimal("AD_QTDEMKG");
						
						ValidaNullEZero(qtdprev,qtdEmKg);
						
						if(!"KG".equals(unidadePadrao)) {
							DynamicVO TGFVOA = getTGFVOA(produto);
							
							if(TGFVOA==null) {
								throw new Error("<br/><b>OPS</b><br/><br/>Produto não pode ser inserido! tipo de contrato <b>Assinatura</b>! não foi encontrado uma unidade alternativa em KG.");
							}
				
							if(TGFVOA!=null) {
								
								if(qtdprev!=null && qtdEmKg==null) {
									calculaPelaQuantidade(qtdprev,qtdEmKg,TGFVOA,VO);
								}
								
								if(qtdEmKg!=null) {
									calcularPeloKg(qtdprev,qtdEmKg,TGFVOA,VO);
								}
				
							}

						}else {
							
							if(qtdprev!=null) {
								VO.setProperty("AD_QTDEMKG", qtdprev);
							}
							
							if (qtdprev==null && qtdEmKg!=null) {
								VO.setProperty("QTDEPREVISTA", qtdEmKg);
							}
						}
						
					}	
				}
				
			}	
		}
	}
	
	private void calculaPelaQuantidade(BigDecimal qtdprev, BigDecimal qtdEmKg, DynamicVO TGFVOA, DynamicVO VO) {
		 //preenchendo atraves da qtd prevista.
			String opcao = TGFVOA.asString("DIVIDEMULTIPLICA");//M = multiplica D=Divide
			BigDecimal qtdTgfvoa = TGFVOA.asBigDecimal("QUANTIDADE").setScale(2, RoundingMode.HALF_EVEN);
			BigDecimal qtdEmKG = null;
			
			if("M".equals(opcao)) {
				qtdEmKG = qtdprev.divide(qtdTgfvoa, 2, RoundingMode.HALF_UP);
			}else if("D".equals(opcao)) {
				qtdEmKG = qtdprev.multiply(qtdTgfvoa);
			}else {
				qtdEmKG = qtdprev;
			}
			
			VO.setProperty("AD_QTDEMKG", qtdEmKG);
	}
	
	private void calcularPeloKg(BigDecimal qtdprev, BigDecimal qtdEmKg, DynamicVO TGFVOA, DynamicVO VO) {
		String opcao = TGFVOA.asString("DIVIDEMULTIPLICA");//M = multiplica D=Divide
		BigDecimal qtdTgfvoa = TGFVOA.asBigDecimal("QUANTIDADE").setScale(2, RoundingMode.HALF_EVEN);
		BigDecimal qtdEmKG = null;
		
		if("M".equals(opcao)) {
			qtdEmKG = qtdEmKg.multiply(qtdTgfvoa);
		}else if("D".equals(opcao)) {
			qtdEmKG = qtdEmKg.divide(qtdTgfvoa, 2, RoundingMode.HALF_UP);
		}else {
			qtdEmKG = qtdprev;
		}
		
		VO.setProperty("QTDEPREVISTA", qtdEmKG);
	}
	
	private void ValidaNullEZero(BigDecimal qtdprev, BigDecimal qtdEmKg) {
		if(qtdprev==null && qtdEmKg==null) {
			throw new Error("<br/><b>OPS</b><br/><br/>tipo de contrato <b>Assinatura</b>! Preencha o campo <b>Qtd. Prevista</b> ou <b>Qtd. Em KG</b>!");
		}else {
		}
	}
	
	private DynamicVO getTCSCON(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		return VO;
	}
	
	private DynamicVO getTGFPRO(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		return VO;
	}
	
	private DynamicVO getTGFVOA(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("VolumeAlternativo");
		DynamicVO VO = DAO.findOne("CODPROD=? AND CODVOL=?",new Object[] { produto, "KG"});
		return VO;
	} 

}
