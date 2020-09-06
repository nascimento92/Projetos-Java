package Travas;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;

public class validaInvativacaoContrato implements EventoProgramavelJava {

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
		
		start(arg0);
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		BigDecimal contrato = newVO.asBigDecimal("NUMCONTRATO");
		
		String newAtivo = newVO.asString("ATIVO");
		String oldAtivo = oldVO.asString("ATIVO");
		
		BigDecimal usuario = getUsuLogado();
		DynamicVO tsiusu = getTSIUSU(usuario);
		String ativaContrato = tsiusu.asString("AD_ATIVACONTRATO");
		String desativaContrato = tsiusu.asString("AD_DESATIVACONTRATO");
		
		if("S".equals(oldAtivo) && "N".equals(newAtivo)) {
			
			if("N".equals(desativaContrato) || desativaContrato==null) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O seu executante não possui autorização para Inativar um contrato!!</b></font>\n\n\n");
			}else {
				
				if(validaOsAbertas(contrato)) {
					throw new PersistenceException(
							"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
							"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Não é possivel inativar um contrato com OS pendentes!!</b></font>\n\n\n");
				}
				
				if(validaEnderecamentos(contrato)) {
					throw new PersistenceException(
							"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
							"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Não é possivel inativar um contrato com máquinas endereçadas!!</b></font>\n\n\n");
				}	
			}	
		}
		
		if("N".equals(oldAtivo) && "S".equals(newAtivo)) {
			if("N".equals(ativaContrato) || ativaContrato==null) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>O seu executante não possui autorização para ativar um contrato!!</b></font>\n\n\n");
			}
		}
		
		BigDecimal newParceiro = newVO.asBigDecimal("CODPARC");
		BigDecimal oldParceiro = oldVO.asBigDecimal("CODPARC");
		
		if(newParceiro!=oldParceiro) {
			if(validaOsAbertas(contrato, oldParceiro)) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
						"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>Não é possivel alterar o parceiro, existem OS em aberto!!</b></font>\n\n\n");
			}
		}
	}
	
	private boolean validaOsAbertas(BigDecimal contrato) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("OrdemServico");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND SITUACAO=?",new Object[] { contrato, "P" });
		
		if(VO!=null) {
			valida=true;
		}
		
		return valida;
	}
	
	private boolean validaOsAbertas(BigDecimal contrato, BigDecimal codparc) throws Exception {
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("OrdemServico");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND SITUACAO=? AND CODPARC=?",new Object[] { contrato, "P", codparc });
		
		if(VO!=null) {
			valida=true;
		}
		
		return valida;
	}
	
	private boolean validaEnderecamentos(BigDecimal contrato) throws Exception {
		boolean valida = false;

		JapeWrapper DAO = JapeFactory.dao("ENDERECAMENTO");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",	new Object[] { contrato});

		if (VO != null) {
			valida = true;
		}

		return valida;
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private DynamicVO getTSIUSU(BigDecimal usuario) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",	new Object[] { usuario});
		return VO;
	}

}
