package br.com.NFCE;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoAlterarEmailNFCE implements EventoProgramavelJava {

	/**
	 * Este objeto altera o e-mail das NFCE que serão enviadas por e-mail.
	 * esta alteração acontece na TMDFMG
	 */
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		if(validaSeEhTop1108(arg0)){
			alteraEmailNFCE(arg0);
		}
		
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
		
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	
	public boolean validaSeEhTop1108(PersistenceEvent arg0) throws Exception{
		boolean valida = false;
			
		BigDecimal nunotaRetorno = validaSeTemNunota(arg0);
			
			if(nunotaRetorno.intValue()>0){
				
				JapeWrapper tgfcabDAO = JapeFactory.dao("CabecalhoNota");
				DynamicVO tgfcabVO = tgfcabDAO.findOne("NUNOTA=?",new Object[] { nunotaRetorno });
				
				BigDecimal top = tgfcabVO.asBigDecimal("CODTIPOPER");
				
				if(top.equals(new BigDecimal(1108))){
					valida = true;
				}
			}	
		
		return valida;
	}
	
	public BigDecimal validaSeTemNunota(PersistenceEvent arg0){
		
		BigDecimal retorno = new BigDecimal(0);
		BigDecimal nunota = getFilaVO(arg0).asBigDecimal("NUCHAVE");
		
		if(nunota!=null){
			return nunota;
		}else{
			return retorno;
		}	
	}
	
	public DynamicVO getFilaVO(PersistenceEvent arg0){
		
		DynamicVO filaVO = (DynamicVO) arg0.getVo();
		
		return filaVO;
	}
	
	public void alteraEmailNFCE(PersistenceEvent arg0) throws Exception{
		
		BigDecimal nunota = getFilaVO(arg0).asBigDecimal("NUCHAVE");
		BigDecimal codfila = getFilaVO(arg0).asBigDecimal("CODFILA");
		
		if (nunota != null) {

			JapeWrapper tgfcabDAO = JapeFactory.dao("CabecalhoNota");
			DynamicVO tgfcabVO = tgfcabDAO.findOne("NUNOTA=?",new Object[] { nunota });
			
			String emailNFCE = tgfcabVO.asString("AD_EMAILNFCE");
			
			if(emailNFCE!=null){
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("MSDFilaMensagem", codfila);
				EntityVO NVO = PersistentLocalEntity.getValueObject();
				DynamicVO filaVO = (DynamicVO) NVO;
				
				filaVO.setProperty("EMAIL", emailNFCE);
				
				PersistentLocalEntity.setValueObject(NVO);
				
				System.out.println("******* A NFC-e: "+nunota+" teve seu e-mail alterado com sucesso! *******");
			}

		}
	}

}

