package br.com.gsn.Projetos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_validacoesAtendentes implements EventoProgramavelJava{

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
		BigDecimal idprojeto = VO.asBigDecimal("ID");
		BigDecimal idetapa = VO.asBigDecimal("IDETAPA");
		String tipo = VO.asString("TIPO");
		DynamicVO etapa = getEtapa(idprojeto,idetapa);
		String tipoCalculoAtividade = etapa.asString("CALCULACAPACIDADE");
		
		if(!"4".equals(tipoCalculoAtividade)) {
			
			Timestamp dtini = etapa.asTimestamp("DTPREVINI");
			Timestamp dtfim = etapa.asTimestamp("DTPREVFIM");
			
			int mesInicial = TimeUtils.getMonth(dtini);
			int mesFinal = TimeUtils.getMonth(dtfim);
			
			if(mesInicial==mesFinal) {
				int mesatual = TimeUtils.getMonth(TimeUtils.getNow());
				if(mesatual==mesInicial) {
					BigDecimal usuario = VO.asBigDecimal("CODUSU");
					BigDecimal hrsEtapa = etapa.asBigDecimal("HRSPREV");
					BigDecimal capacidade = getCapacidade(usuario);
					BigDecimal horasJaPrevistas = getHorasJaPrevistas(usuario);
					
					BigDecimal total = horasJaPrevistas.add(hrsEtapa);
					BigDecimal disponiveis = capacidade.subtract(horasJaPrevistas).setScale(2, RoundingMode.HALF_EVEN);
					
					if(total.doubleValue() > capacidade.doubleValue()) {
						throw new Error("<b>ATENÇÃO</b><br/><br/> A quantidade de <b>"+hrsEtapa+"</b> horas previstas da etapa estão a cima do limite do Atendente, horas disponíveis: <b>"+disponiveis+"</b> !.<br/><br/>");
					}
				}
			}
			
			
			
		}
		
		if("1".equals(tipo)) {
			if(validaPrincipal(idprojeto, idetapa)) {
				VO.setProperty("TIPO", "2");
			}
		}	
	}
	
	private DynamicVO getEtapa(BigDecimal idprojeto, BigDecimal idetapa) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_REQETAPAS");
		DynamicVO VO = DAO.findOne("ID=? AND IDETAPA=?",new Object[] { idprojeto,idetapa });
		return VO;
	}
	
	private boolean validaPrincipal(BigDecimal idprojeto, BigDecimal idetapa) {
		boolean valida = false;
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT COUNT(*) AS QTD FROM AD_ETAPASATENDENTES WHERE TIPO='1' AND ID="+idprojeto+" AND IDETAPA="+idetapa);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count >= 1) {
					valida = true;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return valida;
	}
	
	private BigDecimal getCapacidade(BigDecimal usuario) {
		BigDecimal cap = BigDecimal.ZERO;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
			"SELECT (SELECT DIASUTEIS FROM AD_MESES WHERE NROMES=TO_CHAR(SYSDATE,'MM')) * NVL((SELECT HORASDEVDIA FROM AD_EQUIPETI WHERE CODUSU="+usuario+"),1) AS CAPACIDADE FROM DUAL");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				double count = contagem.getDouble("CAPACIDADE");
				if (count >= 1) {
					cap = new BigDecimal(count);
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return cap;
	}
	
	private BigDecimal getHorasJaPrevistas(BigDecimal usuario) {
		BigDecimal cap = BigDecimal.ZERO;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
			"SELECT "+
			"CASE "+
			"WHEN TO_CHAR(SYSDATE,'MM')='01' THEN (SELECT SUM(JANEIRO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='02' THEN (SELECT SUM(FEVEREIRO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='03' THEN (SELECT SUM(MARCO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='04' THEN (SELECT SUM(ABRIL) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='05' THEN (SELECT SUM(MAIO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='06' THEN (SELECT SUM(JUNHO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='07' THEN (SELECT SUM(JULHO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='08' THEN (SELECT SUM(AGOSTO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='09' THEN (SELECT SUM(SETEMBRO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='10' THEN (SELECT SUM(OUTUBRO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='11' THEN (SELECT SUM(NOVEMBRO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"WHEN TO_CHAR(SYSDATE,'MM')='12' THEN (SELECT SUM(DEZEMBRO) FROM GC_PRJ_ATENDENTE WHERE CODUSU="+usuario+") "+
			"END AS PREVISTAS FROM DUAL");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				double count = contagem.getDouble("PREVISTAS");
				if (count >= 1) {
					cap = new BigDecimal(count);
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return cap;
	}
	
}
