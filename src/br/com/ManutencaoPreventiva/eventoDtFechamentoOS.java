package br.com.ManutencaoPreventiva;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoDtFechamentoOS implements EventoProgramavelJava {
	/**
	 * 04/10/19 15:42 - Criação do Objeto insetido na TCSOSE toda OS que for preventiva e for encerrada, irá alimentar os campos de fechamento na tela controle de man. prev.
	 * 21/10/19 17:32 - Inserida a funcionalidade de se a OS for uma man. prev. programada ou man. prev ele irá atualizar as informações na tela controle de manutenção preventiva, isso acontece pq o sac também abre OS de man preventivas.
	 */

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		alteraData(arg0);
		
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
		start(arg0);	
		alteraData(arg0);
		alterarStatus(arg0);
	}
	
	private void start(PersistenceEvent arg0) throws Exception{
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		String situacao = VO.asString("SITUACAO");
		String manPreventiva = VO.asString("AD_MANPREVENTIVA");
		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal statusOS = VO.asBigDecimal("CODCOS");
		Timestamp dataFechamento = VO.asTimestamp("DTFECHAMENTO");
		Timestamp dataAbertura = VO.asTimestamp("DHCHAMADA");
		BigDecimal usuarioResponsavel = VO.asBigDecimal("CODUSURESP");
		BigDecimal codparc = VO.asBigDecimal("CODPARC");
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		
		if(manPreventiva!=null && situacao!=null){
			
			if("F".equals(situacao) && "S".equals(manPreventiva) && statusOS!=null){
				
				atualizaInformacao(numos,dataFechamento,statusOS);
			}
						
		}
		
		if("F".equals(situacao) && statusOS!=null){
			
			if("N".equals(manPreventiva) || manPreventiva==null){

				try {
					
					validaSeEhUmaOsDeManutencaoPreventiva(numos,dataAbertura,usuarioResponsavel,codparc,contrato,statusOS);
					
				} catch (Exception e) {
					System.out.println("Não foi possivel alterar a OS: "+numos+e.getMessage());
				}
			}
		}
				
	}
	

	private void atualizaInformacao(BigDecimal numos, Timestamp dataFechamento,BigDecimal cancelada) throws Exception{
		//Histórico
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_MANUPREVOS","this.NUMOS=?", new Object[] { numos }));
		
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		DynamicVO VO = (DynamicVO) NVO;
		
		if(VO!=null){
			VO.setProperty("DTFIMOS", dataFechamento);
			if(cancelada.equals(new BigDecimal(5))){
				VO.setProperty("CANCELADA", "S");
			}
			itemEntity.setValueObject(NVO);
		}
		}
		
		//Manuprev
		dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> b = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_MANUPREV","this.ULTIMAOS=?", new Object[] { numos }));
		
		for (Iterator<?> Iterator = b.iterator(); Iterator.hasNext();) {
		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		DynamicVO VO = (DynamicVO) NVO;
		
		BigDecimal prazo = VO.asBigDecimal("PRAZO");
		
		if(VO!=null){
			VO.setProperty("DTFIMOS", dataFechamento);
			VO.setProperty("OBSERVACAOINTERNA", "");
			if(cancelada.intValue()!=5){
				VO.setProperty("DTPROXMANUTENCAO", addDias(dataFechamento,prazo));
			}
			itemEntity.setValueObject(NVO);
		}
		}
}
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	private void alteraData(PersistenceEvent arg0){
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dataChamada = VO.asTimestamp("DHCHAMADA");
		String manPrev = VO.asString("AD_MANPREVENTIVA");
		
		if(manPrev!=null && "S".equals(manPrev)){
			VO.setProperty("DTPREVISTA", addDias(dataChamada, new BigDecimal(7)));
		}
	}
	
	private void alterarStatus(PersistenceEvent arg0){
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal codcos = VO.asBigDecimal("CODCOS");
		String situacao = VO.asString("SITUACAO");
		
		if("F".equals(situacao) && codcos.equals(new BigDecimal(1))){
			VO.setProperty("CODCOS", new BigDecimal(4));
		}
	}
	
	private void validaSeEhUmaOsDeManutencaoPreventiva(BigDecimal numos, Timestamp dataAbertura, BigDecimal usuarioResponsavel, BigDecimal codparc, BigDecimal contrato, BigDecimal statusOS) throws Exception{
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();	
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico","this.NUMOS = ? ", new Object[] { numos }));
			
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			
			BigDecimal motivo = DynamicVO.asBigDecimal("CODOCOROS");
			String patrimonio = DynamicVO.asString("SERIE");
			
				if(motivo.equals(new BigDecimal(14))){
					
					if(patrimonio!=null){
						
						if(validaSeExisteNaTelaControleDeManutencaoPreventiva(patrimonio)){
							atualizaInformacaoPreventivas(numos,patrimonio,dataAbertura,usuarioResponsavel,codparc,contrato,statusOS);
						}else{
							cadastraNaTelaControleManutencaoPreventiva(numos,patrimonio,dataAbertura,usuarioResponsavel,codparc,contrato);
						}
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("Não foi possivel atualizar as informações da OS: "+numos+e.getMessage());
		}
		
		
	}
	
	private boolean validaSeExisteNaTelaControleDeManutencaoPreventiva(String patrimonio) throws Exception{
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("AD_MANUPREV");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		
		if(VO!=null){
			valida = true;
		}

		return valida;
	}
	
	private void cadastraNaTelaControleManutencaoPreventiva(BigDecimal numos, String patrimonio, Timestamp dataAbertura, BigDecimal usuarioResponsavel, BigDecimal codparc, BigDecimal contrato) throws Exception{
		
		Timestamp dtatual = new Timestamp(System.currentTimeMillis());
		
		//man preventiva
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_MANUPREV");
		DynamicVO VO = (DynamicVO) NPVO;
		
		VO.setProperty("CODBEM", patrimonio);
		VO.setProperty("PRAZO", new BigDecimal(60));
		VO.setProperty("DTPRIMEIRA", dataAbertura);
		VO.setProperty("ULTIMAOS", numos);
		VO.setProperty("DTFIMOS", dtatual);
		VO.setProperty("DTULTMANUTENCAO", dataAbertura);
		VO.setProperty("DTPROXMANUTENCAO", addDias(dtatual, new BigDecimal(60)));
		
		dwfFacade.createEntity("AD_MANUPREV", (EntityVO) VO);
		
		//historico
		dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO2 = dwfFacade.getDefaultValueObjectInstance("AD_MANUPREVOS");
		DynamicVO VO2 = (DynamicVO) NPVO2;
		
		VO2.setProperty("NUMOS", numos);
		VO2.setProperty("CODBEM", patrimonio);
		VO2.setProperty("DTABERTURA", dataAbertura);
		VO2.setProperty("DTFIMOS", dtatual);
		VO2.setProperty("CODUSU", usuarioResponsavel);
		VO2.setProperty("ABERTASAC", "S");
		VO2.setProperty("CODPARC", codparc);
		VO2.setProperty("NUMCONTRATO", contrato);
		
		dwfFacade.createEntity("AD_MANUPREVOS", (EntityVO) VO2);

	}
	
	private void atualizaInformacaoPreventivas(BigDecimal numos, String patrimonio, Timestamp dataAbertura, BigDecimal usuarioResponsavel, BigDecimal codparc, BigDecimal contrato, BigDecimal statusOS) throws Exception{
		
		//Man preventiva
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_MANUPREV", patrimonio);
		EntityVO NVO = PersistentLocalEntity.getValueObject();
		DynamicVO VO = (DynamicVO) NVO;
		
		BigDecimal prazo = VO.asBigDecimal("PRAZO");
		Timestamp dtatual = new Timestamp(System.currentTimeMillis());
		Timestamp dataPrimeira = VO.asTimestamp("DTPRIMEIRA");
		
		Timestamp proximaManutencao = VO.asTimestamp("DTPROXMANUTENCAO");
		
		VO.setProperty("ULTIMAOS", numos);
		VO.setProperty("DTFIMOS", dtatual);
		VO.setProperty("DTULTMANUTENCAO", dataAbertura);
		
		if(dataPrimeira==null){
			VO.setProperty("DTPRIMEIRA", dataAbertura);
		}
		
		if (statusOS.intValue()!=5) {
			VO.setProperty("DTPROXMANUTENCAO", addDias(dtatual, prazo));
		}else{
			if(proximaManutencao==null){
				VO.setProperty("DTPROXMANUTENCAO", dataAbertura);
			}
		}
		
	
		PersistentLocalEntity.setValueObject((EntityVO) VO);
		
		//histórico das OS
		dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_MANUPREVOS");
		DynamicVO VO2 = (DynamicVO) NPVO;
		
		VO2.setProperty("NUMOS", numos);
		VO2.setProperty("CODBEM", patrimonio);
		VO2.setProperty("DTABERTURA", dataAbertura);
		VO2.setProperty("DTFIMOS", dtatual);
		VO2.setProperty("CODUSU", usuarioResponsavel);
		VO2.setProperty("ABERTASAC", "S");
		VO2.setProperty("CODPARC", codparc);
		VO2.setProperty("NUMCONTRATO", contrato);
		
		if (statusOS.equals(new BigDecimal(5))) {
			VO2.setProperty("CANCELADA", "S");
		}
		
		dwfFacade.createEntity("AD_MANUPREVOS", (EntityVO) VO2);
	}
		
}

