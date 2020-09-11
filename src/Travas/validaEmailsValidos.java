package Travas;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class validaEmailsValidos implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// start(arg0);

	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// start(arg0);
	}

	private void start(PersistenceEvent arg0) throws PersistenceException {
		try {
			
			DynamicVO VO = (DynamicVO) arg0.getVo();
			String email = VO.asString("EMAIL");
			if (email != null) {
				if (email.equals("sem@grancoffee.com.br") || email.equals("reply@grancoffee.com.br")
						|| email.equals("toktake@toktake") || email.equals("sememail@grancoffee.com.br")
						|| email.equals("naopossui@naopossui.com") || email.equals("teste@teste.com")) {
					VO.setProperty("STATUS", "Sucesso: Enviada");
					VO.setProperty("TENTENVIO", new BigDecimal(3));
					VO.setProperty("TIPOENVIO", "S");
					
				}
			}
			
		} catch (Exception e) {
			System.out.println("## [Travas.validaEmailsValidos] - ERRO AO VALIDAR E-MAIL ##");
		}
		

	}

}
