package br.com.ManutencaoPreventiva;

import java.math.BigDecimal;
//import java.sql.Timestamp;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.GregorianCalendar;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoCadastrarPreventiva implements EventoProgramavelJava {
	
	/**
	 *  04/10/19 15:44 vs 1.0 - Objeto sendo utilizado AD_PATRIMONIO quando um patrimonio é marcado como sim ele é registrado na tela controle man. prev.
	 *  20/06/22 15:40 vs 1.1 - Ajustado para preencher o campo DTPROXMANUTENCAO com a data atual.
	 */
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {		
		
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		insert(arg0);
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		insert(arg0);
		
	}
	
	private void insert(PersistenceEvent arg0) throws Exception{
		DynamicVO enderecamentoVO = (DynamicVO) arg0.getVo();

		String codbem = enderecamentoVO.asString("CODBEM");
		String manprev = enderecamentoVO.asString("MANPREV");

		if (manprev != null && manprev.equals("1")) {
			if (!verificaSeExisteManPreventiva(codbem)) {
				// throw new PersistenceException("Cadastrar!");
				cadastrarPatrimonio(codbem);
			}
		}
	}
	
	
	private boolean verificaSeExisteManPreventiva(String codbem) throws Exception{
		
		boolean existe=false;
		
		JapeWrapper DAO = JapeFactory.dao("AD_MANUPREV");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });

		if (VO!=null){
			return existe=true;
		}else{
			return existe;
		}	
	}
	
	private void cadastrarPatrimonio(String codbem) throws Exception{
			
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_MANUPREV");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("CODBEM", codbem);
		VO.setProperty("PRAZO", new BigDecimal(60));
		VO.setProperty("INSERIDOAUT", "S");
		VO.setProperty("DTPROXMANUTENCAO", TimeUtils.getNow());
		
		dwfFacade.createEntity("AD_MANUPREV", (EntityVO) VO);

	}
	


}

