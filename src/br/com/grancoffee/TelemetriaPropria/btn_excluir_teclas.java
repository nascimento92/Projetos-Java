package br.com.grancoffee.TelemetriaPropria;

import java.sql.ResultSet;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_excluir_teclas implements AcaoRotinaJava{
	/**
	 * Botão para excluir teclas da GC_PLANOGRAMA (apenas das máquinas que não possuem teclas no contrato).
	 * 
	 * 23/03/2023 - vs 1.0 - Gabriel Nascimento - Criação do objeto.
	 */
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		// TODO Auto-generated method stub
		Registro[] linhas = arg0.getLinhas();
		
		for(Registro r : linhas) {
			start(r, arg0);
		}
	}
	
	private void start(Registro linha, ContextoAcao arg0) throws Exception {
		String patrimonio = (String) linha.getCampo("CODBEM");
		if(patrimonio!=null) {
			if(verificaSeExistemTeclasNaAD_TECLAS(patrimonio)) {
				arg0.mostraErro("<b>OPS</b><br/>Existem teclas cadastradas no contrato, exclua as teclas antes!<br/>");
			}else {
				excluirTeclasDaGCPLANOGRAMA(patrimonio);
			}
		}
	}
	
	private void excluirTeclasDaGCPLANOGRAMA(String patrimonio) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			dwfFacade.removeByCriteria(new FinderWrapper("GCPlanograma", "this.CODBEM=?",new Object[] {patrimonio}));
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private Boolean verificaSeExistemTeclasNaAD_TECLAS(String patrimonio) {
		boolean valida = false;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM AD_TECLAS WHERE CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count >= 1) {
					valida = true;
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return valida;
	}

}
