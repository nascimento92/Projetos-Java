package br.com.flow.prod.RelatorioInstalacao;

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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_rel_inst_evento_VerificaMaquinas implements EventoProgramavelJava {
	
	/**
	 * Impede que a mesma m�quinas seja utilizada em dois fluxos diferentes no flow, simultaneamente.
	 */
	
	String tarefa = "UserTask_0qmvbmu";
	String tarefa2 = "UserTask_0yj8z1s";
	String tarefa3 = "UserTask_102vtp7";
	
	String tarefaTeste = "UserTask_1r6c4w6";
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		String idTarefa = VO.asString("IDTAREFA");
		String patrimonio = VO.asString("CODBEM");
		
		System.out.println("=======> TAREFA: "+idTarefa);
		
		if(tarefa.equals(idTarefa) || tarefa2.equals(idTarefa) || tarefa3.equals(idTarefa) || tarefaTeste.equals(idTarefa)) {
			BigDecimal idProcesso = verificaOutrosProcessos(idTarefa,patrimonio);
			
			if(idProcesso!=null) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O patrim�nio "+patrimonio+" j� est� sendo utilizado no processo flow "+idProcesso+" n�o poder� ser utilizado!!</b></font><br/><br/><br/>\n\n\n");
			}
			
			DynamicVO tcibem = getTCIBEM(patrimonio);
			BigDecimal contrato = tcibem.asBigDecimal("NUMCONTRATO");
			
			System.out.println("=======> PATRIMONIO: "+patrimonio+"\nCONTRATO: "+contrato);
			
			if(contrato.intValue()>0) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O patrim�nio "+patrimonio+" est� vinculado ao contrato "+contrato+" n�o poder� ser utilizado!!</b></font><br/><br/><br/>\n\n\n");
			}
		}	
		
	}
	
	private BigDecimal verificaOutrosProcessos(String idTarefa,String patrimonio) throws Exception {
		
		BigDecimal count = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT IDINSTPRN FROM TWFITAR WHERE IDINSTPRN IN (SELECT IDINSTPRN FROM AD_MAQUINASFLOW WHERE IDTAREFA='"+idTarefa+"' AND CODBEM='"+patrimonio+"') AND DHCONCLUSAO IS NULL");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getBigDecimal("IDINSTPRN");
		}
		
		return count;
	}
	
	private DynamicVO getTCIBEM(String codbem) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		return VO;
	}
}
