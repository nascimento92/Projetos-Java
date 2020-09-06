package br.com.flow.RelatorioInstalacao;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class flowPreencheDadosEmail implements TarefaJava {
	
	private String emailSolicitante = new String();
	private String assunto = new String();
	private String dataEmail = new String();
	
	public void executar(ContextoTarefa contexto) throws Exception {
		start(contexto);
		
	}
	
	private void start(ContextoTarefa contexto) throws Exception {
		Object idEmail = contexto.getCampo("EMAIL");
		Object idInstanceProcesso = contexto.getIdInstanceProcesso();
		
		getCampos(idEmail);
		
		if(emailSolicitante==null) {
			emailSolicitante = "sistemas@grancoffee.com.br";
		}
		if(assunto==null) {
			assunto = "SEM ASSUNTO";
		}
		if(dataEmail==null) {
			dataEmail = new Timestamp(System.currentTimeMillis()).toString();
		}
		
		contexto.setCampo("EMAILSOLICITANTE", emailSolicitante);
		contexto.setCampo("ASSUNTO", assunto);
		contexto.setCampo("DTEMAIL", dataEmail);
		contexto.setCampo("IDPROCESSO", idInstanceProcesso.toString());
		 
	}
	
	private void getCampos(Object idEmail) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_EMAILFLOW");
		DynamicVO VO = DAO.findOne("ID=?",new Object[] { idEmail });
		
		assunto = VO.asString("ASSUNTO");
		
		String emailSolicitanteOriginal = VO.asString("REMETENTE");
		String aux = emailSolicitanteOriginal.substring(emailSolicitanteOriginal.indexOf("<")+1,emailSolicitanteOriginal.lastIndexOf(">"));
		emailSolicitante = aux;
		
		Timestamp data = null;
		data = VO.asTimestamp("DTEMAIL");
		
		if(data==null) {
			data=new Timestamp(System.currentTimeMillis());
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		String dataFormatada  = dateFormat.format(data);
		dataEmail = dataFormatada;
		
	}
	


}
