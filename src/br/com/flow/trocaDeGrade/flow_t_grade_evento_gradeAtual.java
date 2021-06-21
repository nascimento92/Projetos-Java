package br.com.flow.trocaDeGrade;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

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
		validaSeJaTemUmPatrimonio(arg0);
		alteraContratoParceiro(arg0);
		
		 
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		deletar(arg0);
		alteraContratoParceiro(arg0);
		start(arg0);		
	}
	
	private void alteraContratoParceiro(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String patrimonio = VO.asString("CODBEM");
		
		if (patrimonio != null) {
			VO.setProperty("NUMCONTRATO", getNumcontrato(patrimonio));
			VO.setProperty("CODPARC", getParceiro(getNumcontrato(patrimonio)));
		}
	}
	
	private void validaSeJaTemUmPatrimonio(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idFlow = VO.asBigDecimal("IDINSTPRN");
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(CODBEM) AS QTD FROM AD_MAQUINASTGRADE WHERE IDINSTPRN=" + idFlow);
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			int count = contagem.getInt("QTD");
			if (count >= 1) {
				throw new Error("Já existe um patrimônio cadastrado neste flow");
			}
		}
	}
	
	public void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idFlow = VO.asBigDecimal("IDINSTPRN");
		BigDecimal idTarefa = VO.asBigDecimal("IDINSTTAR");
		BigDecimal codRegistro = VO.asBigDecimal("CODREGISTRO");
		String idCard = VO.asString("IDTAREFA");
		String patrimonio = VO.asString("CODBEM");
		
		try {
			getTeclas(idFlow,idTarefa,codRegistro,idCard,patrimonio);
		} catch (Exception e) {
			salvarException("[start] não foi possivel realizar os procedimentos! "+e.getMessage()+"\n"+e.getCause());
		}
		
	}
	
	public void deletar(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal idFlow = VO.asBigDecimal("IDINSTPRN");
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GRADEATUAL", "this.IDINSTPRN=?",new Object[] {idFlow}));
		} catch (Exception e) {
			salvarException("[deletar] Não foi possível deletar AD_GRADEATUAL IdFlow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_GRADEFUTURA", "this.IDINSTPRN=?",new Object[] {idFlow}));
		} catch (Exception e) {
			salvarException("[deletar] Não foi possível deletar AD_GRADEFUTURA IdFlow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void getTeclas(BigDecimal idFlow,BigDecimal idTarefa,BigDecimal codRegistro,String idCard,String patrimonio) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("GCPlanograma","this.CODBEM = ? ", new Object[] { patrimonio }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				
			insereTeclaAtual(idFlow,idTarefa,codRegistro,idCard,patrimonio,DynamicVO);
			insereTeclaFutura(idFlow,idTarefa,codRegistro,idCard,patrimonio,DynamicVO);
			deletarAlteracoesIniciais(idFlow);
			}

		} catch (Exception e) {
			salvarException("[getTeclas] Não foi possível obter as teclas IdFlow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void insereTeclaAtual(BigDecimal idFlow,BigDecimal idTarefa,BigDecimal codRegistro,String idCard,String patrimonio, 
			DynamicVO DynamicVO) {
		
		BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
		String tecla = DynamicVO.asString("TECLA");
		BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
		BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
		BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");
		BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
		BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
		
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
			VO.setProperty("VLRFUN", vlrfun);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("VLRPARC", vlrpar);
			
			dwfFacade.createEntity("AD_GRADEATUAL", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[getTeclas] Não foi possível insereTeclaAtual as teclas IdFlow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	public void insereTeclaFutura(BigDecimal idFlow,BigDecimal idTarefa,BigDecimal codRegistro,String idCard,String patrimonio, 
			DynamicVO DynamicVO) {
		
		BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
		String tecla = DynamicVO.asString("TECLA");
		BigDecimal nivelpar = DynamicVO.asBigDecimal("NIVELPAR");
		BigDecimal capacidade = DynamicVO.asBigDecimal("CAPACIDADE");
		BigDecimal nivelalerta = DynamicVO.asBigDecimal("NIVELALERTA");
		BigDecimal vlrpar = DynamicVO.asBigDecimal("VLRPAR");
		BigDecimal vlrfun = DynamicVO.asBigDecimal("VLRFUN");
		
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
			VO.setProperty("VLRFUN", vlrfun);
			VO.setProperty("NIVELPAR", nivelpar);
			VO.setProperty("CAPACIDADE", capacidade);
			VO.setProperty("NIVELALERTA", nivelalerta);
			VO.setProperty("VLRPARC", vlrpar);
			
			dwfFacade.createEntity("AD_GRADEFUTURA", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[getTeclas] Não foi possível insereTeclaAtual as teclas IdFlow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void deletarAlteracoesIniciais(BigDecimal idFlow) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("AD_PRODUTOSALTERADOS", "this.IDINSTPRN=?",new Object[] {idFlow}));
		} catch (Exception e) {
			salvarException("[deletarAlteracoesIniciais] Não foi excluir as teclas iniciais IdFlow: "+idFlow+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "flow_t_grade_evento_gradeAtual");
			VO.setProperty("PACOTE", "br.com.flow.trocaDeGrade");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
	
	private BigDecimal getNumcontrato(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		return contrato;
	}
	
	private BigDecimal getParceiro(BigDecimal numcontrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { numcontrato });
		BigDecimal parceiro = VO.asBigDecimal("CODPARC");
		return parceiro;
	}

}
