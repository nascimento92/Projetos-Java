package br.com.eventos;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoAlterarLocalDestino implements EventoProgramavelJava {

	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		getDados(arg0);	
		
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		

	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		getDados(arg0);

	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		getDados(arg0);

	}
	/**
	 * Variavel que pega os valores da instancia em que o objeto foi inserido
	 * @param arg0
	 * @throws Exception
	 * @author gabriel.nascimento
	 */
	
	public void getDados(PersistenceEvent arg0) throws Exception {

		/**
		 * Adicionado todo o conteudo da instancia na variavél.
		 * 
		 * Variavel nunota recebe o número único (NUNOTA)
		 * Variavel sequencia recebe a sequencia do produto (SEQUENCIA)
		 * 
		 */
		DynamicVO iteDO = (DynamicVO) arg0.getVo();

		BigDecimal nunota = iteDO.asBigDecimal("NUNOTA");
		BigDecimal sequencia = iteDO.asBigDecimal("SEQUENCIA");

		/**
		 * Criar a variavel para verificar os dados em outra tabela neste caso TGFCAB
		 * 
		 * variavel top recebe o tipo de operação da CAB (CODTIPOPER);
		 *
		 */

		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO cabVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nunota);
		
		BigDecimal top = cabVO.asBigDecimal("CODTIPOPER");
		BigDecimal localDestino = cabVO.asBigDecimal("AD_CODLOCAL");
		
		
		if(top.intValue()==1854) {
			
			if(sequencia.intValue()<0) {
				iteDO.setProperty("CODLOCALORIG", localDestino);
			}
		}
		

	}
	

}
