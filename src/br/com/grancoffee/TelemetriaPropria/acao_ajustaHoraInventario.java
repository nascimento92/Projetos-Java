package br.com.grancoffee.TelemetriaPropria;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import com.sankhya.util.JdbcUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SPBeanUtils;
import br.com.sankhya.ws.ServiceContext;

public class acao_ajustaHoraInventario implements ScheduledAction {

	/**
	 * Ação criada para atualizar a data do último inventário.
	 * 
	 * 19-06-2023 - vs 1.0 - Gabriel Nascimento - Criação do objeto.
	 * 30-01-2024 - vs 1.1 - Gabriel Nascimento - Retirada do save na exception.
	 */
	@Override
	public void onTime(ScheduledActionContext arg0) {

		ServiceContext sctx = new ServiceContext(null);
		sctx.setAutentication(AuthenticationInfo.getCurrent());
		sctx.makeCurrent();

		try {
			SPBeanUtils.setupContext(sctx);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("[onTime] não foi possível setar o usuário! " + e.getMessage() + "\n" + e.getCause());
		}

		JapeSession.SessionHandle hnd = null;

		try {

			hnd = JapeSession.open();
			hnd.execWithTX(new JapeSession.TXBlock() {

				public void doWithTx() throws Exception {

					getListaMaquinas();
				}

			});

		} catch (Exception e) {
			System.out.println("[onTime] não foi possível iniciar a sessão! " + e.getMessage() + "\n" + e.getCause());
		} finally {
			JapeSession.close(hnd);
		}

	}

	private void getListaMaquinas() {
		String patrimonio = "";

		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT CODBEM FROM GC_INSTALACAO I WHERE AD_DTULTCONTAGEM IS NOT NULL AND AD_FREQCONTAGEM IS NOT NULL AND TRUNC(AD_DTULTCONTAGEM) <> TRUNC((SELECT MAX(DTABAST) FROM AD_RETABAST WHERE CODBEM=I.CODBEM AND CONTAGEM='S'))");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				patrimonio = contagem.getString("CODBEM");
				Timestamp dtUltimaContagem = getDtUltimaContagem(patrimonio);
				if (dtUltimaContagem != null) {
					atualizaDados(patrimonio, dtUltimaContagem);
				}
			}
			
			JdbcUtils.closeResultSet(contagem);
			NativeSql.releaseResources(nativeSql);
			
		} catch (Exception e) {
			System.out.println("[getListaPendente] Nao foi possivel obter a lista: " + patrimonio + e.getMessage() + "\n"
					+ e.getCause());
		}

	}

	private void atualizaDados(String codbem, Timestamp dtUltimaContagem) {
		try {
			JapeWrapper produtoDAO = JapeFactory.dao("GCInstalacao");
			DynamicVO produtoVO = produtoDAO.findOne("CODBEM=?", new Object[] { codbem });
			produtoDAO.prepareToUpdate(produtoVO).set("AD_DTULTCONTAGEM", dtUltimaContagem).update();
		} catch (Exception e) {
			System.out.println("[atualizaDados] Nao foi possivel atualizar a hora para o pt: " + codbem + e.getMessage()
					+ "\n" + e.getCause());
		}
	}

	private Timestamp getDtUltimaContagem(String codbem) {

		Timestamp dtUltContagem = null;

		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT MAX(DTABAST) AS ULTCONTAGEM FROM AD_RETABAST WHERE CODBEM='" + codbem
					+ "' AND CONTAGEM='S'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				dtUltContagem = contagem.getTimestamp("ULTCONTAGEM");
			}
			
			JdbcUtils.closeResultSet(contagem);
			NativeSql.releaseResources(nativeSql);
		} catch (Exception e) {
			System.out.println("[getDtUltimaContagem] Nao foi possivel obter a data da ultima contagem do pt " + codbem
					+ e.getMessage() + "\n" + e.getCause());
		}

		return dtUltContagem;
	}

	/*
	 * private void salvarException(String mensagem) { try {
	 * 
	 * EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade(); EntityVO NPVO =
	 * dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS"); DynamicVO VO =
	 * (DynamicVO) NPVO;
	 * 
	 * VO.setProperty("OBJETO", "acao_ajustaHoraInventario");
	 * VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
	 * VO.setProperty("DTEXCEPTION", TimeUtils.getNow()); VO.setProperty("CODUSU",
	 * new BigDecimal(0)); VO.setProperty("ERRO", mensagem);
	 * 
	 * dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
	 * 
	 * } catch (Exception e) { // aqui não tem jeito rs tem que mostrar no log
	 * System.out.
	 * println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " +
	 * e.getMessage()); } }
	 */

}
