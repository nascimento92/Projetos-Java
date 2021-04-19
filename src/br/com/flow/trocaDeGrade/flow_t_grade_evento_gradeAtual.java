package br.com.flow.trocaDeGrade;

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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_t_grade_evento_gradeAtual implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);		
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
		deletar(arg0);		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idFlow = VO.asBigDecimal("IDINSTPRN");
		BigDecimal idTarefa = VO.asBigDecimal("IDINSTTAR");
		BigDecimal codRegistro = VO.asBigDecimal("CODREGISTRO");
		String idCard = VO.asString("IDTAREFA");
		String patrimonio = VO.asString("CODBEM");
		
		getTeclas(idFlow,idTarefa,codRegistro,idCard,patrimonio);
	}
	
	public void deletar(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idFlow = VO.asBigDecimal("IDINSTPRN");
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GRADEATUAL", "this.IDINSTPRN=?",new Object[] {idFlow}));
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GRADEFUTURA", "this.IDINSTPRN=?",new Object[] {idFlow}));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public void getTeclas(BigDecimal idFlow,BigDecimal idTarefa,BigDecimal codRegistro,String idCard,String patrimonio) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma","this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
			String tecla = DynamicVO.asString("TECLA");
				
			insereTeclaAtual(idFlow,idTarefa,codRegistro,idCard,patrimonio,produto,tecla);
			insereTeclaFutura(idFlow,idTarefa,codRegistro,idCard,patrimonio,produto,tecla);
			
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public void insereTeclaAtual(BigDecimal idFlow,BigDecimal idTarefa,BigDecimal codRegistro,String idCard,String patrimonio, 
			BigDecimal produto, String tecla) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_GRADEATUAL");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("IDINSTPRN", idFlow);
			VO.setProperty("IDINSTTAR", idTarefa);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODREGISTRO", codRegistro);
			VO.setProperty("IDTAREFA", idCard);
			
			dwfFacade.createEntity("AD_GRADEATUAL", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	public void insereTeclaFutura(BigDecimal idFlow,BigDecimal idTarefa,BigDecimal codRegistro,String idCard,String patrimonio, 
			BigDecimal produto, String tecla) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_GRADEFUTURA");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("IDINSTPRN", idFlow);
			VO.setProperty("IDINSTTAR", idTarefa);
			VO.setProperty("CODPROD", produto);
			VO.setProperty("TECLA", tecla);
			VO.setProperty("CODREGISTRO", codRegistro);
			VO.setProperty("IDTAREFA", idCard);
			
			dwfFacade.createEntity("AD_GRADEFUTURA", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
