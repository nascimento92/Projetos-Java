package br.com.gsn.contratos;

import java.math.BigDecimal;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;

public class evento_validacoes_tcscon implements EventoProgramavelJava{

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
		start(arg0);		
	}
	
	private void start(PersistenceEvent arg0) {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		String diaFixo = VO.asString("AD_DIAFIXO");
		BigDecimal diapag = VO.asBigDecimal("DIAPAG");
		BigDecimal prazo = VO.asBigDecimal("AD_PRAZOMES");
		
		if("S".equals(diaFixo)) {
			
			if(diapag == null) {
				throw new Error("<br/><b>ATENÇÃO</b><br/><br/>Contrato de Dia Fixo, preencha o campo <b>Dia do pagamento</b><br/><br/>");
			}
			
			if(prazo==null) {
				throw new Error("<br/><b>ATENÇÃO</b><br/><br/>Contrato de Dia Fixo, preencha o campo <b>Prazo (Mês p/ dia Fixo)</b><br/><br/>0 = Mês corrente <br/> 1 = Mês atual +1 mês <br/> 2 = Mês atual +2 meses <br/> etc...");
			}
			
			VO.setProperty("CODTIPVENDA", new BigDecimal(100));
			
		}
	}

}
