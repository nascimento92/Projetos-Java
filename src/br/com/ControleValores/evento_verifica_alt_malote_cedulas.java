package br.com.ControleValores;

import java.math.BigDecimal;
import java.sql.Timestamp;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;

public class evento_verifica_alt_malote_cedulas implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
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
		// TODO Auto-generated method stub
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		verifaUsuario(arg0);
		
	}
	
	private void verifaUsuario(PersistenceEvent arg0) throws PersistenceException {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		String newMalote = newVO.asString("MALOTECEDR");
		String oldMalote = oldVO.asString("MALOTECEDR");
		
		if("N".equals(oldMalote)||oldMalote==null) {
			if("S".equals(newMalote)) {
				newVO.setProperty("USURESPALT", getUsuLogado());
				newVO.setProperty("DTALTMALOTECEDULAS", new Timestamp(System.currentTimeMillis()));
			}
		}else if ("S".equals(oldMalote) && "N".equals(newMalote)) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
					"\n\n\n\n<font size=\"15\" color=\"#008B45\">O campo <b>Malote Recebido (Cedulas)</b> não pode ser desmarcado!!</font>\n\n\n");
		
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}

}
