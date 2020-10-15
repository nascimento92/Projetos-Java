package br.com.eventos;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.comercial.ComercialUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class eventoMarcarPedidoComoNaoPendente implements EventoProgramavelJava {

	/**
	 * Objeto está funcionando atualmente na TGFCAB 20/08/2019 Ele serve para
	 * marcar os pedidos de compra como não pendentes após serem confirmados. (Solicitação do Adler Diretor)
	 * 
	 * 20/08/2019 17:08 - Foi observado que se o pedido for faturado parcial e a
	 * nota foi excluida os itens não voltam a ficar pendentes. AJUSTADO 21/08/2019 (método alteraPedidoDeletado)
	 * 
	 * 10/09/2019 11:38 - Foi necessário criar o campo AD_CORTEAUTOPEDIDOS na TGFTOP o objeto agora corta apenas
	 * as tops em que esse campo está marcado como SIM
	 * 
	 * 30/09/2019 13:52 - Foi necessário retirar das condições as empresas 70 e 74 (BEC)
	 */

	public void afterDelete(PersistenceEvent arg0) throws Exception {}

	public void afterInsert(PersistenceEvent arg0) throws Exception {}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {}

	public void beforeCommit(TransactionContext arg0) throws Exception {}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {

		DynamicVO tgfcabVO = (DynamicVO) arg0.getVo();
		String pendente = tgfcabVO.asString("PENDENTE");
		String tipMov = tgfcabVO.asString("TIPMOV");

		if (tipMov.equals(new String("C"))) {
			if (pendente.equals(new String("N"))) {
				
					
					int nuNota = tgfcabVO.asInt("NUNOTA");
					int nunotaPedido = getNunotaOrigem(nuNota);
					
					alterarPedidoDeletado(nunotaPedido);
				
			}
		}

	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		alterarPedido(arg0);

	}

	public void alterarPedido(PersistenceEvent arg0) throws Exception {

		DynamicVO tgfcabVO = (DynamicVO) arg0.getVo();
		String pendente = tgfcabVO.asString("PENDENTE");
		String tipMov = tgfcabVO.asString("TIPMOV");
		BigDecimal top = tgfcabVO.asBigDecimal("CODTIPOPER");
		BigDecimal empresa = tgfcabVO.asBigDecimal("CODEMP");

		if (tipMov.equals(new String("C"))) {
			if (pendente.equals(new String("N"))) {
				
				DynamicVO topRVO = ComercialUtils.getTipoOperacao(top);
				String cortaAutoPedidos = topRVO.asString("AD_CORTEAUTOPEDIDOS");
				
				if ("S".equals(cortaAutoPedidos)) {
					
					if(empresa.intValue()!= 70 && empresa.intValue()!=74){//ignora empresas da BEC
						
						int nuNota = tgfcabVO.asInt("NUNOTA");
						int nunotaPedido = getNunotaOrigem(nuNota);

						alterarPedido(nunotaPedido);
					}			
				}
			}
		}
		
		// pedido: 130115403
		// nota = 577814
		// chave = 35190863310411003038550010005778141875947902
		// CFOP = 1403 / 1102

	}

	public int getNunotaOrigem(int nuNota) throws Exception {

		int nunotaOrig = 0;

		JapeWrapper RecorrenciaDAO = JapeFactory.dao("CompraVendavariosPedido");
		DynamicVO nunotaORIG = RecorrenciaDAO.findOne("NUNOTA=?",
				new Object[] { nuNota });

		if (nunotaORIG != null) {
			nunotaOrig = nunotaORIG.asInt("NUNOTAORIG");
			return nunotaOrig;
		}

		return nunotaOrig;
	}

	public void alterarPedido(int nuNota) throws Exception {

		if (nuNota > 0) {

			EntityFacade dwf = EntityFacadeFactory.getDWFFacade();

			// alterando a TGFITE do pedido
			Collection<PersistentLocalEntity> iteEntities = dwf
					.findByDynamicFinder(new FinderWrapper("ItemNota",
							"this.NUNOTA = ? AND this.PENDENTE = 'S'",
							new Object[] { nuNota }));

			for (Iterator<?> iterator = iteEntities.iterator(); iterator
					.hasNext();) {

				PersistentLocalEntity iteEntity = (PersistentLocalEntity) iterator
						.next();
				DynamicVO iteVO = (DynamicVO) iteEntity.getValueObject();

				if ("S".equals(iteVO.asString("PENDENTE"))) {
					iteVO.setProperty("PENDENTE", "N");
					iteEntity.setValueObject((EntityVO) iteVO);

				}
			}
		}
	}

	public void alterarPedidoDeletado(int nuNota) throws Exception {
		
		if (nuNota > 0) {
				
			EntityFacade dwf = EntityFacadeFactory.getDWFFacade();

			// alterando a TGFITE do pedido
			Collection<PersistentLocalEntity> iteEntities = dwf.findByDynamicFinder(new FinderWrapper("ItemNota","this.NUNOTA = ?",	new Object[] { nuNota }));

			for (Iterator<?> iterator = iteEntities.iterator(); iterator.hasNext();) {

				PersistentLocalEntity iteEntity = (PersistentLocalEntity) iterator.next();
				DynamicVO iteVO = (DynamicVO) iteEntity.getValueObject();

				if ("N".equals(iteVO.asString("PENDENTE"))) {
					iteVO.setProperty("PENDENTE", "S");
					iteEntity.setValueObject((EntityVO) iteVO);

				}
			}
		}
	}

}

