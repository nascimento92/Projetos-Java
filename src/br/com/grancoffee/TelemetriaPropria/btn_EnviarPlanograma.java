package br.com.grancoffee.TelemetriaPropria;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;

public class btn_EnviarPlanograma implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(ContextoAcao arg0) {
		SELECT
		T.CODBEM,
		(SELECT COUNT(VALOR) FROM GC_TP_TROCADEGRADE_GERAL WHERE VALOR='NOVO' AND CODBEM=T.CODBEM) AS NOVO_PRODUTO,
		(SELECT COUNT(VALOR) FROM GC_TP_TROCADEGRADE_GERAL WHERE VALOR='AUMENTO' AND CODBEM=T.CODBEM) AS AUMENTO_VALOR,
		(SELECT COUNT(VALOR) FROM GC_TP_TROCADEGRADE_GERAL WHERE VALOR='REDUÇÃO' AND CODBEM=T.CODBEM) AS REDUCAO_VALOR,
		(SELECT COUNT(PAR) FROM GC_TP_TROCADEGRADE_GERAL WHERE PAR='AUMENTO' AND CODBEM=T.CODBEM) AS AUMENTO_PAR,
		(SELECT COUNT(PAR) FROM GC_TP_TROCADEGRADE_GERAL WHERE PAR='REDUÇÃO' AND CODBEM=T.CODBEM) AS REDUCAO_PAR
		FROM GC_INSTALACAO T
		WHERE T.CODBEM='025509'
	}

}
