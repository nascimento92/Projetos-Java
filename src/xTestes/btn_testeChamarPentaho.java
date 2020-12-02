package xTestes;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class btn_testeChamarPentaho implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		chamaPentaho();
	}

	private void chamaPentaho() {

		try {

			String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC/Projetos/GCW/Jobs/";
			String objName = "JOB - GSN008 - Verificar Visitas";

			si.runJob(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}
}
