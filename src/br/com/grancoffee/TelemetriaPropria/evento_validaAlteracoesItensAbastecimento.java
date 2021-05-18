package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class evento_validaAlteracoesItensAbastecimento implements EventoProgramavelJava{
	
	/**
	 * 08/10/20 15:41 esse objeto valida se uma tecla da tela Retornos Abastecimento que já foi ajustada está sendo alterada e tbm faz o calculo para o campo Saldo Após.
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
		
		if(contagem!=null) {
		//BigDecimal saldoEsperado = newVO.asBigDecimal("SALDOESPERADO");
		BigDecimal qtdretorno = newVO.asBigDecimal("QTDRETORNO");
		BigDecimal id = newVO.asBigDecimal("ID");
		BigDecimal diferenca = null;
		BigDecimal saldoAntes = newVO.asBigDecimal("SALDOANTERIOR");
		BigDecimal pedido = newVO.asBigDecimal("QTDPEDIDO");
		BigDecimal saldoEsperado = null;
		
		if(saldoAntes!=null && pedido!=null) {
			saldoEsperado = saldoAntes.add(pedido);
		}
		
		if (id != null) {
			if (validaSeHouveContagem(id)) { // quando há contagem
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
			} else { // quando não há contagem
				
				DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
				
				if (newVO.asBigDecimal("CONTAGEM") != oldVO.asBigDecimal("CONTAGEM")) {
					throw new Error("Esta visita não houve contagem, não alterar este campo!");
				}

				newVO.setProperty("SALDOAPOS", saldoEsperado.subtract(qtdretorno));
			}
		}
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
