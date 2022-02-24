package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;

import com.sankhya.util.BigDecimalUtil;
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

public class evento_valida_gc_instalacao implements EventoProgramavelJava{

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
		update(arg0);		
	}
	
	private void update(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		
		String abastecimento = VO.asString("ABASTECIMENTO");
		String oldAbastecimento = oldVO.asString("ABASTECIMENTO");
		
		String patrimonio = VO.asString("CODBEM");
		String valid = "";
		
		if(abastecimento!=oldAbastecimento) {
			
			if("S".equals(abastecimento)) {
				valid = "S";
			}else {
				valid = "N";
			}
			
			VO.setProperty("AD_NOPICK", valid);
			registraFila(patrimonio,valid);
		}
		
		
	}
	
	private void insert(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String abastecimento = VO.asString("ABASTECIMENTO");
		String patrimonio = VO.asString("CODBEM");
		String valid = "";
		
		if("S".equals(abastecimento)) {
			valid = "S";	
		}else {
			valid = "N";
		}
	
		VO.setProperty("AD_NOPICK", valid);
		registraFila(patrimonio,valid);
	}
	
	private void registraFila(String patrimonio, String nopick) {
	
		BigDecimal usu = BigDecimalUtil.getValueOrZero(((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_INTTP");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("TIPO", "P");
			VO.setProperty("IGNORESYNC", nopick);
			VO.setProperty("DTSOLICIT", TimeUtils.getNow());
			VO.setProperty("CODUSU", usu);
			VO.setProperty("CODBEM", patrimonio);
			
			dwfFacade.createEntity("AD_INTTP", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
