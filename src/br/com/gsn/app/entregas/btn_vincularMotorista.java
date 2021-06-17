package br.com.gsn.app.entregas;

import java.math.BigDecimal;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class btn_vincularMotorista implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String idMotorista = (String) arg0.getParam("ID");
		String veiculo = (String) arg0.getParam("VEICULO");
		
		for(int i=0; i<linhas.length; i++) {
			registrarMotorista(linhas[i], new BigDecimal(idMotorista), new BigDecimal(veiculo));
		}
		
		arg0.setMensagemRetorno("Motorista / Veículo vinculados!");
	}
	
	private void registrarMotorista(Registro linhas, BigDecimal idMotorista, BigDecimal veiculo) {
		try {
			linhas.setCampo("AD_APPMOTO", idMotorista);
			linhas.setCampo("CODVEICULO", veiculo);
		} catch (Exception e) {
			throw new Error("ops "+ e.getCause());
		}
	}

}
