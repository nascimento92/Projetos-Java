package br.com.ChamadosTI;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class validaChamadoDeSolicitacaoDesenvolvimento implements EventoProgramavelJava {

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
		start(arg0);
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public void start(PersistenceEvent arg0) throws PersistenceException {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal tipo = VO.asBigDecimal("TIPO");
		BigDecimal ganho = VO.asBigDecimal("SD_GANHO");
		String prioridade = VO.asString("SD_PRIORIDADE");
		
		if(tipo.intValue() >= 1006000 && tipo.intValue() < 1007000) {
			
			if(ganho==null) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Tipo do Chamado é uma solicitação de Desenvolvimento, sendo assim preencher na aba \"Solicitação Desenvolvimento\" o campo \"Ganho desenvolvimento (Horas) \" !</b></font>\n\n\n");
			}else if(prioridade==null) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Tipo do Chamado é uma solicitação de Desenvolvimento, sendo assim preencher na aba \"Solicitação Desenvolvimento\" o campo \"Prioridade para o setor\" !</b></font>\n\n\n");
			}
			
		}
	}
}
