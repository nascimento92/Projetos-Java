package br.com.integracaoTecnicos;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class evento_calculaPrazo implements EventoProgramavelJava {

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
		Timestamp dtinicial = VO.asTimestamp("DTINT");
		BigDecimal dias = VO.asBigDecimal("PRAZO");
		
		if(dias == null) {
			dias = (BigDecimal) MGECoreParameter.getParameter("PRAZOINTEGRACAO");
			VO.setProperty("PRAZO", dias);
		}
		
		  
		if(dtinicial!=null) {
			Timestamp newData = TimeUtils.dataAddDay(dtinicial, dias.intValue());
			VO.setProperty("DTVAL", newData);
		}
		
	}

}
