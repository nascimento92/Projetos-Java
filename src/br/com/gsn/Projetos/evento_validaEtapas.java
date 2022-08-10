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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_validaEtapas implements EventoProgramavelJava {

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
		String status = VO.asString("STATUS");
		Timestamp dtfim = VO.asTimestamp("DTFIM");
	
		validacoes(VO,null, "insert");
		
		if("2".equals(status)) {
			VO.setProperty("DTFIM", TimeUtils.getNow());
		}
		
		if(dtfim!=null) {
			VO.setProperty("STATUS", "2");
		}
		
		if(status==null) {
			VO.setProperty("STATUS", "1");
		}
		
		
		
	}
	
	private void update(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		validacoes(VO,oldVO, "update");
		
		String status = VO.asString("STATUS");
		String oldstatus = oldVO.asString("STATUS");
		
		Timestamp dtfim = VO.asTimestamp("DTFIM");
		Timestamp olddtfim = oldVO.asTimestamp("DTFIM");
		
		if(status!=oldstatus) {
			if("2".equals(status)) {
				VO.setProperty("DTFIM", TimeUtils.getNow());
			}
			
			if("1".equals(status)) {
				VO.setProperty("DTFIM", null);
			}
		}
		
		if(dtfim!=olddtfim) {
			if(dtfim!=null) {
				VO.setProperty("STATUS", "2");
			}
		}
	}
	
	private void validacoes(DynamicVO VO, DynamicVO oldVO, String tipo) {
		
		//TODO :: Verifica se a data final é manor que a data inicial
		Timestamp dtprevfim = VO.asTimestamp("DTPREVFIM");
		Timestamp dtprevini = VO.asTimestamp("DTPREVINI");
		
		if(dtprevfim!=null) {
			if(dtprevfim.before(dtprevini)) {
				throw new Error("<b>ATENÇÃO</b><br/><br/> Data final, não pode ser menor que a data inicial.<br/><br/>");
			}
		}
		
		//TODO :: Verifica se o calculo na capacidade não se aplica, se não, as horas previstas são automaticamente alteradas para 0
		String tipoCalculoHoras = VO.asString("CALCULACAPACIDADE");
		if ("4".equals(tipoCalculoHoras)) { // para nenhum calculo
			VO.setProperty("HRSPREV", new BigDecimal(0));
		}
		
		//TODO :: Verifica se as horas previstas estouram a capacidade da equipe
		Timestamp dtini = VO.asTimestamp("DTPREVINI");
		Timestamp dtfim = VO.asTimestamp("DTPREVFIM");
		
		int mesInicial = TimeUtils.getMonth(dtini);
		int mesFinal = TimeUtils.getMonth(dtfim);
		
		if(mesInicial==mesFinal) {
			int mesatual = TimeUtils.getMonth(TimeUtils.getNow());
			if(mesatual==mesInicial) {
				BigDecimal hrsprevistas = VO.asBigDecimal("HRSPREV");
				BigDecimal capacidade = getCapacidade();
				BigDecimal totalHorasJaAlocadas = getTotalHorasJaAlocadas();
				BigDecimal soma = BigDecimal.ZERO;
				BigDecimal horasDisponiveis = capacidade.subtract(totalHorasJaAlocadas).setScale(2, RoundingMode.HALF_EVEN);
				
				if("insert".equals(tipo)) {
					soma = totalHorasJaAlocadas.add(hrsprevistas);
					if(soma.doubleValue()>capacidade.doubleValue()) {
						throw new Error("<b>ATENÇÃO</b><br/><br/> A quantidade de <b>"+hrsprevistas+"</b> horas previstas está superior a quantidade ainda disponível para o mês <b>"+horasDisponiveis+"</b>, uma opção é reduzir as horas previstas ou aumentar o prazo de atendimento !.<br/><br/>");
					}
				}
				
				if("update".equals(tipo)) {
					BigDecimal hrasJaAlocadas = oldVO.asBigDecimal("HRSPREV");
					soma = totalHorasJaAlocadas.add(hrsprevistas).subtract(hrasJaAlocadas);
					if(soma.doubleValue()>capacidade.doubleValue()) {
						throw new Error("<b>ATENÇÃO</b><br/><br/> A quantidade de <b>"+hrsprevistas+"</b> horas previstas está superior a quantidade ainda disponível para o mês ! essa atividade pode ter no máximo <b>"+horasDisponiveis.add(hrasJaAlocadas)+"</b>, uma opção é reduzir as horas previstas ou aumentar o prazo de atendimento !.<br/><br/>");
					}
				}
			}
		}
					
	}
	
	private BigDecimal getCapacidade() {
		BigDecimal cap = BigDecimal.ZERO;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT CAP FROM GC_CAPACIDADE_MES");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				double count = contagem.getDouble("CAP");
				if (count >= 1) {
					cap = new BigDecimal(count);
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return cap;
	}
	
	private BigDecimal getTotalHorasJaAlocadas() {
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
			"WHEN TO_CHAR(SYSDATE,'MM')='01' THEN (SELECT SUM(JANEIRO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='02' THEN (SELECT SUM(FEVEREIRO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='03' THEN (SELECT SUM(MARCO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='04' THEN (SELECT SUM(ABRIL) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='05' THEN (SELECT SUM(MAIO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='06' THEN (SELECT SUM(JUNHO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='07' THEN (SELECT SUM(JULHO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='08' THEN (SELECT SUM(AGOSTO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='09' THEN (SELECT SUM(SETEMBRO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='10' THEN (SELECT SUM(OUTUBRO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='11' THEN (SELECT SUM(NOVEMBRO) FROM GC_PRJ_MESES) "+
			"WHEN TO_CHAR(SYSDATE,'MM')='12' THEN (SELECT SUM(DEZEMBRO) FROM GC_PRJ_MESES) "+
			"END AS HORAS FROM DUAL"
			);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				double count = contagem.getDouble("HORAS");
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
