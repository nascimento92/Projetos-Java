package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_ajustaTeclaVisita implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		
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
		DynamicVO VO = (DynamicVO) arg0.getVo();	
		VO.setProperty("DTALTER", TimeUtils.getNow());
	}

	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String tecla = VO.asString("TECLA");
		BigDecimal os = VO.asBigDecimal("NUMOS");
		BigDecimal produto = VO.asBigDecimal("CODPROD");

		if (tecla == null) {
			
			String obtemTecla = obtemTecla(os, produto);
			
			if(obtemTecla==null) {
				obtemTecla = "0";
			}
			
			VO.setProperty("TECLA", obtemTecla);
		}
		
		VO.setProperty("DTREGISTRO", TimeUtils.getNow());
	}

	private String obtemTecla(BigDecimal os, BigDecimal produto) {
		
		String tecla = null;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT T.TECLA FROM GC_PLANOGRAMA T WHERE T.CODBEM = (SELECT CODBEM FROM GC_SOLICITABAST WHERE NUMOS="+os+") AND T.CODPROD = "+produto+" AND ROWNUM=1");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				tecla = contagem.getString("TECLA");
			}
		} catch (Exception e) {
			salvarException("[obtemTecla] Nao foi possivel obter a tecla! OS: "+os+" produto: "+produto+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return tecla;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_ajustaTeclaVisita");
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
