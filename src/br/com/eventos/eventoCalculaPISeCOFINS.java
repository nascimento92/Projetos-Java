package br.com.eventos;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoCalculaPISeCOFINS implements EventoProgramavelJava {

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
		// TODO Auto-generated method stub

	}

	public void getInfoNotas(PersistenceEvent arg0) throws Exception {

		//TABELA PRINCIPAL TGFDIN
		DynamicVO dinVO = (DynamicVO) arg0.getVo();
		
		BigDecimal nunota = (BigDecimal) dinVO.getProperty("NUNOTA");
		BigDecimal valor = (BigDecimal) dinVO.getProperty("VALOR");
		BigDecimal codimp = (BigDecimal) dinVO.getProperty("CODIMP");
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		
		// abrindo a consulta com a tabela TGFITE
		DynamicVO iteVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("ItemNota", nunota); // pega tudo da ite que tenha esse nunota

		String usoProd = (String) iteVO.getProperty("USOPROD"); // peguei o Uso do Produto
		BigDecimal qtdneg = (BigDecimal) iteVO.getProperty("QTDNEG");

		// abrindo a consulta com a tabela TGFCAB
		DynamicVO cabVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nunota);
		
		BigDecimal top = (BigDecimal) cabVO.getProperty("CODTIPOPER");
		
		// abrindo a consulta com a tabela TGFTOP
		DynamicVO topVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("TipoOperacao", top);
		
		String atualBem = (String) topVO.getProperty("ATUALBEM");
		
		if(valida(usoProd,atualBem)==true) {
			System.out.println("ok");
		}

	}
	
	public boolean valida(String usoProd, String atualBem) {
		boolean ret = false;
		
		if(usoProd=="I" && atualBem=="C") {
			ret = true;
		}
		
		return ret;
		
	}
	
	public int calculaPIS(BigDecimal valor, BigDecimal codimp, BigDecimal qtdneg) {
		if (codimp.intValue()==6) {
			int resultado = valor.intValue()/qtdneg.intValue();
			
			return resultado;
		}
		return 0;
	}

}
