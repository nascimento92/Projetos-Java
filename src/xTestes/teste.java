package xTestes;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.modelcore.util.MGECoreParameter;

public class teste implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Timestamp now = TimeUtils.getNow();
		Timestamp now2 = new Timestamp(System.currentTimeMillis());
		Instant now3 = Instant.now();
		ZonedDateTime now4 = ZonedDateTime.now();
		System.out.println(now);
		
		String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
		
		arg0.setMensagemRetorno("param: "+parameter);
		
	}
}
