package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class btn_forcarAjuste implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(ContextoAcao arg0) {
		arg0.confirmar("Atenção", "A rotina demora alguns minutos para ser executada.", 0);
		
		Timer timer = new Timer(5000, new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				chamaPentaho();				
			}
		});
		timer.setRepeats(false);
		timer.start();
		
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Mid-Ajuste_Estoque/";
			String objName = "J-Ajuste_de_estoque";

			si.runJob(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}

}
