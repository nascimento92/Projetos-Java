package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;
import java.util.Collection;
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

public class flow_cc_evento_atualizaDados implements EventoProgramavelJava {

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
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		 
		
		if(contrato!=null) {
			VO.setProperty("CODCENCUS", getTcsCon(contrato).asBigDecimal("CODCENCUS"));
			VO.setProperty("DTCONTRATO", getTcsCon(contrato).asTimestamp("DTCONTRATO"));
			VO.setProperty("CODPARC", getTcsCon(contrato).asBigDecimal("CODPARC"));
			
			String ie = getTgfPar(getTcsCon(contrato).asBigDecimal("CODPARC")).asString("IDENTINSCESTAD");
			if(ie!=null) {
				VO.setProperty("TEMIE", "S");
			}else {
				VO.setProperty("TEMIE", "N");
			}
			
			String tipoCancelamento = VO.asString("TIPOCANCEL");
			BigDecimal idflow = VO.asBigDecimal("IDINSTPRN");
			validaCancelamento(tipoCancelamento,contrato,idflow);
		}
	}
		
	private void validaCancelamento(String tipo, BigDecimal contrato, BigDecimal idflow) {
		if("2".equals(tipo)) {
			limpaPatrimonios(idflow);
			cadastrarTodosOsPatrimonios(contrato,idflow);
		}
		
		if("1".equals(tipo)) {
			limpaPatrimonios(idflow);	
		}
	}
	
	private void limpaPatrimonios(BigDecimal idflow) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PATCANCELAMENTO", "this.IDINSTPRN=?",new Object[] {idflow}));
			
		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private void cadastrarTodosOsPatrimonios(BigDecimal contrato, BigDecimal idflow) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("ENDERECAMENTO", "this.NUMCONTRATO = ? ", new Object[] { contrato }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject())
						.wrapInterface(DynamicVO.class);

				//insert
				EntityVO NPVO = dwfEntityFacade.getDefaultValueObjectInstance("AD_PATCANCELAMENTO");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("IDINSTPRN", idflow);
				VO.setProperty("IDINSTTAR", new BigDecimal(0));
				VO.setProperty("CODREGISTRO", new BigDecimal(1));
				VO.setProperty("CODBEM", DynamicVO.asString("CODBEM"));
				VO.setProperty("IDTAREFA", "UserTask_1rgod34");
				VO.setProperty("IDPLANTA", DynamicVO.asBigDecimal("ID"));
				VO.setProperty("NUMCONTRATO", contrato);
				
				dwfEntityFacade.createEntity("AD_PATCANCELAMENTO", (EntityVO) VO);

			}
			
		} catch (Exception e) {
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private DynamicVO getTcsCon(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		return VO;
	}
	
	private DynamicVO getTgfPar(BigDecimal codparc) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Parceiro");
		DynamicVO VO = DAO.findOne("CODPARC=?",new Object[] { codparc });
		return VO;
	}
	
}
