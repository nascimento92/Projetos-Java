package br.com.gsn.Projetos;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class evento_diasAusentes implements EventoProgramavelJava {

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
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		Timestamp dtinicial = VO.asTimestamp("DTINICIAL");
		Timestamp dtfinal = VO.asTimestamp("DTFINAL");
		
		if(dtfinal!=null) {
			int qtdDias = calculateDuration(dtinicial,dtfinal);
			VO.setProperty("QTDDIAS", new BigDecimal(qtdDias));
		}
	}
	
	public static int calculateDuration(Timestamp startDate, Timestamp endDate)
	{
	  Calendar startCal = Calendar.getInstance();
	  startCal.setTime(startDate);

	  Calendar endCal = Calendar.getInstance();
	  endCal.setTime(endDate);

	  int workDays = 0;

	  if (startCal.getTimeInMillis() > endCal.getTimeInMillis())
	  {
	    startCal.setTime(endDate);
	    endCal.setTime(startDate);
	  }

	  do
	  {
	    startCal.add(Calendar.DAY_OF_MONTH, 1);
	    if (startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && startCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY)
	    {
	      workDays++;
	    }
	  }
	  while (startCal.getTimeInMillis() <= endCal.getTimeInMillis());

	  return workDays;
	}

}
