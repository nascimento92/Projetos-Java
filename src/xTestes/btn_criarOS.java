package xTestes;

import com.google.gson.JsonObject;

import Helpers.SWServiceInvoker;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class btn_criarOS implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		SWServiceInvoker si = new SWServiceInvoker("http://sankhya.grancoffee.com.br:8180", "GABRIEL", "gabriel123456");
		String service = "SacSP.newOS";
		String modulo = "grancoffee-labsx";
		String body = "{\"config\":{\"CODPARC\":\"2\",\"NUMCONTRATO\":\"21814\",\"CODSERV\":\"200000\",\"EXECUTANTE\":\"3152\",\"PARCEIRONESPRESSO\":\"\",\"CODBEM\":\"019109\",\"PROBLEMA\":\"ABASTECER BEM: 019109\","+
		"\"MOTIVO\":\"97\",\"EMAIL\":\"\",\"BENSVINCULADOS\":[{\"TAREFA\":\"\",\"CODBEM\":\"019109\",\"CODPROD\":\"2676\"}]}}}";
		
		JsonObject callAsJson = si.callAsJson(service, modulo, body);
		
		//teste
	}

}
