package br.com.NFCE;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;



public class eventoProdutosNFCE implements EventoProgramavelJava{

	/**
	 * Antiga trigger TRG_UPD_TGFITE_VLRUNIT_GRAN atualiza o preço do produto e total das notas NFCE
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
		start(arg0);
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
	}
	
	public void start(PersistenceEvent arg0) throws Exception{
		
		try {
			
			DynamicVO iteVO = (DynamicVO) arg0.getVo();
			
			BigDecimal vlrAdicional = iteVO.asBigDecimal("AD_VLRUN");
			BigDecimal qtdneg = iteVO.asBigDecimal("QTDNEG");
			BigDecimal nunota = iteVO.asBigDecimal("NUNOTA");
			
			DynamicVO tgfcab = TGFCAB(nunota);
			BigDecimal top = tgfcab.asBigDecimal("CODTIPOPER");
			BigDecimal codvend = tgfcab.asBigDecimal("CODVEND");
			
			if(top.equals(new BigDecimal(1108)) && codvend.equals(new BigDecimal(280))){
				
				if(vlrAdicional!=null){
					BigDecimal total = vlrAdicional.multiply(qtdneg);
					
					iteVO.setProperty("VLRUNIT", vlrAdicional);
					iteVO.setProperty("VLRTOT", total);
				}		
			}
			
			
		} catch (Exception e) {
			System.out.println("***ERRO AO ALTERAR O VALOR NFCe****"+ e.getMessage());
		}
		
		
		/*if(top.equals(new BigDecimal(1108)) && codvend!=new BigDecimal(280)){
			alteraPreco(nunota);
		}*/
		
	}
	
	public DynamicVO TGFCAB(BigDecimal nunota) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",	new Object[] { nunota });
		return VO;

	}
	
	
	
	
	
	
	
	
	//Métodos Inutilizados
	/*private void alteraPreco(BigDecimal nunota) throws Exception{
		
		BigDecimal nunotaOrig = descobreNotaOrig(nunota);
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemNota","this.NUNOTA = ? ", new Object[] { nunota }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

		BigDecimal produto = DynamicVO.asBigDecimal("CODPROD");
		BigDecimal sequencia = DynamicVO.asBigDecimal("SEQUENCIA");
		BigDecimal vlrunit = DynamicVO.asBigDecimal("VLRUNIT");
		
		BigDecimal valorunit = pegaVlrUnitNotaOrigem(nunotaOrig, produto, sequencia);
		
		if(vlrunit!=valorunit){
			DynamicVO.setProperty("VLRUNIT", valorunit);
		}
	
		itemEntity.setValueObject((EntityVO) DynamicVO);
		}
		
	}*/
	
	/*private BigDecimal descobreNotaOrig(BigDecimal nunota) throws Exception{
		JapeWrapper RecorrenciaDAO = JapeFactory.dao("CompraVendavariosPedido");
		DynamicVO VO = RecorrenciaDAO.findOne("NUNOTA=?",
				new Object[] { nunota });

		 BigDecimal nunotaOrig = VO.asBigDecimal("NUNOTAORIG");

		return nunotaOrig;
	}*/
	
	/*private BigDecimal pegaVlrUnitNotaOrigem(BigDecimal nunota, BigDecimal codprod, BigDecimal sequencia) throws Exception{
				
		BigDecimal retorno = new BigDecimal(0);
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemNota","this.NUNOTA = ? AND this.CODPROD=? AND this.SEQUENCIA=? ", new Object[] { nunota,codprod,sequencia }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

		BigDecimal vlrUnit = DynamicVO.asBigDecimal("VLRUNIT");
		
		retorno = vlrUnit;
		}
		
		return retorno;	

	}*/

}

