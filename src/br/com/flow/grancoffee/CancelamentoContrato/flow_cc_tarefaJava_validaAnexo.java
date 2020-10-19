package br.com.flow.grancoffee.CancelamentoContrato;

import java.sql.ResultSet;

import br.com.sankhya.extensions.flow.ContextoEvento;
import br.com.sankhya.extensions.flow.EventoProcessoJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_cc_tarefaJava_validaAnexo implements EventoProcessoJava {

	@Override
	public void executar(ContextoEvento arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoEvento arg0) throws Exception {
		Object idInstanceTarefa = arg0.getIdInstanceProcesso();

		if (!validaSeFoiAnexado(idInstanceTarefa)) {
			throw new PersistenceException(
					"<br/><br/><br/><b>Anexar pelo menos um documento!</b><br/><br/><br/>");
		}
	}

	private boolean validaSeFoiAnexado(Object idInstanceTarefa) {
		boolean valida = false;

		try {

			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql(
					"SELECT count(*) AS QTD FROM TSIANX WHERE NOMEINSTANCIA ='InstanciaProcesso' AND PKREGISTRO LIKE '%"+idInstanceTarefa+"%'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("QTD");
				if (count > 0) {
					valida = true;
				}
			}

		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_validaAnexo] ## - Nao foi possivel verificar os anexos");
		}

		return valida;
	}
}
