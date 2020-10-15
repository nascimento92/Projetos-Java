package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

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
		start(arg0);		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		valida(arg0);
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) throws PersistenceException {
		
		DynamicVO newVO = (DynamicVO) arg0.getVo();
		BigDecimal contagem = newVO.asBigDecimal("CONTAGEM");
		BigDecimal saldoEsperado = newVO.asBigDecimal("SALDOESPERADO");
		
		if(contagem!=null) {
			newVO.setProperty("DIFERENCA", contagem.subtract(saldoEsperado));
		}
		
		BigDecimal diferenca = newVO.asBigDecimal("DIFERENCA");
		BigDecimal saldoAntes = newVO.asBigDecimal("SALDOANTERIOR");
		BigDecimal pedido = newVO.asBigDecimal("QTDPEDIDO");
		
		if(diferenca!=null) {
			newVO.setProperty("SALDOAPOS", diferenca.add(saldoAntes.add(pedido)));
		}
		
	}
	
	private void valida(PersistenceEvent arg0) throws PersistenceException {
		DynamicVO VO = (DynamicVO) arg0.getOldVO();
		String ajustado = VO.asString("AJUSTADO");
		
		if("S".equals(ajustado)) {
			throw new PersistenceException("<br/><br/><br/> <b>Não é possivel alterar uma tecla já ajustada!</b> <br/><br/><br/>");
		}
	}
}
