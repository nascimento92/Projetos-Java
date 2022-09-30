package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.BigDecimalUtil;
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
		inserirTecla(arg0);
		
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
		BeforeInsert(arg0);	
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		star(arg0);	
	}
	
	private void star(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		String patrimonio = newVO.asString("CODBEM");
		BigDecimal tecla = newVO.asBigDecimal("TECLA");
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		
		if(validaSeOhPatrimonioEstaNaTelaDeInstalacoes(patrimonio)) { //valida se existe nas instalações
			
			if("S".equals(validaSeEhMicroMarketing(patrimonio))) {
				tecla = new BigDecimal(0);
			}
			
			if(verificaSeExisteNaAbaInstalacoes(patrimonio,tecla,produto)) {
				salvaDadosNaTelainstalacaoAbaPlanograma(oldVO,newVO);
			}else {
				inserirTecla(arg0);
			}
			
			insereNaTelaLogDasTeclas(oldVO,newVO);
		}
	}
	
	//atualizar
	private void salvaDadosNaTelainstalacaoAbaPlanograma(DynamicVO oldVO,DynamicVO newVO) throws Exception {
		String patrimonio = newVO.asString("CODBEM");
		String tecla = newVO.asBigDecimal("TECLA").toString();
		BigDecimal produto = newVO.asBigDecimal("CODPROD");
		BigDecimal estoque = BigDecimalUtil.getValueOrZero(getEstoque(patrimonio, tecla, produto));
		
		if(validaSeOhPatrimonioEstaNaTelaDeInstalacoes(patrimonio)) {
			String microMarketing = validaSeEhMicroMarketing(patrimonio);
			
			if("S".equals(microMarketing)) {
				tecla = "0";
			}
			
			try {

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

						VO.setProperty("TECLA", tecla);					 
						VO.setProperty("CODPROD", newVO.asBigDecimal("CODPROD"));
						VO.setProperty("NIVELPAR", newVO.asBigDecimal("AD_NIVELPAR"));
						VO.setProperty("CAPACIDADE", newVO.asBigDecimal("AD_CAPACIDADE"));
						VO.setProperty("NIVELALERTA", newVO.asBigDecimal("AD_NIVELALERTA"));
						VO.setProperty("VLRPAR", newVO.asBigDecimal("VLRPAR"));
						VO.setProperty("VLRFUN", newVO.asBigDecimal("VLRFUN"));
						VO.setProperty("ESTOQUE", estoque);

						itemEntity.setValueObject(NVO);
					}	
				}

			} catch (Exception e) {
				salvarException("[salvaDadosNaTelainstalacaoAbaPlanograma] Nao foi possivel atualizar a tecla! patrimonio: "+patrimonio+" tecla: "+tecla+e.getMessage()+"\n"+e.getCause());
			}
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
	
	private BigDecimal getEstoque(String patrimonio, String tecla, BigDecimal produto) throws Exception {
		
		BigDecimal estoque = null;
		JapeWrapper DAO = JapeFactory.dao("AD_ESTOQUE");
		DynamicVO VO = DAO.findOne("this.CODBEM=? AND this.TECLA=? AND this.CODPROD=?",new Object[] { patrimonio, tecla, produto });
		if(VO!=null) {
			estoque = VO.asBigDecimal("ESTOQUE");
		}else {
			estoque = new BigDecimal(0);
		}
		
		return estoque;
	}
	
	private boolean validaSeOhPatrimonioEstaNaTelaDeInstalacoes(String patrimonio) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		if(VO!=null) {
			valida=true;
		}
		 return valida;
	}
	
	private void insereNaTelaLogDasTeclas(DynamicVO oldVO,DynamicVO newVO) {
		try {
			
			BigDecimal usuario = getUsuLogado();
			if(usuario==null) {
				usuario = new BigDecimal(0);
			}
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOGTECLAS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CAPACIDADE", newVO.asBigDecimal("AD_CAPACIDADE"));
			VO.setProperty("CAPACIDADEANT", oldVO.asBigDecimal("AD_CAPACIDADE"));
			VO.setProperty("CODBEM", newVO.asString("CODBEM"));
			VO.setProperty("CODPROD", newVO.asBigDecimal("CODPROD"));
			VO.setProperty("CODPRODANT", oldVO.asBigDecimal("CODPROD"));
			VO.setProperty("CODUSU", usuario);
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
		try {
		    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
		} catch (Exception e) {
			// TODO: handle exception
		}	
	    return codUsuLogado;    	
}
	
	//after insert
	private void inserirTecla(PersistenceEvent arg0) throws Exception {
		DynamicVO teclas = (DynamicVO) arg0.getVo();
		String patrimonio = teclas.asString("CODBEM");

		if(validaSeOhPatrimonioEstaNaTelaDeInstalacoes(patrimonio)) {
			String micromarketing = validaSeEhMicroMarketing(patrimonio);
			
			try {
				
				BigDecimal estoque = BigDecimalUtil.getValueOrZero(getEstoque(patrimonio, teclas.asBigDecimal("TECLA").toString(), teclas.asBigDecimal("CODPROD")));
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCPlanograma");
				DynamicVO VO = (DynamicVO) NPVO;
				
				VO.setProperty("CODBEM", patrimonio);
				VO.setProperty("CODPROD", teclas.asBigDecimal("CODPROD"));
				if("S".equals(micromarketing)) {
					VO.setProperty("TECLA", new BigDecimal(0).toString());
				}else {
					VO.setProperty("TECLA", teclas.asBigDecimal("TECLA").toString());
				}
				VO.setProperty("VLRPAR", teclas.asBigDecimal("VLRPAR"));
				VO.setProperty("VLRFUN", teclas.asBigDecimal("VLRFUN"));
				VO.setProperty("AD_ABASTECER", "S");
				VO.setProperty("ESTOQUE", estoque);
				
				if(teclas.asBigDecimal("AD_NIVELPAR")!=null) {
					VO.setProperty("NIVELPAR", teclas.asBigDecimal("AD_NIVELPAR"));
				}
				
				if(teclas.asBigDecimal("AD_CAPACIDADE")!=null) {
					VO.setProperty("CAPACIDADE", teclas.asBigDecimal("AD_CAPACIDADE"));
				}
				
				if(teclas.asBigDecimal("AD_NIVELALERTA")!=null) {
					VO.setProperty("NIVELALERTA", teclas.asBigDecimal("AD_NIVELALERTA"));
				}
				
				dwfFacade.createEntity("GCPlanograma", (EntityVO) VO);
				
			} catch (Exception e) {
				salvarException("[inserirTecla] Nao foi possivel cadastrar a tecla! patrimonio: "+patrimonio+" tecla: "+teclas.asBigDecimal("TECLA")+e.getMessage()+"\n"+e.getCause());
			}
		}
	}
	
	//insert
	private void BeforeInsert(PersistenceEvent arg0) {
		DynamicVO teclas = (DynamicVO) arg0.getVo();
		String patrimonio = teclas.asString("CODBEM");
		try {
			
			BigDecimal usuario = getUsuLogado();
			if(usuario==null) {
				usuario = new BigDecimal(0);
			}
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOGTECLAS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("DTALTER", TimeUtils.getNow());
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("TECLA", teclas.asBigDecimal("TECLA").toString());
			VO.setProperty("CODPROD", teclas.asBigDecimal("CODPROD"));
			VO.setProperty("CAPACIDADE", teclas.asBigDecimal("AD_CAPACIDADE"));
			VO.setProperty("NIVELPAR", teclas.asBigDecimal("AD_NIVELPAR"));
			VO.setProperty("NIVELALERTA", teclas.asBigDecimal("AD_NIVELALERTA"));
			VO.setProperty("VLRPAR", teclas.asBigDecimal("VLRPAR"));
			VO.setProperty("VLRFUN", teclas.asBigDecimal("VLRFUN"));
			VO.setProperty("STATUS", "Novo Cadastro");
			
			dwfFacade.createEntity("AD_LOGTECLAS", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[BeforeInsert] nao foi possivel registrar o Insert da tecla no AD_LOGTECLAS, patrimonio: "+patrimonio+" tecla: "+teclas.asBigDecimal("TECLA")+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}

	//delete
	private void deletarTecla(PersistenceEvent arg0) throws Exception {
		DynamicVO teclas = (DynamicVO) arg0.getVo();
		String patrimonio = teclas.asString("CODBEM");
		if(validaSeOhPatrimonioEstaNaTelaDeInstalacoes(patrimonio)) {
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
				salvarException("[deletarTecla] Nao foi possivel excluir a tecla! patrimonio: "+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
			}
		}
		
		try {
			
			BigDecimal usuario = getUsuLogado();
			if(usuario==null) {
				usuario = new BigDecimal(0);
			}
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOGTECLAS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("DTALTER", TimeUtils.getNow());
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("CODBEM", patrimonio);
			VO.setProperty("TECLA", teclas.asBigDecimal("TECLA").toString());
			VO.setProperty("CODPROD", teclas.asBigDecimal("CODPROD"));
			VO.setProperty("CAPACIDADE", teclas.asBigDecimal("AD_CAPACIDADE"));
			VO.setProperty("NIVELPAR", teclas.asBigDecimal("AD_NIVELPAR"));
			VO.setProperty("NIVELALERTA", teclas.asBigDecimal("AD_NIVELALERTA"));
			VO.setProperty("VLRPAR", teclas.asBigDecimal("VLRPAR"));
			VO.setProperty("VLRFUN", teclas.asBigDecimal("VLRFUN"));
			VO.setProperty("STATUS", "Excluido");
			
			dwfFacade.createEntity("AD_LOGTECLAS", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[deletarTecla] nao foi possivel registrar o delete da tecla no AD_LOGTECLAS, patrimonio: "+patrimonio+" tecla: "+teclas.asBigDecimal("TECLA")+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private boolean verificaSeExisteNaAbaInstalacoes(String codbem, BigDecimal tecla, BigDecimal codprod) throws Exception{
		boolean existe = false;
		JapeWrapper DAO = JapeFactory.dao("GCPlanograma");
		DynamicVO VO = DAO.findOne("CODBEM=? AND TECLA=? AND CODPROD=?", new Object[] { codbem, tecla, codprod });
		if (VO != null) {
			return existe = true;
		}
		return existe;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_registraAlteracaoTecla");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
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
