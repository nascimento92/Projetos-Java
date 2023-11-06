package br.com.gsn.contratos;

import java.math.BigDecimal;
import java.util.ArrayList;

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
		BigDecimal diaLeitura = VO.asBigDecimal("AD_DIALEITURA");
		String tipo = VO.asString("AD_TIPCONT");
		BigDecimal tipoNegociacao = VO.asBigDecimal("CODTIPVENDA");
		
		ArrayList<BigDecimal> tiposDeNegociacaoValidos = new ArrayList<BigDecimal>();
		tiposDeNegociacaoValidos.add(new BigDecimal(100));
		tiposDeNegociacaoValidos.add(new BigDecimal(1246));
		
		if("S".equals(diaFixo)) {
			
			if(diapag == null) {
				throw new Error("<br/><b>ATENÇÃO</b><br/><br/>Contrato de Dia Fixo, preencha o campo <b>Dia do pagamento</b><br/><br/>");
			}
			
			if(prazo==null) {
				throw new Error("<br/><b>ATENÇÃO</b><br/><br/>Contrato de Dia Fixo, preencha o campo <b>Prazo (Mês p/ dia Fixo)</b><br/><br/>0 = Mês corrente <br/> 1 = Mês atual +1 mês <br/> 2 = Mês atual +2 meses <br/> etc...");
			}
			
			if(!tiposDeNegociacaoValidos.contains(tipoNegociacao)) {
				throw new Error("<br/><b>ATENÇÃO</b><br/><br/> Tipo de negociação inválido, utilizar o 100 ou 1246");
			}

		}
		
		if(diaLeitura!=null) {
			if(diaLeitura.intValue()==0) {
				VO.setProperty("AD_DIALEITURA", new BigDecimal(1));
			}
			
			
			if (diaLeitura.intValue() > 31) {
				VO.setProperty("AD_DIALEITURA", new BigDecimal(31));
			}
			 
		}
		
		if("A".equals(tipo)) {
			VO.setProperty("CODNAT", new BigDecimal(11700));
		}
		
	}

}
