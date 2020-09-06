package br.com.flow.RelatorioInstalacao;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_5_AtualizaSerie implements TarefaJava {

	public void executar(ContextoTarefa arg0) throws Exception {
		try {
			start(arg0);
		} catch (Exception e) {
			System.out.println("Nao foi possivel atualizar a serie! "+e.getMessage());
		}
		
	}
	
	private void start(ContextoTarefa arg0) throws Exception {
		atualizaSeries();
	}
	
	private void atualizaSeries() throws Exception {
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CODBEM,NOVASERIE FROM AD_MAQUINASFLOW WHERE NOVASERIE IS NOT NULL");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			String patrimonio = contagem.getString("CODBEM");
			String novaSerie = contagem.getString("NOVASERIE");
			
			atualizaPatrimonio(patrimonio, novaSerie);
		}
	}
	
	private void atualizaPatrimonio(String patrimonio, String serie) throws Exception {
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("Imobilizado","this.CODBEM=?", new Object[] { patrimonio}));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		DynamicVO VO = (DynamicVO) NVO;
		
		String atualSerie = VO.asString("DESCRBEM");
		
		if(serie!=atualSerie) {
			VO.setProperty("DESCRBEM", serie);
		}
		
		itemEntity.setValueObject(NVO);
		}
	}
}
