package br.com.flow.grancoffee.CancelamentoContrato;

import java.sql.ResultSet;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_cc_tarefaJava_VerificaQtdInsumos implements TarefaJava {

	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoTarefa arg0) {
		Object idflow = arg0.getIdInstanceProcesso();
		int cont = cont(idflow);
		
		if(cont>0) {
			arg0.setCampo("SYS_INSUMOS", "1");
		}else {
			arg0.setCampo("SYS_INSUMOS", "2");
		}
	}

	private int cont(Object idflow) {
		int qtd = 0;
		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT count(*) AS QTD FROM AD_PRODCANCELAMENTO WHERE IDINSTPRN=" + idflow);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int cont = contagem.getInt("QTD");
				
				if(cont>0) {
					qtd=cont;
				}

			}

		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_VerificaQtdInsumos] ## - Nao foi possivel verificar a quantidade!");
			e.getMessage();
			e.getCause();
		}
		
		return qtd;
	}
}
