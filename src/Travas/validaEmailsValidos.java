package Travas;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class validaEmailsValidos implements EventoProgramavelJava {
	
	/**
	 * E-mails validados da tela AD_EMAILBLOQUEADO (E-mails bloqueados)
	 * @author gabriel.nascimento
	 */
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
			String tipoEnvio = VO.asString("TIPOENVIO");
			
			if("E".equals(tipoEnvio)) {
				if (email != null) {
					if (validaEmail(email)) {
						VO.setProperty("STATUS", "Erro: E-mail Invalido");
						VO.setProperty("TENTENVIO", new BigDecimal(3));
						VO.setProperty("TIPOENVIO", "S");
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("## [Travas.validaEmailsValidos] - ERRO AO VALIDAR E-MAIL ##");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private boolean validaEmail(String email) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("AD_EMAILBLOQUEADO");
		DynamicVO VO = DAO.findOne("EMAIL=?",new Object[] { email });
		if(VO!=null) {
			valida = true;
		}
		
		return valida;
	}

}
