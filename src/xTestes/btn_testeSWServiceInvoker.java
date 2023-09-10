package xTestes;

import org.json.JSONObject;

import com.google.gson.JsonObject;

import Helpers.SWServiceInvoker;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class btn_testeSWServiceInvoker implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		SWServiceInvoker sw = new SWServiceInvoker("http://sankhya.grancoffee.com.br:8180/", "GABRIEL", "gabriel123456");
		//String jsessionid = sw.doLogin();
		String body = "{nota:1}";
		JsonObject j = new JsonObject();
		JSONObject j2 = new JSONObject(body);
		
		arg0.setMensagemRetorno(j2.toString());
		
		
		//arg0.setMensagemRetorno(jsessionid);
		//sw.doLogout(jsessionid);
	}

}
