package br.com.NFCE;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class eventoAjustarContratoNFCE implements EventoProgramavelJava {
	
	/**
	 * Objeto para inserir o contrato na nota NFCE quando o patrimonio está preenchido e o contrato está vazio
	 * 
	 * 09/02/2021 13:09 inserir funcionalidade para ajustar o CR do contrato.
	 */
	
	BigDecimal nota;
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}
	
	private void start(PersistenceEvent arg0){
		try {
			
			DynamicVO VO = (DynamicVO) arg0.getVo();
			
			String patrimonio = VO.asString("AD_CODBEM");
			BigDecimal top = VO.asBigDecimal("CODTIPOPER");
			BigDecimal contratoAtual = VO.asBigDecimal("NUMCONTRATO");
			nota = VO.asBigDecimal("NUNOTA");
			
			if(top.equals(new BigDecimal(1108)) && patrimonio!=null && contratoAtual.equals(new BigDecimal(0))){
			
					BigDecimal contrato = getNumeroDoContrato(patrimonio);
					
					if(contrato!=null){
						VO.setProperty("NUMCONTRATO", contrato);
						BigDecimal cr = getCR(contrato);
						VO.setProperty("CODCENCUS", cr);
						//System.out.println("-------->Contrato alterado da nota!");
					}

			}
			
		} catch (Exception e) {
			System.out.println("------> Não foi possivel descobrir o contrato da nota NFCE: "+nota+"\n"+ e.getMessage());
		}
		
	}
	
	private BigDecimal getNumeroDoContrato(String codbem) throws Exception{		
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		
		BigDecimal numcontrato = VO.asBigDecimal("NUMCONTRATO");
		
		return numcontrato;
	}
	
	private BigDecimal getCR(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		
		BigDecimal cr = VO.asBigDecimal("CODCENCUS");
		
		return cr;
	}

}
