package br.com.OrdemServico;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import com.sankhya.util.StringUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_valida_os_recorrente implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	// 1.0
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();

		BigDecimal numos = VO.asBigDecimal("NUMOS");
		BigDecimal motivo = VO.asBigDecimal("CODOCOROS");
		String patrimonio = VO.asString("SERIE");
		Timestamp dtAbertura = VO.asTimestamp("DHENTRADA");
		BigDecimal corsla = VO.asBigDecimal("CORSLA");
		
		if(corsla==null) {
			corsla = new BigDecimal(0);
		}
		
		if(patrimonio!=null) {
			if(motivo.intValue()==4) {
				if(verificaSeExisteOsAbertasNosUltimosQuinzeDias(patrimonio,dtAbertura,numos)) {
					if(corsla.intValue()!=16776960) {
						VO.setProperty("CORSLA", new BigDecimal(10027161));
					}
				}
			}
		}
	}

	// 1.1
	private boolean verificaSeExisteOsAbertasNosUltimosQuinzeDias(String patrimonio, Timestamp dtAbertura,
			BigDecimal numos) throws Exception {
		
		boolean valida = false;
		String formatTimestamp = StringUtils.formatTimestamp(dtAbertura, "dd/MM/YYYY");
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM TCSITE WHERE SERIE='"+patrimonio+"' AND DHENTRADA >=TO_DATE('"+formatTimestamp+"')-15 AND DHENTRADA<=TO_DATE('"+formatTimestamp+"') AND HRFINAL IS NULL AND NUMOS NOT IN("+numos+")");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			int count = contagem.getInt("QTD");
			
			if (count > 1) {
				valida = true;
			}
		}
		return valida;
	}

}
