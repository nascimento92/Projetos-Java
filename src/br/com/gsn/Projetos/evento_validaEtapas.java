package br.com.gsn.Projetos;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

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
		insertValidation(arg0);
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		startValidations(arg0);		
	}
	
	private void startValidations(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		BigDecimal sprint = VO.asBigDecimal("SPRINT");
		DynamicVO adSprint = getSprint(sprint);
		
		Timestamp dtfinal = adSprint.asTimestamp("DTFINAL");
		
		/*
		 * if(dtfinal.before(TimeUtils.getNow())) { throw new
		 * Error("Não é possível atribuir tarefas a uma Sprint já finalizada !"); }
		 */
		
		String oldStatus = oldVO.asString("STATUS");
		String newStatus = VO.asString("STATUS");
		
		Timestamp newDtFim = VO.asTimestamp("DTFIM");
		
		if(newStatus!=oldStatus && newStatus.equals("2")) {
			VO.setProperty("DTFIM", TimeUtils.getNow());
		}
		
		if(newDtFim!=null) {
			VO.setProperty("STATUS", new String("2"));
		}
	}
	
	private void insertValidation(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal sprint = VO.asBigDecimal("SPRINT");
		DynamicVO adSprint = getSprint(sprint);
		Timestamp newDtFim = VO.asTimestamp("DTFIM");
		String newStatus = VO.asString("STATUS");
		
		Timestamp dtfinal = adSprint.asTimestamp("DTFINAL");
		/*
		 * if(dtfinal.before(TimeUtils.getNow())) { throw new
		 * Error("Não é possível atribuir tarefas a uma Sprint já finalizada !"); }
		 */
		
		if(newDtFim!=null) {
			VO.setProperty("STATUS", new String("2"));
		}
		
		if(newStatus.equals("2")) {
			VO.setProperty("DTFIM", TimeUtils.getNow());
		}
	}
	
	private DynamicVO getSprint(BigDecimal sprint) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_SPRINTS");
		DynamicVO VO = DAO.findOne("IDSPRINT=?",new Object[] { sprint });
		return VO;		
	}

}
