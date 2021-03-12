package br.com.NFCE;

import java.math.BigDecimal;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class eventoAjustarContratoNFCE implements EventoProgramavelJava {
	
	/**
	 * Objeto para inserir o contrato na nota NFCE quando o patrimonio está preenchido e o contrato está vazio
	 * 
	 * 09/02/2021 13:09 inserir funcionalidade para ajustar o CR do contrato.
	 * 01/03/2021 09:23 ajustar a rotina de alteração do CR.
	 * 11/03/2021 14:20 chamado 633321 ajustar o CR de todas as notas.
	 */
	BigDecimal nota;
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		//start(arg0);
		
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
			BigDecimal contrato = getNumeroDoContrato(patrimonio);
			
			if(top.equals(new BigDecimal(1108)) && patrimonio!=null && contratoAtual.equals(new BigDecimal(0))){				
				if(contrato.intValue()!=0){
					VO.setProperty("NUMCONTRATO", contrato);
				}
			}
			
			if(contratoAtual.intValue()!=0 && contratoAtual!=null) {
				BigDecimal cr = getCR(contratoAtual);
				VO.setProperty("CODCENCUS", cr);
			}
			
			if(contrato.intValue()!=0){
				BigDecimal cr = getCR(contrato);
				VO.setProperty("CODCENCUS", cr);
			}
			
		} catch (Exception e) {
			salvarException("[start] nao foi possivel alterar o CR ou o Contrato! nota: "+nota+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
	}
	
	private BigDecimal getNumeroDoContrato(String codbem) throws Exception{		
		BigDecimal numcontrato = BigDecimal.ZERO;
		
		if(codbem==null) {
			codbem="0";
		}
		
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		if(VO!=null) {
			numcontrato = VO.asBigDecimal("NUMCONTRATO");
		}
		
		return numcontrato;
	}
	
	private BigDecimal getCR(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		
		BigDecimal cr = VO.asBigDecimal("CODCENCUS");
		
		return cr;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "eventoAjustarContratoNFCE");
			VO.setProperty("PACOTE", "br.com.NFCE");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
