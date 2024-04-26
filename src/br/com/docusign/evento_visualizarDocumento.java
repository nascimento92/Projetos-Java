package br.com.docusign;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class evento_visualizarDocumento implements EventoProgramavelJava{
	
	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {

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
		try {
			DynamicVO VO = (DynamicVO) arg0.getVo();
			String idEnvelope = VO.asString("ENVELOPEID");
			String url = "https://apps-d.docusign.com/api/send/api/accounts/"+getAccountId()+"/envelopes/"+idEnvelope+"/documents/1/preview/CONTRATO_-_DOCUSIGN.XLS.pdf?#page=1";
			VO.setProperty("AD_URL", url);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private String getAccountId() throws Exception {
		String accountId = "";
		JapeWrapper dao = JapeFactory.dao("DocuSignConfiguracao");
		DynamicVO configVO = dao.findOne("this.CODDOCUSIGN=?", new Object[] { new BigDecimal(1)});
		if(configVO!=null) {
			accountId = configVO.asString("ACCOUNTID");
		}
		return accountId;
	}
}
