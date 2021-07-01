package br.com.app.liberacao;

import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.ConfirmacaoNotaHelper;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_liberacaoLimite implements EventoProgramavelJava {

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
		BigDecimal nunota = VO.asBigDecimal("NUNOTA");
		String liberado = VO.asString("LIBERADO");
		String usuario = VO.asString("NOMEUSU");
		
		if(validaSeConfirmaNota(usuario)) {
			if("N".equals(liberado)) {
				confirmar(nunota);
				VO.setProperty("LIBERADO", "S");
			}
		}
		
	}
	
	private boolean validaSeConfirmaNota(String usuario) throws Exception {
		boolean valida = false;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT TENTACONFIRMAR FROM AD_APPLIBCONFIG WHERE NOMEUSU='"+usuario+"'");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			String confirma = contagem.getString("TENTACONFIRMAR");
			if ("S".equals(confirma)) {
				valida = true;
			}
		}
		
		return valida;
	}

	public void confirmar(BigDecimal notaConfirmando) throws MGEModelException {
		
		try {
			ServiceContext serviceCtx = ServiceContext.getCurrent();
			JapeSession.putProperty("CabecalhoNota.confirmacao.ehPedido.Web", Boolean.FALSE);
			AuthenticationInfo auth = (AuthenticationInfo) serviceCtx.getAutentication();
			BarramentoRegra bRegras = BarramentoRegra.build(CACHelper.class, "regrasAprovarCAC.xml", auth);

			CACHelper.setupContext(serviceCtx);
			ConfirmacaoNotaHelper.confirmarNota(notaConfirmando, bRegras);

		} catch (Exception e) {
			salvarException("[confirmar] Nao foi possivel confirmar a nota "+notaConfirmando+"\n"+e.getMessage()+e.getMessage());
		}
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_liberacaoLimite");
			VO.setProperty("PACOTE", "br.com.app.liberacao");
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
