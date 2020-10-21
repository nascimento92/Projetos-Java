package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_registraAlteracaoTecla implements EventoProgramavelJava {

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
		deletarTecla(arg0);		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		inserirTecla(arg0);	
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		star(arg0);	
	}
	
	private void star(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		salvaDadosNaTelainstalacaoAbaPlanograma(oldVO,newVO);
		insereNaTelaLogDasTeclas(oldVO,newVO);
	}
	
	private void salvaDadosNaTelainstalacaoAbaPlanograma(DynamicVO oldVO,DynamicVO newVO) {
		try {
			
			String patrimonio = oldVO.asString("CODBEM");
			String tecla = oldVO.asBigDecimal("TECLA").toString();
			BigDecimal produto = oldVO.asBigDecimal("CODPROD");
			
			String microMarketing = validaSeEhMicroMarketing(patrimonio);
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> query = null;
			
			if("S".equals(microMarketing)) {
				query = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma",
						"this.CODBEM=? AND this.CODPROD=? ", new Object[] { patrimonio, produto }));
			}else if("N".equals(microMarketing)) {
				query = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma",
						"this.CODBEM=? AND this.TECLA=? ", new Object[] { patrimonio, tecla }));
			}else {
				query = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma",
						"this.CODBEM=? AND this.TECLA=? ", new Object[] { patrimonio, tecla }));
			}
			
			for (Iterator<?> Iterator = query.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;
				
				if(VO!=null) {

					if ("N".equals(microMarketing)) {
						VO.setProperty("TECLA", newVO.asBigDecimal("TECLA").toString());
					}					 
					
					VO.setProperty("CODPROD", newVO.asBigDecimal("CODPROD"));
					VO.setProperty("NIVELPAR", newVO.asBigDecimal("AD_NIVELPAR"));
					VO.setProperty("CAPACIDADE", newVO.asBigDecimal("AD_CAPACIDADE"));
					VO.setProperty("NIVELALERTA", newVO.asBigDecimal("AD_NIVELALERTA"));
					VO.setProperty("VLRPAR", newVO.asBigDecimal("VLRPAR"));
					VO.setProperty("VLRFUN", newVO.asBigDecimal("VLRFUN"));

					itemEntity.setValueObject(NVO);
				}	
			}

		} catch (Exception e) {
			System.out.println("##[evento_registraAlteracaoTecla]## - Nao foi possivel registrar a alteracao na tela planograma!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private String validaSeEhMicroMarketing(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		 String micromarketing = VO.asString("TOTEM");
		 if(micromarketing==null) {
			 micromarketing="N";
		 }
		 return micromarketing;
	}
	
	private void insereNaTelaLogDasTeclas(DynamicVO oldVO,DynamicVO newVO) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOGTECLAS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CAPACIDADE", newVO.asBigDecimal("AD_CAPACIDADE"));
			VO.setProperty("CAPACIDADEANT", oldVO.asBigDecimal("AD_CAPACIDADE"));
			VO.setProperty("CODBEM", newVO.asString("CODBEM"));
			VO.setProperty("CODPROD", newVO.asBigDecimal("CODPROD"));
			VO.setProperty("CODPRODANT", oldVO.asBigDecimal("CODPROD"));
			VO.setProperty("CODUSU", getUsuLogado());
			VO.setProperty("DTALTER", TimeUtils.getNow());
			VO.setProperty("NIVELALERTA", newVO.asBigDecimal("AD_NIVELALERTA"));
			VO.setProperty("NIVELALERTAANT", oldVO.asBigDecimal("AD_NIVELALERTA"));
			VO.setProperty("NIVELPAR", newVO.asBigDecimal("AD_NIVELPAR"));
			VO.setProperty("NIVELPARANT", oldVO.asBigDecimal("AD_NIVELPAR"));
			VO.setProperty("TECLA", newVO.asBigDecimal("TECLA").toString());
			VO.setProperty("VLRFUN", newVO.asBigDecimal("VLRFUN"));
			VO.setProperty("VLRFUNANT", oldVO.asBigDecimal("VLRFUN"));
			VO.setProperty("VLRPAR", newVO.asBigDecimal("VLRPAR"));
			VO.setProperty("VLRPARANT", oldVO.asBigDecimal("VLRPAR"));
			
			dwfFacade.createEntity("AD_LOGTECLAS", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("##[evento_registraAlteracaoTecla]## - Nao foi possivel registrar a alteracao na tela log!");
			e.getMessage();
			e.getCause();
			e.printStackTrace();
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
}
	
	//insert
	private void inserirTecla(PersistenceEvent arg0) throws Exception {
		DynamicVO teclas = (DynamicVO) arg0.getVo();
		String patrimonio = teclas.asString("CODBEM");
		String micromarketing = validaSeEhMicroMarketing(patrimonio);
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCPlanograma");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("CODPROD", teclas.asBigDecimal("CODPROD"));
			VO.setProperty("NIVELPAR", teclas.asBigDecimal("AD_NIVELPAR"));
			VO.setProperty("CAPACIDADE", teclas.asBigDecimal("AD_CAPACIDADE"));
			VO.setProperty("NIVELALERTA", teclas.asBigDecimal("AD_NIVELALERTA"));
			VO.setProperty("VLRPAR", teclas.asBigDecimal("VLRPAR"));
			VO.setProperty("VLRFUN", teclas.asBigDecimal("VLRFUN"));
			VO.setProperty("ESTOQUE", new BigDecimal(0));
			VO.setProperty("AD_ABASTECER", "S");
			
			if("S".equals(micromarketing)) {
				VO.setProperty("TECLA", new BigDecimal(0).toString());
			}else {
				VO.setProperty("TECLA", teclas.asBigDecimal("TECLA").toString());
			}
			
			dwfFacade.createEntity("GCPlanograma", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## [evento_registraAlteracaoTecla] ## - Nao foi possivel cadastrar a tecla!");
			e.getMessage();
			e.getCause();
		}
	}

	//delete
	private void deletarTecla(PersistenceEvent arg0) throws Exception {
		DynamicVO teclas = (DynamicVO) arg0.getVo();
		String patrimonio = teclas.asString("CODBEM");
		String micromarketing = validaSeEhMicroMarketing(patrimonio);
		
		try {
			
			if("S".equals(micromarketing)) {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				dwfFacade.removeByCriteria(new FinderWrapper("GCPlanograma", "this.CODBEM=? and this.CODPROD=?",new Object[] {patrimonio,teclas.asBigDecimal("CODPROD")}));
			}else {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				dwfFacade.removeByCriteria(new FinderWrapper("GCPlanograma", "this.CODBEM=? and this.TECLA=?",new Object[] {patrimonio,teclas.asBigDecimal("TECLA")}));
			}
			
			
		} catch (Exception e) {
			System.out.println("## [evento_registraAlteracaoTecla] ## - Nao foi possivel excluir a tecla!");
			e.getMessage();
			e.getCause();
		}
	}
}
