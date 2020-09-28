package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class flow_cc_tarefaJava_preencherEmailSolicitante implements TarefaJava {

	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(ContextoTarefa arg0) {
		Object idflow = arg0.getIdInstanceProcesso();
		
		BigDecimal usudono = getUsuarioInclusao(idflow);
		if(usudono==null) {
			usudono=new BigDecimal(0);
		}
		
		String email = getEmail(usudono);
		
		arg0.setCampo("SYS_EMAIL_RESP", email);
		arg0.setCampo("SYS_IDFLOW", idflow.toString());
		
		System.out.println("## [flow_cc_tarefaJava_preencherEmailSolicitante] ## - E-mail enviado para: "+email+" idflow: "+idflow);
		
	}
	
	private BigDecimal getUsuarioInclusao(Object idflow) {
		BigDecimal codusudono=null;
		try {
			JapeWrapper DAO = JapeFactory.dao("InstanciaTarefa");
			DynamicVO VO = DAO.findOne("IDINSTPRN=? and IDELEMENTO=?",new Object[] { idflow,"UserTask_1rgod34" });
			codusudono = VO.asBigDecimal("CODUSUDONO");
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_preencherEmailSolicitante] ## - Não foi possivel obter o Usuario de Inclusao.");
			e.getMessage();
			e.printStackTrace();
		}
		return codusudono;
	}
	
	private String getEmail(BigDecimal codusu) {
		String email=null;
		try {
			JapeWrapper DAO = JapeFactory.dao("Usuario");
			DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { codusu });
			email = VO.asString("EMAIL");
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_preencherEmailSolicitante] ## - Não foi possivel obter o Email.");
			e.getMessage();
			e.printStackTrace();
		}
		return email;
	}
}
