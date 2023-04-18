package br.com.ManutencaoPreventiva;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;

public class validaUsuarioQueAlteraPrazo implements EventoProgramavelJava {
	
	/**
	 * 14/04/2023 - vs 1.1 - Gabriel Nascimento - Inserida função para calcular o prazo das máquinas recem inseridas.
	 */
	
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
		calculaPrazoInicial(arg0);		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}
	
	private void calculaPrazoInicial(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal prazo = VO.asBigDecimal("PRAZO");
		if(prazo!=null) {
			Timestamp dataHoje = TimeUtils.getNow();
			VO.setProperty("DTPROXMANUTENCAO", TimeUtils.dataAddDay(dataHoje, prazo.intValue()));
		}
	}
	
	private void start(PersistenceEvent arg0) throws Exception{
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO OLDVO = (DynamicVO) arg0.getOldVO();
		
		BigDecimal newPrazo = VO.asBigDecimal("PRAZO");
		BigDecimal oldPrazo = OLDVO.asBigDecimal("PRAZO");
		
		if(newPrazo!=oldPrazo){
			BigDecimal usuario = getUsuLogado();
			
			if(!verificaUsuario(usuario)){
				throw new PersistenceException("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"<font size=\"15\">\n<b>O seu usuário não possui autorização para alterar o Prazo das OS!</b></font>");
			}
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;
	    	
	}
	
	private boolean verificaUsuario(BigDecimal usuario) throws Exception{
		
		boolean valida = false;
		
		JapeWrapper RecorrenciaDAO = JapeFactory.dao("Usuario");
		DynamicVO TGFCPLVO = RecorrenciaDAO.findOne("CODUSU=?", new Object[] { usuario });

		String altera = TGFCPLVO.asString("AD_ALTPRAZOMANPREV");
		
		if("S".equals(altera)){
			valida = true;
		}

		return valida;

	}

}

