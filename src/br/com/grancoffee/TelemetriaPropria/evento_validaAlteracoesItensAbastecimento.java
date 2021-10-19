package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class evento_validaAlteracoesItensAbastecimento implements EventoProgramavelJava{
	
	/**
	 * 08/10/20 15:41 esse objeto valida se uma tecla da tela Retornos Abastecimento que já foi ajustada está sendo alterada e tbm faz o calculo para o campo Saldo Após.
	 * 17/10/21 vs 1.5 Alterada a validação para considerar apenas alterações de teclas onde a contagem foi > 0
	 * 18/10/21 vs 1.6 Alteração no objeto para, se não era uma visita com contagem, porém algué insere informações de contagem, ele faz os calculos e começa a adotar aquela visita como uma de contagem.
	 */
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
		DynamicVO VO = (DynamicVO) arg0.getVo();
		VO.setProperty("QTDRETORNO", new BigDecimal(0));
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {	
		valida(arg0);
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) throws Exception {
		
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		BigDecimal contagem = newVO.asBigDecimal("CONTAGEM");
		
		//BigDecimal saldoEsperado = newVO.asBigDecimal("SALDOESPERADO");
		BigDecimal qtdretorno = newVO.asBigDecimal("QTDRETORNO");
		BigDecimal id = newVO.asBigDecimal("ID");
		BigDecimal diferenca = null;
		BigDecimal saldoAntes = newVO.asBigDecimal("SALDOANTERIOR");
		BigDecimal pedido = newVO.asBigDecimal("QTDPEDIDO");
		BigDecimal saldoEsperado = null;

		if (saldoAntes != null && pedido != null) {
			saldoEsperado = saldoAntes.add(pedido);
		}

		if (id != null) {
			if (validaSeHouveContagem(id)) { // quando há contagem
				
				alteraDadosContagem(qtdretorno,contagem,newVO,saldoEsperado,diferenca);
				
			} else { // quando não há contagem

				DynamicVO oldVO = (DynamicVO) arg0.getOldVO();

				if (newVO.asBigDecimal("CONTAGEM") != oldVO.asBigDecimal("CONTAGEM")) {
					if(newVO.asBigDecimal("CONTAGEM").intValue()>0) {
						
						alteraDadosContagem(qtdretorno,contagem,newVO,saldoEsperado,diferenca);
						marcaVisitaComoTendoContagem(id);
						
					}
				}

				newVO.setProperty("SALDOAPOS", saldoEsperado.subtract(qtdretorno));
			}
		}
		
	}
	
	private void alteraDadosContagem(BigDecimal qtdretorno, BigDecimal contagem, DynamicVO newVO, BigDecimal saldoEsperado, BigDecimal diferenca) {
		if (qtdretorno == null) {
			qtdretorno = new BigDecimal(0);
		}

		if (contagem != null) {
			BigDecimal diferencaFinal = contagem.subtract(saldoEsperado);
			newVO.setProperty("DIFERENCA", diferencaFinal.add(qtdretorno));
		}

		diferenca = newVO.asBigDecimal("DIFERENCA");

		if (diferenca != null) {
			BigDecimal saldoFinal = diferenca.add(saldoEsperado);
			newVO.setProperty("SALDOAPOS", saldoFinal.subtract(qtdretorno));
		}
	}
	
	private void marcaVisitaComoTendoContagem(BigDecimal id){
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_RETABAST",
					"this.ID=?", new Object[] { id }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CONTAGEM", "S");

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private void valida(PersistenceEvent arg0) throws PersistenceException {
		DynamicVO VO = (DynamicVO) arg0.getOldVO();
		if(VO!=null) {
			String ajustado = VO.asString("AJUSTADO");
			
			if("S".equals(ajustado)) {
				throw new PersistenceException("<br/><br/><br/> <b>Não é possivel alterar uma tecla já ajustada!</b> <br/><br/><br/>");
			}
		}	
	}
	
	private boolean validaSeHouveContagem(BigDecimal id) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("AD_RETABAST");
		DynamicVO VO = DAO.findOne("ID=?",new Object[] { id });
		if(VO!=null) {
			String contagem = VO.asString("CONTAGEM");
			if("S".equals(contagem)) {
				valida=true;
			}
		}
		
		return valida;
	}
}
