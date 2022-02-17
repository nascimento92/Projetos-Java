package br.com.gsn.astro;

import java.math.BigDecimal;

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
		start(arg0);		
	}
	
	public void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		
		DynamicVO TCSCON = getTCSCON(contrato);
		String tipoContrato = TCSCON.asString("AD_TIPCONT"); //contrato tipo A - Assinatura
		
		DynamicVO TGFPRO = getTGFPRO(produto);
		String unidadePadrao = TGFPRO.asString("CODVOL");
		
		if("A".equals(tipoContrato)) {
			String tipoFranquia = VO.asString("AD_FRANQUIA"); 
			
			if("S".equals(tipoFranquia)) {
				
				BigDecimal qtdprev = VO.asBigDecimal("QTDEPREVISTA");
				
				if(qtdprev==null) {
					throw new Error("<br/><b>OPS</b><br/><br/>tipo de contrato <b>Assinatura</b>! Preencha o campo Qtd. Prevista!");
				}
				
				if(!"KG".equals(unidadePadrao)) {
					DynamicVO TGFVOA = getTGFVOA(produto);
					if(TGFVOA!=null) {
						
						String opcao = TGFVOA.asString("DIVIDEMULTIPLICA");//M = multiplica D=Divide
						BigDecimal qtdTgfvoa = TGFVOA.asBigDecimal("QUANTIDADE");
						BigDecimal qtdEmKG = null;
						
						if("M".equals(opcao)) {
							qtdEmKG = qtdprev.divide(qtdTgfvoa);
						}else if("D".equals(opcao)) {
							qtdEmKG = qtdprev.multiply(qtdTgfvoa);
						}else {
							qtdEmKG = qtdprev;
						}
						
						VO.setProperty("AD_QTDEMKG", qtdEmKG);
						
					}else {
						throw new Error("<br/><b>OPS</b><br/><br/>Produto não pode ser inserido! tipo de contrato <b>Assinatura</b>! não foi encontrado uma unidade alternativa em KG.");
					}
				}
				
			}
			
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
