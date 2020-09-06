package br.com.ChamadosTI;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class validaChamadoDeSolicitacaoComputadores implements EventoProgramavelJava {
	
	private String nometab = "AD_CHAMADOSTI";
	
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
		
	}
	
	
	
	public void start(PersistenceEvent arg0) throws Exception {
		
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		BigDecimal tipo = VO.asBigDecimal("TIPO");
		String tipoSolicit = VO.asString("TIPSOLICIT");
		String patAtual = VO.asString("PATEQUIPATUAL");
		String cpfColaborador = VO.asString("SC_CPF");
		String perfilTrabalho = VO.asString("SC_PERFIL");
		
		if(tipo.intValue() >= 11002000 && tipo.intValue() < 11003000) {
			if(tipoSolicit==null) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Tipo do Chamado é uma solicitação de computadores, sendo assim preencher na aba \"Solicitação Computador\" o campo tipo de solicitação!</b></font>\n\n\n");
		
			}else if("3".equals(tipoSolicit) || "4".equals(tipoSolicit) || "5".equals(tipoSolicit) || "6".equals(tipoSolicit) || "7".equals(tipoSolicit) || "8".equals(tipoSolicit)) {
				
				if(patAtual==null) {
					throw new PersistenceException(
							"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
							"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O tipo selecionado foi uma Substituição, por gentileza preencher o campo \"Patrimônio do Equipamento Atual\" !</b></font>\n\n\n");
			
				}
			}
			
			validaCampo(cpfColaborador, new String("SC_CPF"));
			validaCampo(perfilTrabalho, new String("SC_PERFIL"));
		}
	}
	
	private void validaCampo(Object campo, String nomeCampo) throws Exception {
		if(campo==null) {
			
			JapeWrapper DAO = JapeFactory.dao("Campo");
			DynamicVO VO = DAO.findOne("NOMECAMPO=? AND NOMETAB=?",new Object[] { nomeCampo,nometab });	
			String descricaoCampo = VO.asString("DESCRCAMPO");

			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
					"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Tipo do Chamado é uma solicitação de computadores, sendo assim preencher na aba \"Solicitação Computador\" o campo "+descricaoCampo+"!</b></font>\n\n\n");
	
		}
		
	}

}
