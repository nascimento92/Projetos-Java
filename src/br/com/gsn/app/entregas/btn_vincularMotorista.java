package br.com.gsn.app.entregas;

import java.math.BigDecimal;
import java.sql.Timestamp;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.modelcore.util.MGECoreParameter;

/**
 * 
 * @author fernando.silva
 * @version 1.3 
 * 20/10/2021 - Adicionado campos a ser inseridos na tabela (DTEXP, NOMEROTA);
 */

public class btn_vincularMotorista implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		String idMotorista = (String) arg0.getParam("ID");
		String veiculo = (String) arg0.getParam("VEICULO");
		Timestamp dtExp =  (Timestamp) arg0.getParam("DTEXP");
		String rota = (String) arg0.getParam("NOMEROTA");
		
		for(int i=0; i<linhas.length; i++) {
			registrarMotorista(linhas[i], new BigDecimal(idMotorista), new BigDecimal(veiculo), dtExp, rota);
		}
		
		if(linhas.length>0) {
			arg0.setMensagemRetorno("Motorista / Veículo vinculados!");
			chamaPentaho();
		}else {
			throw new Error("<br/><br/><b>Selecione uma ou mais Ordens de carga!</b><br/></b><br/>");
		}
		
	}
	
	private void registrarMotorista(Registro linhas, BigDecimal idMotorista, BigDecimal veiculo, Timestamp dtExp, String rota) {
		try {
			linhas.setCampo("AD_APPMOTO", idMotorista);
			linhas.setCampo("CODVEICULO", veiculo);
			linhas.setCampo("AD_DTEXP", dtExp);
			linhas.setCampo("AD_NOMEROTA", rota);
		} catch (Exception e) {
			throw new Error("ops "+ e.getCause());
		}
	}
	
	private void chamaPentaho() {

		try {

			String site = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/APPS/APP Entregas/Prod/Entregas/";
			String objName = "T-Cadastrar_entregas";

			si.runTrans(path, objName);

		} catch (Exception e) {
			e.getMessage();
		}
	}

}
