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
						throw new Error("<br/><b>OPS</b><br/><br/>tipo de contrato <b>Assinatura</b>! Valor 0 não é válido ! Preencha o campo <b>Qtd. Prevista</b> ou <b>Qtd. Em KG</b> com uma quantidade maior que zero!");
						
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
		DynamicVO TCSCON = getTCSCON(contrato);
		String tipoContrato = TCSCON.asString("AD_TIPCONT");

		if("A".equals(tipoContrato)) {
			
			BigDecimal produto = VO.asBigDecimal("CODPROD");
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
			
			
			if(grupo.intValue()<500000 || grupo.intValue()>=600000) {
				
				//VO.setProperty("AD_FRANQUIA", "S");
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
							
							if(qtdprev!=null && qtdEmKg==null) {
								BigDecimal qtd = calculo(qtdprev,qtdEmKg,TGFVOA,VO,"P");
								VO.setProperty("AD_QTDEMKG", qtd);
							}
							
							if(qtdprev==null && qtdEmKg!=null) {
								BigDecimal qtd = calculo(qtdprev,qtdEmKg,TGFVOA,VO,"K");
								VO.setProperty("QTDEPREVISTA", qtd);
							}
							
							if(qtdprev!=oldqtdprev && qtdEmKg==oldqtdEmKg) {
								BigDecimal qtd = calculo(qtdprev,qtdEmKg,TGFVOA,VO,"P"); 
								VO.setProperty("AD_QTDEMKG", qtd);
							}
							
							if(qtdEmKg!=oldqtdEmKg && qtdprev==oldqtdprev) {
								BigDecimal qtd = calculo(qtdprev,qtdEmKg,TGFVOA,VO,"K");
								VO.setProperty("QTDEPREVISTA", qtd);
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
		DynamicVO TCSCON = getTCSCON(contrato);
		String tipoContrato = TCSCON.asString("AD_TIPCONT"); //contrato tipo A - Assinatura

		if("A".equals(tipoContrato)) {
			
			BigDecimal produto = VO.asBigDecimal("CODPROD");
			DynamicVO TGFPRO = getTGFPRO(produto);
			String unidadePadrao = TGFPRO.asString("CODVOL");
			BigDecimal grupo = TGFPRO.asBigDecimal("CODGRUPOPROD");
			
			if(grupo.intValue()<500000 || grupo.intValue()>=600000) { //valida apenas produtos que não são máquinas
				
				//VO.setProperty("AD_FRANQUIA", "S");
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
									BigDecimal qtd = calculo(qtdprev,qtdEmKg,TGFVOA,VO,"P"); //quantidade prevista preenchida
									VO.setProperty("AD_QTDEMKG", qtd);
								}
								
								if(qtdprev==null && qtdEmKg!=null) {
									BigDecimal qtd = calculo(qtdprev,qtdEmKg,TGFVOA,VO,"K"); //qtd em quilo preenchida
									VO.setProperty("QTDEPREVISTA", qtd);
								}
															
							}

						}else {
							
							if(qtdprev!=null && qtdEmKg==null) {
								VO.setProperty("AD_QTDEMKG", qtdprev);
							}
							
							if(qtdprev==null && qtdEmKg!=null) {
								VO.setProperty("QTDEPREVISTA", qtdEmKg);
							}
						}
					}	
				}	
			}	
		}	
	}
	
	private BigDecimal calculo(BigDecimal qtdprev, BigDecimal qtdEmKg, DynamicVO TGFVOA, DynamicVO VO, String tipo) {
		 //preenchendo atraves da qtd prevista.
			String opcao = TGFVOA.asString("DIVIDEMULTIPLICA");//M = multiplica D=Divide
			BigDecimal qtdTgfvoa = TGFVOA.asBigDecimal("QUANTIDADE").setScale(2, RoundingMode.HALF_EVEN);
			BigDecimal qtdparaCalculo = null;
			
			if(qtdTgfvoa==null) {
				qtdparaCalculo = new BigDecimal(1);
			}else if(qtdTgfvoa.intValue()==0) {
				qtdparaCalculo = new BigDecimal(1);
			}else {
				qtdparaCalculo = qtdTgfvoa;
			}
			
			BigDecimal qtd = null;
			
			if("P".equals(tipo)) {
				if("M".equals(opcao)) {
					qtd = qtdprev.divide(qtdparaCalculo, 2, RoundingMode.HALF_UP);
				}else if("D".equals(opcao)) {
					qtd = qtdprev.multiply(qtdparaCalculo);
				}else {
					qtd = qtdprev;
				}
			}else {
				if("M".equals(opcao)) {
					qtd = qtdEmKg.multiply(qtdparaCalculo);
				}else if("D".equals(opcao)) {
					qtd = qtdEmKg.divide(qtdparaCalculo, 2, RoundingMode.HALF_UP);
				}else {
					qtd = qtdEmKg;
				}
			}
			
			return qtd;
	}
	
	
	private void ValidaNullEZero(BigDecimal qtdprev, BigDecimal qtdEmKg) {
		if(qtdprev==null && qtdEmKg==null) {
			throw new Error("<br/><b>OPS</b><br/><br/>tipo de contrato <b>Assinatura</b>! Preencha o campo <b>Qtd. Prevista</b> ou <b>Qtd. Em KG</b>!");
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
