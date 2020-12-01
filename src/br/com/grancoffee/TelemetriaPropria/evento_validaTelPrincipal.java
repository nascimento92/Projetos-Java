package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_validaTelPrincipal implements EventoProgramavelJava {

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
		insert(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		update(arg0);
	}

	private void insert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String patrimonio = VO.asString("CODBEM");
		
		validacoes(arg0);
		
		if("S".equals(VO.asString("PRINCIPAL"))) {

			int qtd = verificaSeJaExisteUmaTelemetriaPrincipal(patrimonio);
			
			if(qtd>0) {
				throw new PersistenceException("<br/><br/><br/><b>Erro - Já existe uma telemetria como principal!</b><br/><br/><br/>");
			}
		}	
	}
	
	private void update(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();	
		String patrimonio = VO.asString("CODBEM");
		
		validacoes(arg0);
		
		if("S".equals(VO.asString("PRINCIPAL")) && "N".equals(oldVO.asString("PRINCIPAL"))) {

			int qtd = verificaSeJaExisteUmaTelemetriaPrincipal(patrimonio);
			
			if(qtd>0) {
				throw new PersistenceException("<br/><br/><br/><b>Erro - Já existe uma telemetria como principal!</b><br/><br/><br/>");
			}
		}
	}
	
	private void validacoes(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		//String patrimonio = VO.asString("CODBEM");
		BigDecimal idtel = VO.asBigDecimal("IDTEL");
		String pinpadDigital = VO.asString("AD_PINPADDIG");
		BigDecimal boxVerti = VO.asBigDecimal("AD_BOXVERTI");
		
		if(idtel.intValue()==1 && pinpadDigital!=null) {
			throw new Error("<br/><br/><br/><b> Pinpad Digital não pode ser vinculado a Verti! <br/><br/><br/><b>");
		}
		
		if(idtel.intValue()==2 && boxVerti!=null) {
			throw new Error("<br/><br/><br/><b> Box não pode ser vinculada a Uppay! <br/><br/><br/><b>");
		}
	}

	private int verificaSeJaExisteUmaTelemetriaPrincipal(String patrimonio) throws Exception {
		int qtd = 0;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM GC_TELINST WHERE CODBEM='"+patrimonio+"' AND PRINCIPAL='S'");
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			qtd = contagem.getInt("QTD");
		}
		
		return qtd;
	}
}
