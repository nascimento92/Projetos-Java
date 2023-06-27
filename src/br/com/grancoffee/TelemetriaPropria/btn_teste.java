package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_teste implements AcaoRotinaJava{
	
	int qtdTeclas = 99;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		getListaMaquinas();
	}
	
	
	private void getListaMaquinas() {
		String patrimonio="";
	
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
				if(dtUltimaContagem!=null) {
					atualizaDados(patrimonio, dtUltimaContagem);
				}
			}
		} catch (Exception e) {
			salvarException("[getListaPendente] Nao foi possivel obter a lista: "+patrimonio
					+ e.getMessage() + "\n" + e.getCause());
		}
	}
	
	private void atualizaDados(String codbem, Timestamp dtUltimaContagem) {
		try {
			JapeWrapper produtoDAO = JapeFactory.dao("GCInstalacao"); 
			DynamicVO produtoVO = produtoDAO.findOne("CODBEM=?",new Object[] { codbem });
			produtoDAO.prepareToUpdate(produtoVO).set("AD_DTULTCONTAGEM", dtUltimaContagem).update();
		} catch (Exception e) {
			salvarException("[atualizaDados] Nao foi possivel atualizar a hora para o pt: "+codbem
					+ e.getMessage() + "\n" + e.getCause());
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
			nativeSql.appendSql(
					"SELECT MAX(DTABAST) AS ULTCONTAGEM FROM AD_RETABAST WHERE CODBEM='"+codbem+"' AND CONTAGEM='S'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				dtUltContagem = contagem.getTimestamp("ULTCONTAGEM");
			}
		} catch (Exception e) {
			salvarException("[getDtUltimaContagem] Nao foi possivel obter a data da ultima contagem do pt "+codbem
					+ e.getMessage() + "\n" + e.getCause());
		}
		
		return dtUltContagem;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "acao_ajustaHoraInventario");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
