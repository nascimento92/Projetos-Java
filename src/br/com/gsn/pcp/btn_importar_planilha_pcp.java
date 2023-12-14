package br.com.gsn.pcp;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class btn_importar_planilha_pcp implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		
//		Timer timer = new Timer(1000, new ActionListener() {	
//			@Override
//			public void actionPerformed(ActionEvent e) {
//								
//			}
//		});
//		timer.setRepeats(false);
//		timer.start();
		
		try {
			chamaPentaho();
		} catch (Exception e) {
			throw new Error("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
		
		arg0.setMensagemRetorno("<br/><br/> <b>Atenção !</b> <br/><br/> A rotina demora alguns segundos para realizar a importação ! <br/> Atualize a página para visualizar os dados ! <br/><br/><br/>");

	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic Z2FicmllbC5uYXNjaW1lbnRvOkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Google/PCP-MRP/";
			String objName = "J-Planilha_pcp";

			si.runJob(path, objName);

		} catch (Exception e) {
			throw new Error("[chamaPentaho] nao foi possivel chamar o pentaho! "+e.getMessage()+"\n"+e.getCause());
		}
	}

}
