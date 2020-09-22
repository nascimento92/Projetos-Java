package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_cc_evento_validaPatrimonio implements EventoProgramavelJava {

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
		alteraTipoCancelamento(arg0);
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idFlow = VO.asBigDecimal("IDINSTPRN");
		String patrimonio = VO.asString("CODBEM");
		
		BigDecimal contratoFlow = getContratoFlow(idFlow);
		BigDecimal contratoEnderecamento = getContratoEnderecamento(patrimonio);
		
		if(contratoEnderecamento==null) {
			throw new PersistenceException("<br/><br/><br/><b>Patrimônio Inválido!</b><br/><br/><br/>");
		}

		if (contratoEnderecamento.intValue() != contratoFlow.intValue()) {
			throw new PersistenceException(
					"<br/><br/><br/><b>Patrimônio não pertence ao contrato " + contratoFlow + "!</b><br/><br/><br/>");
		}
		
		VO.setProperty("IDPLANTA", getEnderecamento(patrimonio).asBigDecimal("ID"));
		VO.setProperty("NUMCONTRATO", contratoEnderecamento);
		 
	}
	
	private BigDecimal getContratoFlow(BigDecimal idFlow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_FORMCANCELAMENTO");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idFlow });
		return VO.asBigDecimal("NUMCONTRATO");
	}
	
	private BigDecimal getContratoEnderecamento(String patrimonio) throws Exception {
		BigDecimal contrato = null;
		JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		
		if(VO!=null) {
			contrato= VO.asBigDecimal("NUMCONTRATO");
		}
		
		return contrato;
	}
	
	private DynamicVO getEnderecamento(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	private void alteraTipoCancelamento(PersistenceEvent arg0) {
		DynamicVO VOS = (DynamicVO) arg0.getVo();
		BigDecimal idFlow = VOS.asBigDecimal("IDINSTPRN");
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_FORMCANCELAMENTO",
					"this.IDINSTPRN=?", new Object[] { idFlow }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("TIPOCANCEL", "1");

				itemEntity.setValueObject(NVO);
			}

			
		} catch (Exception e) {
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
}
