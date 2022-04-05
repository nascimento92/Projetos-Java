package br.com.TGFPAR;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class  evento_cadastraContato implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal codparceiro = VO.asBigDecimal("CODPARC");
		String nome = VO.asString("NOMEPARC");
		String cpfcnpj = VO.asString("CGC_CPF");
		
		if(!verificaContatoUm(codparceiro)) {
			cadastraContato(codparceiro, nome, cpfcnpj);
		}
	}
	
	private boolean verificaContatoUm(BigDecimal codparceiro) throws Exception {
		boolean existe = false;
		JapeWrapper DAO = JapeFactory.dao("Contato");
		DynamicVO VO = DAO.findOne("CODPARC=? AND CODCONTATO=?",new Object[] { codparceiro, new BigDecimal(1) });
		
		if(VO!=null) {
			existe=true;
		}

		return existe;

	}
	
	private void cadastraContato(BigDecimal parceiro, String nome, String cpfcnpj) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("Contato");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODCONTATO", new BigDecimal(1));
			VO.setProperty("CODPARC", parceiro);
			VO.setProperty("NOMECONTATO", nome);
			VO.setProperty("CNPJ", cpfcnpj);
			
			dwfFacade.createEntity("Contato", (EntityVO) VO);
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
