package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class evento_novoEncerramentoOs implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
				
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
			
		String newPendente = newVO.asString("PENDENTE");
		
		
		if("S".equals(newPendente)) {
			
			Timer timer = new Timer(10000, new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent e) {
					chamaPentaho();				
				}
			});
			timer.setRepeats(false);
			timer.start();
			
			newVO.setProperty("PENDENTE", "N");
		}
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Sankhya-EncerramentoOS/";
			String objName = "J-Loop_visitas_encerradas";

			si.runJob(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}

}
