package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;

public class btn_alterarVisita implements AcaoRotinaJava{
	
	int x = 0;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		
		String motivo = (String) arg0.getParam("MOTIVO");
		String substituto = (String) arg0.getParam("SUB");
		Timestamp data = (Timestamp) arg0.getParam("DATA");
		Timestamp dataAtendimento = (Timestamp) arg0.getParam("DTVISIT");
		
		Registro[] linhas = arg0.getLinhas();
		
		for(Registro r : linhas) {
			start(r,motivo, new BigDecimal(substituto), data, arg0, dataAtendimento);
		}
		
		if(x>0) {
			arg0.setMensagemRetorno("<br/>Foram alteradas <b>"+x+"</b> visitas!");
		}
	}
	
	private void start(Registro linha,String motivo,BigDecimal substituto, Timestamp data, ContextoAcao arg0, Timestamp dataAtendimento) throws Exception {
		
		validacoes(linha,arg0,data);
		
		linha.setCampo("AD_MOTALT", motivo);
		linha.setCampo("AD_CODUSUALT", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
		linha.setCampo("AD_DTALTAGEND", TimeUtils.getNow());
		linha.setCampo("DTAGENDAMENTO", data);
		linha.setCampo("AD_DTATENDIMENTO", dataAtendimento);
		
		if(substituto!=null) {
			linha.setCampo("AD_USUSUB", substituto);
		}
		
		x++;
	}
	
	private void validacoes(Registro linha, ContextoAcao arg0, Timestamp data) throws Exception {
		String status = (String) linha.getCampo("STATUS");
		BigDecimal os = (BigDecimal) linha.getCampo("NUMOS");
		
		if("3".equals(status)) {
			arg0.mostraErro("<br/><b>ATENÇÃO</b><br/><br/> Visita Concluída ! não pode ser alterada !<br/><br/>");
		}
		
		if("4".equals(status)) {
			arg0.mostraErro("<br/><b>ATENÇÃO</b><br/><br/> Visita Cancelada ! não pode ser alterada !<br/><br/>");
		}
		
		if(data.before(TimeUtils.getNow())) {
			arg0.mostraErro("<br/><b>ATENÇÃO</b><br/><br/> A data não pode ser inferior que a data atual !<br/><br/>");
		}
		
		if(os != null) {
			arg0.mostraErro("<br/><b>ATENÇÃO</b><br/><br/> Só é possível alterar uma visita agendada ! a visita solicitada já gerou a OS: <b>"+os+"</b> não poderá ser alterada !<br/><br/>");
		}
	}

}
