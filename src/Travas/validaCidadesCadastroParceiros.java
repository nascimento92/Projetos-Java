package Travas;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class validaCidadesCadastroParceiros implements EventoProgramavelJava {
	
	/**
	 * Objeto que verifica se a cidade do parceiro possui Mun. domicílio fiscal
	 * 
	 * 21/10/2019 09:16 inserida na tgfpar
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
		// TODO Auto-generated method stub
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}
	
	private void start (PersistenceEvent arg0) throws Exception{
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal cidade = VO.asBigDecimal("CODCID");
		
		if(!validaSeAhCidadePossuiCodigoMunDomicilioFiscal(cidade)){
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/><br/><br/><br/><br/><br/>"+
					"\n\n\n\n<font size=\"15\" color=\"#008B45\"><b>A cidade <u>"+cidade+"</u>\n\nNão possui Mun. domicílio fiscal ou o código do domicílio fiscal está zerado, não poderá ser utilizada!!</b></font>\n\n\n");
		}
	}
	
	private boolean validaSeAhCidadePossuiCodigoMunDomicilioFiscal(BigDecimal cidade) throws Exception{
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("Cidade");
		DynamicVO VO = DAO.findOne("CODCID=?",new Object[] { cidade });
		
		BigDecimal munDomicioFiscal = VO.asBigDecimal("CODMUNFIS");
		
		if(munDomicioFiscal!=null){
			
			if(munDomicioFiscal.equals(new BigDecimal(0))){
				valida = false;
			}else{
				valida = true;
			}
			
		}
		
		return valida;
		
	}

}
