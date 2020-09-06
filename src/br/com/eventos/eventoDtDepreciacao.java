package br.com.eventos;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class eventoDtDepreciacao implements EventoProgramavelJava {


	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}


	public void afterInsert(PersistenceEvent arg0) throws Exception {
		//DynamicVO bemVO = (DynamicVO) arg0.getVo();
		//bemVO.setProperty("AD_QTDMESESDEP", 120);
		

	}


	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		
	}


	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}


	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}


	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		getInformacoes(arg0);

	}


	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	private void getInformacoes(PersistenceEvent arg0) {

		DynamicVO bemVO = (DynamicVO) arg0.getVo();

		Date dateInicialDep = (Date) bemVO.getProperty("DTINICIODEP");

		bemVO.setProperty("DTINICIODEP", setDia(dateInicialDep));
		bemVO.setProperty("DTFIMDEP", addMeses(setDia(dateInicialDep), bemVO));

	}

	private Timestamp addMeses(Date dt, DynamicVO d) {
		
		//int meses = (int) d.getProperty("AD_QTDMESESDEP");
		//int me = meses.intValue();
		GregorianCalendar gcm = new GregorianCalendar();
		gcm.setTime(dt);
		gcm.add(Calendar.MONTH, 119);
		//gcm.add(Calendar.DATE, -1);
		dt = gcm.getTime();
		Timestamp Resultado = new Timestamp(dt.getTime());
		
		return Resultado;
		
	}

	private Timestamp setDia(Date data) {

		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(data);
		gc.set(GregorianCalendar.DAY_OF_MONTH, 1);
		data = gc.getTime();
		Timestamp Result = new Timestamp(data.getTime());

		return Result;
	}

}
