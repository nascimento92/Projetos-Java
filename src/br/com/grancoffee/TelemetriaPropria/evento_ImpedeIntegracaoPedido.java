package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class evento_ImpedeIntegracaoPedido implements EventoProgramavelJava {

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
		start(arg0);	
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
				
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal top = VO.asBigDecimal("CODTIPOPER");
		BigDecimal vendedor = VO.asBigDecimal("CODVEND");
		String patrimonio = VO.asString("AD_CODBEM");
		
		boolean validaPatrimonio = validaPatrimonio(patrimonio);
		
		if(top.intValue()==1018 && vendedor.intValue()==743 && validaPatrimonio==true) {
			throw new Error("ERRO X0014578XBE");
		}
	}
	
	private boolean validaPatrimonio(String patrimonio) {
		boolean valida = false;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("GCInstalacao");
			DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
			if(VO!=null) {
				String micromarketing = VO.asString("TOTEM");
				String acompanhaAbastecimento = VO.asString("ABASTECIMENTO");
				
				if("S".equals(micromarketing) && "S".equals(acompanhaAbastecimento)) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valida;
	}

}
