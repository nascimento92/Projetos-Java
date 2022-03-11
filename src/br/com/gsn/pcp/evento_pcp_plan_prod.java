package br.com.gsn.pcp;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class evento_pcp_plan_prod implements EventoProgramavelJava{

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
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
		insert(arg0);	
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		insert(arg0);	
	}
	
	private void insert(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		
		try {

			String qtdimport = VO.asString("QTD2");
			//String p2 = qtdimport.replace("(?:[^\\d\\,])", "").replace(",", "");
			if(qtdimport!=null) {
				Double db = Double.valueOf(qtdimport);
				BigDecimal qtd = new BigDecimal(db);
				VO.setProperty("QTDNEG", qtd);
			}

		} catch (Exception e) {
			salvarException("[insert] Não foi possível alterar o valor !"+"\n"+e.getCause
					  ()+"\n"+e.getCause());
		}
	
		 
		
	}
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "evento_pcp_plan_prod");
			VO.setProperty("PACOTE", "br.com.gsn.pcp");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}


}
