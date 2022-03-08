package br.com.gsn.Projetos;

import java.sql.Timestamp;
import com.sankhya.util.TimeUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

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
	
	private void insert(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String status = VO.asString("STATUS");
		Timestamp dtfim = VO.asTimestamp("DTFIM");
		
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
	
	private void update(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
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

}
